package net.iakovlev.timeshape;

import com.esri.core.geometry.*;
import net.iakovlev.timeshape.proto.Geojson;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class Index {
    static private final class Entry {
        final ZoneId zoneId;
        final Geometry geometry;

        Entry(ZoneId zoneId, Geometry geometry) {
            this.zoneId = zoneId;
            this.geometry = geometry;
        }
    }

    private final ArrayList<Entry> zoneIds;
    private final SpatialReference spatialReference;
    private final QuadTree quadTree;

    private Index(QuadTree quadTree, ArrayList<Entry> zoneIds) {
        int WGS84_WKID = 4326;
        this.quadTree = quadTree;
        this.zoneIds = zoneIds;
        this.spatialReference = SpatialReference.create(WGS84_WKID);
    }

    List<ZoneId> getKnownZoneIds() {
        return zoneIds.stream().map(e -> e.zoneId).collect(Collectors.toList());
    }

    Optional<ZoneId> query(double latitude, double longitude) {
        Point point = new Point(longitude, latitude);
        QuadTree.QuadTreeIterator iterator = quadTree.getIterator(point, 0);
        for (int i = iterator.next(); i >= 0; i = iterator.next()) {
            int element = quadTree.getElement(i);
            Entry entry = zoneIds.get(element);
            if (GeometryEngine.contains(entry.geometry, point, spatialReference)) {
                return Optional.of(entry.zoneId);
            }
        }
        return Optional.empty();
    }

    private static void buildPoly(Geojson.Polygon from, Polygon poly) {
        from.getCoordinatesList().stream()
            .map(Geojson.LineString::getCoordinatesList)
            .forEachOrdered(lp -> {
                poly.startPath(lp.get(0).getLon(), lp.get(0).getLat());
                lp.subList(1, lp.size()).forEach(p -> poly.lineTo(p.getLon(), p.getLat()));
            });
    }

    static Index build(Stream<Geojson.Feature> features, int size, Envelope2D boundaries) {
        QuadTree quadTree = new QuadTree(boundaries, 8);
        Envelope2D env = new Envelope2D();
        ArrayList<Entry> zoneIds = new ArrayList<>(size);
        PrimitiveIterator.OfInt indices = IntStream.iterate(0, i -> i + 1).iterator();
        features.forEach(f -> {
            Polygon polygon = new Polygon();
            if (f.getGeometry().hasPolygon()) {
                Geojson.Polygon polygonProto = f.getGeometry().getPolygon();
                buildPoly(polygonProto, polygon);
            } else if (f.getGeometry().hasMultiPolygon()) {
                Geojson.MultiPolygon multiPolygonProto = f.getGeometry().getMultiPolygon();
                multiPolygonProto.getCoordinatesList().forEach(lp -> buildPoly(lp, polygon));
            }
            polygon.queryEnvelope2D(env);
            if (boundaries.contains(env)) {
                int index = indices.next();
                quadTree.insert(index, env);
                zoneIds.add(index, new Entry(ZoneId.of(f.getProperties(0).getValueString()), polygon));
            }
        });
        return new Index(quadTree, zoneIds);
    }

}
