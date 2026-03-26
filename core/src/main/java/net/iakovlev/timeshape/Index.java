package net.iakovlev.timeshape;

import com.esri.core.geometry.*;
import net.iakovlev.timeshape.proto.Geojson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class Index implements Serializable {
    static final class Entry implements Serializable {
        final ZoneId zoneId;
        final Geometry geometry;

        Entry(ZoneId zoneId, Geometry geometry) {
            this.zoneId = zoneId;
            this.geometry = geometry;
        }
    }

    private static final int WGS84_WKID = 4326;
    private final ArrayList<Entry> zoneIds;
    private static final SpatialReference spatialReference = SpatialReference.create(WGS84_WKID);
    private final QuadTree quadTree;
    private static final Logger log = LoggerFactory.getLogger(Index.class);

    private Index(QuadTree quadTree, ArrayList<Entry> zoneIds) {
        log.info("Initialized index with {} time zones", zoneIds.size());
        this.quadTree = quadTree;
        this.zoneIds = zoneIds;
    }

    List<ZoneId> getKnownZoneIds() {
        return zoneIds.stream().map(e -> e.zoneId).collect(Collectors.toList());
    }

    List<ZoneId> query(double latitude, double longitude) {
        ArrayList<ZoneId> result = new ArrayList<>(2);
        Point point = new Point(longitude, latitude);
        OperatorIntersects operator = OperatorIntersects.local();
        QuadTree.QuadTreeIterator iterator = quadTree.getIterator(point, 0);
        for (int i = iterator.next(); i >= 0; i = iterator.next()) {
            int element = quadTree.getElement(i);
            Entry entry = zoneIds.get(element);
            if(operator.execute(entry.geometry, point, spatialReference, null)) {
                result.add(entry.zoneId);
            }
        }
        return result;
    }

    List<SameZoneSpan> queryPolyline(double[] line) {

        Polyline polyline = new Polyline();
        ArrayList<Point> points = new ArrayList<>(line.length / 2);
        for (int i = 0; i < line.length - 1; i += 2) {
            Point p = new Point(line[i + 1], line[i]);
            points.add(p);
        }
        polyline.startPath(points.get(0));
        for (int i = 1; i < points.size(); i += 1) {
            polyline.lineTo(points.get(i));
        }
        QuadTree.QuadTreeIterator iterator = quadTree.getIterator(polyline, 0);

        ArrayList<Entry> potentiallyMatchingEntries = new ArrayList<>();

        for (int i = iterator.next(); i >= 0; i = iterator.next()) {
            int element = quadTree.getElement(i);
            Entry entry = zoneIds.get(element);
            potentiallyMatchingEntries.add(entry);
        }

        ArrayList<SameZoneSpan> sameZoneSegments = new ArrayList<>();
        List<Entry> currentEntry = null;
        // 1. find next matching geometry or geometries
        // 2. for every match, increase the index
        // 3. when it doesn't match anymore, save currentSegment to sameZoneSegments and start new one
        // 4. goto 1.
        int index = 0;
        boolean lastWasEmpty = false;
        OperatorIntersects operator = OperatorIntersects.local();
        while (index < points.size()) {
            Point p = points.get(index);
            if (currentEntry == null) {
                currentEntry = potentiallyMatchingEntries
                        .stream()
                        .filter(e -> operator.execute(e.geometry, p, spatialReference, null))
                        .collect(Collectors.toList());
            }
            if (currentEntry.isEmpty()) {
                currentEntry = null;
                lastWasEmpty = true;
                index++;
            } else {
                if (lastWasEmpty) {
                    lastWasEmpty = false;
                    sameZoneSegments.add(SameZoneSpan.fromIndexEntries(Collections.emptyList(), (index - 1) * 2 + 1));
                    continue;
                }
                if (currentEntry.stream().allMatch(e -> operator.execute(e.geometry, p, spatialReference, null))) {
                    if (index == points.size() - 1) {
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
            sameZoneSegments.add(SameZoneSpan.fromIndexEntries(Collections.emptyList(), index * 2 - 1));
        }

        return sameZoneSegments;
    }

    private static Polygon buildPoly(Geojson.Polygon from) {
        Polygon poly = new Polygon();
        from.getCoordinatesList().stream()
                .map(Geojson.LineString::getCoordinatesList)
                .forEachOrdered(lp -> {
                    poly.startPath(lp.get(0).getLon(), lp.get(0).getLat());
                    lp.subList(1, lp.size()).forEach(p -> poly.lineTo(p.getLon(), p.getLat()));
                });
        return poly;
    }

    static Index build(Stream<Geojson.Feature> features, int size, Envelope boundaries) {
        return build(features, size, boundaries, false);
    }

    private static Stream<Polygon> getPolygons(Geojson.Feature f) {
        if (f.getGeometry().hasPolygon()) {
            return Stream.of(buildPoly(f.getGeometry().getPolygon()));
        } else if (f.getGeometry().hasMultiPolygon()) {
            Geojson.MultiPolygon multiPolygonProto = f.getGeometry().getMultiPolygon();
            return multiPolygonProto.getCoordinatesList().stream().map(Index::buildPoly);
        } else {
            throw new RuntimeException("Unknown geometry type");
        }
    }

    @SuppressWarnings("SizeReplaceableByIsEmpty")
    static Index build(Stream<Geojson.Feature> features, int size, Envelope boundaries, boolean accelerateGeometry) {
        Envelope2D boundariesEnvelope = new Envelope2D();
        boundaries.queryEnvelope2D(boundariesEnvelope);
        QuadTree quadTree = new QuadTree(boundariesEnvelope, 8);
        Envelope2D env = new Envelope2D();
        ArrayList<Entry> zoneIds = new ArrayList<>(size);
        PrimitiveIterator.OfInt indices = IntStream.iterate(0, i -> i + 1).iterator();
        List<String> unknownZones = new ArrayList<>();
        OperatorIntersects operatorIntersects = OperatorIntersects.local();
        features.forEach(f -> {
            String zoneIdName = f.getProperties(0).getValueString();
            try {
                ZoneId zoneId = ZoneId.of(zoneIdName);
                getPolygons(f).forEach(polygon -> {
                    if (GeometryEngine.contains(boundaries, polygon, spatialReference)) {
                        log.debug("Adding zone {} to index", zoneIdName);
                        if (accelerateGeometry) {
                            operatorIntersects.accelerateGeometry(polygon, spatialReference, Geometry.GeometryAccelerationDegree.enumMild);
                        }
                        polygon.queryEnvelope2D(env);
                        int index = indices.next();
                        quadTree.insert(index, env);
                        zoneIds.add(index, new Entry(zoneId, polygon));
                    } else {
                        log.debug("Not adding zone {} to index because it's out of provided boundaries", zoneIdName);
                    }
                });
            } catch (Exception ex) {
                unknownZones.add(zoneIdName);
            }
        });
        if (unknownZones.size() != 0) {
            String allUnknownZones = String.join(", ", unknownZones);
            log.error(
                    "Some of the zone ids were not recognized by the Java runtime and will be ignored. " +
                            "The most probable reason for this is outdated Java runtime version. " +
                            "The following zones were not recognized: " + allUnknownZones);
        }
        return new Index(quadTree, zoneIds);
    }

    
    @SuppressWarnings("SizeReplaceableByIsEmpty")
    static Index build(Stream<Geojson.Feature> features, int size, Set<ZoneId> timeZones, boolean accelerateGeometry) {
        Envelope2D boundariesEnvelope = new Envelope2D();
        Envelope boundaries = new Envelope(-180, -90, 180, +90);// (minLon, minLat, maxLon, maxLat);
        boundaries.queryEnvelope2D(boundariesEnvelope);
        QuadTree quadTree = new QuadTree(boundariesEnvelope, 8);
        Envelope2D env = new Envelope2D();
        ArrayList<Entry> zoneIds = new ArrayList<>(size);
        PrimitiveIterator.OfInt indices = IntStream.iterate(0, i -> i + 1).iterator();
        List<String> unknownZones = new ArrayList<>();
        OperatorIntersects operatorIntersects = OperatorIntersects.local();
        features.forEach(f -> {
            String zoneIdName = f.getProperties(0).getValueString();
            try {
                ZoneId zoneId = ZoneId.of(zoneIdName);
                if (timeZones.contains (zoneId)) {
                    getPolygons(f).forEach(polygon -> {
                        log.debug("Adding zone {} to index", zoneIdName);
                        if (accelerateGeometry) {
                            operatorIntersects.accelerateGeometry(polygon, spatialReference, Geometry.GeometryAccelerationDegree.enumMild);
                        }
                        polygon.queryEnvelope2D(env);
                        int index = indices.next();
                        quadTree.insert(index, env);
                        zoneIds.add(index, new Entry(zoneId, polygon));
                    });
                } else {
                    log.debug("Not adding zone {} to index because it's out of provided boundaries", zoneIdName);
                }
            } catch (Exception ex) {
                unknownZones.add(zoneIdName);
            }
        });
        if (unknownZones.size() != 0) {
            String allUnknownZones = String.join(", ", unknownZones);
            log.error(
                    "Some of the zone ids were not recognized by the Java runtime and will be ignored. " +
                            "The most probable reason for this is outdated Java runtime version. " +
                            "The following zones were not recognized: " + allUnknownZones);
        }
        return new Index(quadTree, zoneIds);
    }
}
