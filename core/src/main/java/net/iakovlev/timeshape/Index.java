package net.iakovlev.timeshape;

import net.iakovlev.timeshape.proto.Geojson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Index implements Serializable {

    private static final long serialVersionUID = 1L;

    static final class Entry implements Serializable {
        private static final long serialVersionUID = 1L;

        final ZoneId zoneId;
        final PreparedPolygon geometry;

        Entry(ZoneId zoneId, PreparedPolygon geometry) {
            this.zoneId = zoneId;
            this.geometry = geometry;
        }
    }

    /**
     * A uniform grid over the indexed area, mapping each cell to the polygons whose bounding box
     * overlaps it. Looking up the candidates for a point is a bit of arithmetic and one array
     * offset, with no tree to descend and nothing to allocate.
     */
    static final class Grid implements Serializable {
        private static final long serialVersionUID = 1L;

        /** Roughly one degree per cell, which keeps the world grid at 360x180 cells. */
        private static final double TARGET_CELL_SIZE_DEGREES = 1.0;
        private static final int MAX_CELLS_PER_AXIS = 1024;

        private final double minX;
        private final double minY;
        private final double maxX;
        private final double maxY;
        private final int cellsX;
        private final int cellsY;
        private final double cellsPerDegreeX;
        private final double cellsPerDegreeY;
        /** Cell {@code c} owns {@code [cellStart[c], cellStart[c + 1])} of {@link #cellPolygons}. */
        private final int[] cellStart;
        /** Polygon indices, ascending within every cell. */
        private final int[] cellPolygons;

        private Grid(double minX, double minY, double maxX, double maxY,
                     int cellsX, int cellsY, int[] cellStart, int[] cellPolygons) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.cellsX = cellsX;
            this.cellsY = cellsY;
            this.cellsPerDegreeX = cellsX / (maxX - minX);
            this.cellsPerDegreeY = cellsY / (maxY - minY);
            this.cellStart = cellStart;
            this.cellPolygons = cellPolygons;
        }

        static Grid build(List<Entry> entries) {
            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < entries.size(); i++) {
                PreparedPolygon p = entries.get(i).geometry;
                if (p.minX < minX) minX = p.minX;
                if (p.maxX > maxX) maxX = p.maxX;
                if (p.minY < minY) minY = p.minY;
                if (p.maxY > maxY) maxY = p.maxY;
            }
            if (entries.isEmpty()) {
                return new Grid(0, 0, 1, 1, 1, 1, new int[]{0, 0}, new int[0]);
            }
            // Keep the extent non-degenerate so that the cell size stays a finite number.
            if (maxX <= minX) {
                maxX = minX + 1.0E-6;
            }
            if (maxY <= minY) {
                maxY = minY + 1.0E-6;
            }

            int cellsX = cellCount(maxX - minX);
            int cellsY = cellCount(maxY - minY);
            double perDegreeX = cellsX / (maxX - minX);
            double perDegreeY = cellsY / (maxY - minY);
            int cellCount = cellsX * cellsY;

            // Two passes: count how many polygons land in every cell, then fill the flat array.
            int[] cellStart = new int[cellCount + 1];
            for (int i = 0; i < entries.size(); i++) {
                PreparedPolygon p = entries.get(i).geometry;
                int fromX = clamp((p.minX - minX) * perDegreeX, cellsX), toX = clamp((p.maxX - minX) * perDegreeX, cellsX);
                int fromY = clamp((p.minY - minY) * perDegreeY, cellsY), toY = clamp((p.maxY - minY) * perDegreeY, cellsY);
                for (int cy = fromY; cy <= toY; cy++) {
                    for (int cx = fromX; cx <= toX; cx++) {
                        cellStart[cy * cellsX + cx + 1]++;
                    }
                }
            }
            for (int c = 0; c < cellCount; c++) {
                cellStart[c + 1] += cellStart[c];
            }

            int[] cursor = new int[cellCount];
            System.arraycopy(cellStart, 0, cursor, 0, cellCount);
            int[] cellPolygons = new int[cellStart[cellCount]];
            for (int i = 0; i < entries.size(); i++) {
                PreparedPolygon p = entries.get(i).geometry;
                int fromX = clamp((p.minX - minX) * perDegreeX, cellsX), toX = clamp((p.maxX - minX) * perDegreeX, cellsX);
                int fromY = clamp((p.minY - minY) * perDegreeY, cellsY), toY = clamp((p.maxY - minY) * perDegreeY, cellsY);
                for (int cy = fromY; cy <= toY; cy++) {
                    for (int cx = fromX; cx <= toX; cx++) {
                        cellPolygons[cursor[cy * cellsX + cx]++] = i;
                    }
                }
            }
            return new Grid(minX, minY, maxX, maxY, cellsX, cellsY, cellStart, cellPolygons);
        }

        private static int clamp(double cell, int cellCount) {
            int c = (int) cell;
            if (c < 0) return 0;
            return c >= cellCount ? cellCount - 1 : c;
        }

        private static int cellCount(double extentDegrees) {
            int count = (int) Math.ceil(extentDegrees / TARGET_CELL_SIZE_DEGREES);
            return Math.max(1, Math.min(MAX_CELLS_PER_AXIS, count));
        }

        /** Index of the cell containing the point, or -1 if the point is outside the indexed area. */
        int cellOf(double x, double y) {
            if (x < minX || x > maxX || y < minY || y > maxY) {
                return -1;
            }
            return clamp((y - minY) * cellsPerDegreeY, cellsY) * cellsX + clamp((x - minX) * cellsPerDegreeX, cellsX);
        }

        int firstPolygonOf(int cell) {
            return cellStart[cell];
        }

        int lastPolygonOf(int cell) {
            return cellStart[cell + 1];
        }

        int polygonAt(int position) {
            return cellPolygons[position];
        }
    }

    private final ArrayList<Entry> zoneIds;
    private final Grid grid;
    private static final Logger log = LoggerFactory.getLogger(Index.class);

    private Index(Grid grid, ArrayList<Entry> zoneIds) {
        log.info("Initialized index with {} time zones", zoneIds.size());
        this.grid = grid;
        this.zoneIds = zoneIds;
    }

    List<ZoneId> getKnownZoneIds() {
        return zoneIds.stream().map(e -> e.zoneId).collect(Collectors.toList());
    }

    List<ZoneId> query(double latitude, double longitude) {
        int cell = grid.cellOf(longitude, latitude);
        if (cell < 0) {
            return Collections.emptyList();
        }
        ZoneId first = null;
        ArrayList<ZoneId> result = null;
        for (int i = grid.firstPolygonOf(cell), end = grid.lastPolygonOf(cell); i < end; i++) {
            Entry entry = zoneIds.get(grid.polygonAt(i));
            if (entry.geometry.contains(longitude, latitude)) {
                if (first == null) {
                    first = entry.zoneId;
                } else {
                    if (result == null) {
                        result = new ArrayList<>(2);
                        result.add(first);
                    }
                    result.add(entry.zoneId);
                }
            }
        }
        if (result != null) {
            return result;
        }
        return first == null ? Collections.emptyList() : Collections.singletonList(first);
    }

    /**
     * Collects the entries covering a point. Returns an empty list rather than null when nothing
     * matches, so callers can treat the result uniformly.
     */
    private List<Entry> entriesAt(double longitude, double latitude) {
        int cell = grid.cellOf(longitude, latitude);
        if (cell < 0) {
            return Collections.emptyList();
        }
        List<Entry> matching = null;
        for (int i = grid.firstPolygonOf(cell), end = grid.lastPolygonOf(cell); i < end; i++) {
            Entry entry = zoneIds.get(grid.polygonAt(i));
            if (entry.geometry.contains(longitude, latitude)) {
                if (matching == null) {
                    matching = new ArrayList<>(2);
                }
                matching.add(entry);
            }
        }
        return matching == null ? Collections.<Entry>emptyList() : matching;
    }

    private static boolean allContain(List<Entry> entries, double longitude, double latitude) {
        for (int i = 0; i < entries.size(); i++) {
            if (!entries.get(i).geometry.contains(longitude, latitude)) {
                return false;
            }
        }
        return true;
    }

    List<SameZoneSpan> queryPolyline(double[] line) {
        ArrayList<SameZoneSpan> sameZoneSegments = new ArrayList<>();
        int pointCount = line.length / 2;
        if (pointCount == 0) {
            return sameZoneSegments;
        }

        List<Entry> currentEntry = null;
        // 1. find next matching geometry or geometries
        // 2. for every match, increase the index
        // 3. when it doesn't match anymore, save currentSegment to sameZoneSegments and start new one
        // 4. goto 1.
        int index = 0;
        boolean lastWasEmpty = false;
        // The entries were just looked up by containment at this very point, so re-testing them
        // would be pure repetition.
        boolean freshlyMatched = false;
        while (index < pointCount) {
            double latitude = line[index * 2];
            double longitude = line[index * 2 + 1];
            if (currentEntry == null) {
                currentEntry = entriesAt(longitude, latitude);
                freshlyMatched = true;
            }
            if (currentEntry.isEmpty()) {
                currentEntry = null;
                lastWasEmpty = true;
                index++;
            } else {
                if (lastWasEmpty) {
                    lastWasEmpty = false;
                    sameZoneSegments.add(SameZoneSpan.fromIndexEntries(Collections.<Entry>emptyList(), (index - 1) * 2 + 1));
                    continue;
                }
                if (freshlyMatched || allContain(currentEntry, longitude, latitude)) {
                    freshlyMatched = false;
                    if (index == pointCount - 1) {
                        sameZoneSegments.add(SameZoneSpan.fromIndexEntries(currentEntry, index * 2 + 1));
                    }
                    index++;
                } else {
                    sameZoneSegments.add(SameZoneSpan.fromIndexEntries(currentEntry, (index - 1) * 2 + 1));
                    currentEntry = null;
                }
            }
        }

        if (lastWasEmpty) {
            sameZoneSegments.add(SameZoneSpan.fromIndexEntries(Collections.<Entry>emptyList(), index * 2 - 1));
        }

        return sameZoneSegments;
    }

    private static Stream<Geojson.Polygon> getPolygons(Geojson.Feature f) {
        if (f.getGeometry().hasPolygon()) {
            return Stream.of(f.getGeometry().getPolygon());
        } else if (f.getGeometry().hasMultiPolygon()) {
            return f.getGeometry().getMultiPolygon().getCoordinatesList().stream();
        } else {
            throw new RuntimeException("Unknown geometry type");
        }
    }

    static Index build(Stream<Geojson.Feature> features, int size,
                       double minLat, double minLon, double maxLat, double maxLon) {
        return build(features, size, minLat, minLon, maxLat, maxLon, false);
    }

    static Index build(Stream<Geojson.Feature> features, int size,
                       double minLat, double minLon, double maxLat, double maxLon,
                       boolean accelerateGeometry) {
        ArrayList<Entry> zoneIds = new ArrayList<>(size);
        List<String> unknownZones = new ArrayList<>();
        features.forEach(f -> {
            String zoneIdName = f.getProperties(0).getValueString();
            ZoneId zoneId;
            try {
                zoneId = ZoneId.of(zoneIdName);
            } catch (Exception ex) {
                unknownZones.add(zoneIdName);
                return;
            }
            getPolygons(f).forEach(protoPolygon -> {
                PreparedPolygon polygon = PreparedPolygon.fromProto(protoPolygon, accelerateGeometry);
                // A zone is only indexed when it fits into the requested boundaries entirely, which
                // for an axis aligned box is exactly the same as its bounding box fitting.
                if (polygon != null && polygon.isWithin(minLon, minLat, maxLon, maxLat)) {
                    log.debug("Adding zone {} to index", zoneIdName);
                    zoneIds.add(new Entry(zoneId, polygon));
                } else {
                    log.debug("Not adding zone {} to index because it's out of provided boundaries", zoneIdName);
                }
            });
        });
        reportUnknownZones(unknownZones);
        return new Index(Grid.build(zoneIds), zoneIds);
    }

    static Index build(Stream<Geojson.Feature> features, int size, Set<ZoneId> timeZones, boolean accelerateGeometry) {
        ArrayList<Entry> zoneIds = new ArrayList<>(size);
        List<String> unknownZones = new ArrayList<>();
        features.forEach(f -> {
            String zoneIdName = f.getProperties(0).getValueString();
            ZoneId zoneId;
            try {
                zoneId = ZoneId.of(zoneIdName);
            } catch (Exception ex) {
                unknownZones.add(zoneIdName);
                return;
            }
            if (!timeZones.contains(zoneId)) {
                log.debug("Not adding zone {} to index because it's not in the requested set", zoneIdName);
                return;
            }
            getPolygons(f).forEach(protoPolygon -> {
                PreparedPolygon polygon = PreparedPolygon.fromProto(protoPolygon, accelerateGeometry);
                if (polygon != null) {
                    log.debug("Adding zone {} to index", zoneIdName);
                    zoneIds.add(new Entry(zoneId, polygon));
                }
            });
        });
        reportUnknownZones(unknownZones);
        return new Index(Grid.build(zoneIds), zoneIds);
    }

    private static void reportUnknownZones(List<String> unknownZones) {
        if (!unknownZones.isEmpty()) {
            String allUnknownZones = String.join(", ", unknownZones);
            log.error(
                    "Some of the zone ids were not recognized by the Java runtime and will be ignored. " +
                            "The most probable reason for this is outdated Java runtime version. " +
                            "The following zones were not recognized: " + allUnknownZones);
        }
    }
}
