package net.iakovlev.timeshape;

import net.iakovlev.timeshape.proto.Geojson;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * A polygon stored in a layout that makes point-in-polygon queries cheap.
 * <p>
 * All rings of the polygon are concatenated into two flat coordinate arrays, so a query walks
 * contiguous memory instead of chasing objects. Coordinates are kept as {@code float}, which is
 * lossless: the source data is float precision to begin with. Arithmetic is done in {@code double},
 * so widening the inputs introduces no rounding of its own.
 * <p>
 * Optionally the polygon carries a latitude index: edges are bucketed by the latitude band they
 * span, so a query only visits the edges that can possibly cross the query latitude instead of all
 * of them. For the larger time zones this turns an O(vertices) scan into a handful of edge tests.
 */
final class PreparedPolygon implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Points closer than this to the outline count as being inside the polygon. Time zones share
     * their borders, so a coordinate that sits exactly on one belongs to both of its neighbours.
     * The value matches the tolerance of the WGS84 spatial reference, which is what the library
     * used to inherit from the geometry engine it was built on.
     */
    private static final double TOLERANCE = 1.0E-8;
    private static final double TOLERANCE_SQUARED = TOLERANCE * TOLERANCE;

    /** Longitudes of every ring, concatenated. */
    private final float[] xs;
    /** Latitudes of every ring, concatenated. */
    private final float[] ys;
    /** Ring {@code r} occupies vertices {@code [ringStart[r], ringStart[r + 1])}, first == last. */
    private final int[] ringStart;

    final float minX;
    final float minY;
    final float maxX;
    final float maxY;

    /** Start offset of each latitude bucket into {@link #bucketEdges}, or null if not indexed. */
    private final int[] bucketStart;
    /** Index of the first vertex of each edge, grouped by latitude bucket. */
    private final int[] bucketEdges;
    /** Buckets per degree of latitude. */
    private final double bucketScale;
    private final int bucketCount;

    private PreparedPolygon(float[] xs, float[] ys, int[] ringStart,
                            float minX, float minY, float maxX, float maxY,
                            int[] bucketStart, int[] bucketEdges, double bucketScale, int bucketCount) {
        this.xs = xs;
        this.ys = ys;
        this.ringStart = ringStart;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.bucketStart = bucketStart;
        this.bucketEdges = bucketEdges;
        this.bucketScale = bucketScale;
        this.bucketCount = bucketCount;
    }

    /**
     * Builds a polygon from its protobuf representation.
     *
     * @param proto              the GeoJSON polygon, first ring is the exterior one, the rest are holes
     * @param buildLatitudeIndex whether to spend extra memory on the latitude index
     * @return the prepared polygon, or null if it has no ring enclosing an area
     */
    static PreparedPolygon fromProto(Geojson.Polygon proto, boolean buildLatitudeIndex) {
        List<Geojson.LineString> rings = proto.getCoordinatesList();
        int capacity = 0;
        for (int r = 0; r < rings.size(); r++) {
            capacity += rings.get(r).getCoordinatesCount() + 1;
        }
        float[] xs = new float[capacity];
        float[] ys = new float[capacity];
        int[] starts = new int[rings.size() + 1];

        int n = 0;
        int ringCount = 0;
        for (int r = 0; r < rings.size(); r++) {
            List<Geojson.Position> positions = rings.get(r).getCoordinatesList();
            int start = n;
            for (int i = 0; i < positions.size(); i++) {
                Geojson.Position p = positions.get(i);
                float x = p.getLon();
                float y = p.getLat();
                // Repeated points contribute nothing but slow every query down.
                if (n > start && x == xs[n - 1] && y == ys[n - 1]) {
                    continue;
                }
                xs[n] = x;
                ys[n] = y;
                n++;
            }
            if (n - start < 3) {
                n = start; // Not enough distinct points to enclose an area.
                continue;
            }
            if (xs[n - 1] != xs[start] || ys[n - 1] != ys[start]) {
                xs[n] = xs[start];
                ys[n] = ys[start];
                n++;
            }
            starts[ringCount++] = start;
        }
        if (ringCount == 0) {
            return null;
        }
        starts[ringCount] = n;

        xs = Arrays.copyOf(xs, n);
        ys = Arrays.copyOf(ys, n);
        int[] ringStart = Arrays.copyOf(starts, ringCount + 1);

        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            float x = xs[i], y = ys[i];
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        PreparedPolygon polygon =
                new PreparedPolygon(xs, ys, ringStart, minX, minY, maxX, maxY, null, null, 0, 0);
        return buildLatitudeIndex ? polygon.withLatitudeIndex() : polygon;
    }

    /**
     * Returns a copy of this polygon carrying a latitude index over its edges.
     * <p>
     * The bucket height is picked so that the index stays proportional to the polygon size: it is
     * never finer than the average edge height, which keeps the number of buckets an edge spans
     * (and therefore the number of times it is listed) close to one.
     */
    private PreparedPolygon withLatitudeIndex() {
        double height = (double) maxY - minY;
        int edgeCount = 0;
        double sumEdgeHeight = 0;
        for (int r = 0; r + 1 < ringStart.length; r++) {
            for (int v = ringStart[r], last = ringStart[r + 1] - 1; v < last; v++) {
                edgeCount++;
                sumEdgeHeight += Math.abs(ys[v + 1] - ys[v]);
            }
        }
        if (edgeCount == 0 || height <= 0) {
            return this;
        }

        int maxBuckets = Math.max(1, edgeCount / 2);
        double bucketHeight = Math.max(height / maxBuckets, 2.0 * sumEdgeHeight / edgeCount);
        int buckets = (int) Math.min(maxBuckets, Math.max(1, Math.ceil(height / bucketHeight)));
        double scale = buckets / height;

        // Two passes: count the edges of every bucket, then place them into the flat array.
        int[] starts = new int[buckets + 1];
        for (int r = 0; r + 1 < ringStart.length; r++) {
            for (int v = ringStart[r], last = ringStart[r + 1] - 1; v < last; v++) {
                int from = bucketOf(Math.min(ys[v], ys[v + 1]), scale, buckets);
                int to = bucketOf(Math.max(ys[v], ys[v + 1]), scale, buckets);
                for (int b = from; b <= to; b++) {
                    starts[b + 1]++;
                }
            }
        }
        for (int b = 0; b < buckets; b++) {
            starts[b + 1] += starts[b];
        }

        int[] cursor = Arrays.copyOf(starts, buckets);
        int[] edges = new int[starts[buckets]];
        for (int r = 0; r + 1 < ringStart.length; r++) {
            for (int v = ringStart[r], last = ringStart[r + 1] - 1; v < last; v++) {
                int from = bucketOf(Math.min(ys[v], ys[v + 1]), scale, buckets);
                int to = bucketOf(Math.max(ys[v], ys[v + 1]), scale, buckets);
                for (int b = from; b <= to; b++) {
                    edges[cursor[b]++] = v;
                }
            }
        }
        return new PreparedPolygon(xs, ys, ringStart, minX, minY, maxX, maxY, starts, edges, scale, buckets);
    }

    private int bucketOf(double y, double scale, int buckets) {
        int b = (int) ((y - minY) * scale);
        if (b < 0) {
            return 0;
        }
        return b >= buckets ? buckets - 1 : b;
    }

    private int bucketOf(double y) {
        return bucketOf(y, bucketScale, bucketCount);
    }

    /**
     * Tests whether the given point belongs to this polygon, that is, whether it is enclosed by the
     * outer ring, outside of every hole, or within {@link #TOLERANCE} of the outline.
     *
     * @param x longitude of the point
     * @param y latitude of the point
     */
    boolean contains(double x, double y) {
        if (x < minX - TOLERANCE || x > maxX + TOLERANCE || y < minY - TOLERANCE || y > maxY + TOLERANCE) {
            return false;
        }
        if (bucketEdges == null) {
            return isEnclosedByAllEdges(x, y) || touchesAnyEdge(x, y);
        }
        int bucket = bucketOf(y);
        if (isEnclosedByBucket(x, y, bucket)) {
            return true;
        }
        // Only a point that is not enclosed can still be sitting on the outline, and it can only do
        // so within a hair of its own latitude, so at most two buckets are worth looking at.
        return touchesEdgeInBuckets(x, y, bucketOf(y - TOLERANCE), bucketOf(y + TOLERANCE));
    }

    /**
     * Counts, using the even-odd rule, how often a ray cast towards increasing longitude crosses
     * the outline. The half-open comparison {@code (y1 > y) != (y2 > y)} makes a vertex shared by
     * two edges count exactly once, which is what keeps the rule consistent.
     */
    private boolean isEnclosedByBucket(double x, double y, int bucket) {
        final float[] xs = this.xs;
        final float[] ys = this.ys;
        final int[] edges = this.bucketEdges;
        boolean inside = false;
        for (int i = bucketStart[bucket], end = bucketStart[bucket + 1]; i < end; i++) {
            int v = edges[i];
            double y1 = ys[v], y2 = ys[v + 1];
            if ((y1 > y) != (y2 > y) && crossesToTheRight(x, y, xs[v], y1, xs[v + 1], y2)) {
                inside = !inside;
            }
        }
        return inside;
    }

    /** Same rule as {@link #isEnclosedByBucket}, for polygons built without a latitude index. */
    private boolean isEnclosedByAllEdges(double x, double y) {
        final float[] xs = this.xs;
        final float[] ys = this.ys;
        boolean inside = false;
        for (int r = 0; r + 1 < ringStart.length; r++) {
            for (int v = ringStart[r], last = ringStart[r + 1] - 1; v < last; v++) {
                double y1 = ys[v], y2 = ys[v + 1];
                if ((y1 > y) != (y2 > y) && crossesToTheRight(x, y, xs[v], y1, xs[v + 1], y2)) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    /** Whether the point lies within {@link #TOLERANCE} of an edge in the given bucket range. */
    private boolean touchesEdgeInBuckets(double x, double y, int fromBucket, int toBucket) {
        final float[] xs = this.xs;
        final float[] ys = this.ys;
        final int[] edges = this.bucketEdges;
        for (int i = bucketStart[fromBucket], end = bucketStart[toBucket + 1]; i < end; i++) {
            int v = edges[i];
            if (isNearEdge(x, y, xs[v], ys[v], xs[v + 1], ys[v + 1])) {
                return true;
            }
        }
        return false;
    }

    /** Same test as {@link #touchesEdgeInBuckets}, for polygons built without a latitude index. */
    private boolean touchesAnyEdge(double x, double y) {
        final float[] xs = this.xs;
        final float[] ys = this.ys;
        for (int r = 0; r + 1 < ringStart.length; r++) {
            for (int v = ringStart[r], last = ringStart[r + 1] - 1; v < last; v++) {
                if (isNearEdge(x, y, xs[v], ys[v], xs[v + 1], ys[v + 1])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tells whether the edge from {@code (x1, y1)} to {@code (x2, y2)}, already known to span the
     * latitude {@code y}, crosses it to the right of {@code x}. Equivalent to comparing {@code x}
     * with the edge's longitude at latitude {@code y}, but expressed as a cross product to keep the
     * division out of the inner loop.
     */
    private static boolean crossesToTheRight(double x, double y, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double side = (x - x1) * dy - (y - y1) * dx;
        return dy > 0 ? side < 0 : side > 0;
    }

    private static boolean isNearEdge(double x, double y, double x1, double y1, double x2, double y2) {
        // Reject the vast majority of edges on their bounding box before doing any arithmetic.
        if (x1 < x2 ? (x < x1 - TOLERANCE || x > x2 + TOLERANCE) : (x < x2 - TOLERANCE || x > x1 + TOLERANCE)) {
            return false;
        }
        if (y1 < y2 ? (y < y1 - TOLERANCE || y > y2 + TOLERANCE) : (y < y2 - TOLERANCE || y > y1 + TOLERANCE)) {
            return false;
        }
        double dx = x2 - x1;
        double dy = y2 - y1;
        double projection = ((x - x1) * dx + (y - y1) * dy) / (dx * dx + dy * dy);
        if (projection < 0) {
            projection = 0;
        } else if (projection > 1) {
            projection = 1;
        }
        double offsetX = x1 + projection * dx - x;
        double offsetY = y1 + projection * dy - y;
        return offsetX * offsetX + offsetY * offsetY <= TOLERANCE_SQUARED;
    }

    /** Whether this polygon's bounding box lies entirely within the given one. */
    boolean isWithin(double boxMinX, double boxMinY, double boxMaxX, double boxMaxY) {
        return minX >= boxMinX && maxX <= boxMaxX && minY >= boxMinY && maxY <= boxMaxY;
    }
}
