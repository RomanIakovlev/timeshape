package net.iakovlev.timeshape;

import com.esri.core.geometry.*;
import net.iakovlev.timeshape.proto.Geojson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
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
    private static final Logger log = LoggerFactory.getLogger(Index.class);

    private Index(QuadTree quadTree, ArrayList<Entry> zoneIds) {
        log.info("Initialized index with {} time zones", zoneIds.size());
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

    ArrayList<SameZoneSpan> query2(double[] line) {
        ArrayList<SameZoneSpan> sameZoneSegments = new ArrayList<>();
        Entry currentEntry = null;
        ArrayList<Point> points = new ArrayList<>(line.length / 2);
        for (int i = 0; i < line.length - 1; i += 2) {
            Point p = new Point(line[i + 1], line[i]);
            points.add(p);
        }
        log.info("Got " + points.size() + " points");
        QuadTree.QuadTreeIterator iterator = quadTree.getIterator();
        int index = 0;
        while (index < points.size()) {
            Point p = points.get(index);
            if (currentEntry == null) {
                iterator.resetIterator(p, 0.0);
                for (int i = iterator.next(); i >= 0; i = iterator.next()) {
                    int element = quadTree.getElement(i);
                    Entry entry = zoneIds.get(element);
                    if (GeometryEngine.contains(entry.geometry, p, spatialReference)) {
                        currentEntry = entry;
                        break;
                    }
                }
                if (currentEntry == null) {
                    throw new RuntimeException("no matching geometries found");
                }
            }
            if (GeometryEngine.contains(currentEntry.geometry, p, spatialReference)) {
                if (index == points.size() - 1) {
//                    log.info("Last segment is complete");
                    sameZoneSegments.add(new SameZoneSpan(currentEntry.zoneId, index));
                }
                index++;
            } else {
                sameZoneSegments.add(new SameZoneSpan(currentEntry.zoneId, index - 1));
                log.info("point {}, at index {} belongs to next span", p.toString(), index);
                currentEntry = null;
            }
        }
        return sameZoneSegments;
    }

    ArrayList<SameZoneSegment> query1(double[] line) {
        ArrayList<SameZoneSegment> sameZoneSegments = new ArrayList<>();
        ArrayList<Point> currentSegment = new ArrayList<>();
        Entry currentEntry = null;
        ArrayList<Point> points = new ArrayList<>(line.length / 2);
        for (int i = 0; i < line.length - 1; i += 2) {
            Point p = new Point(line[i + 1], line[i]);
            points.add(p);
        }
        QuadTree.QuadTreeIterator iterator = quadTree.getIterator();
        int index = 0;
        while (index < points.size()) {
            Point p = points.get(index++);
            if (currentEntry == null) {
                iterator.resetIterator(p, 0.0);
                for (int i = iterator.next(); i >= 0; i = iterator.next()) {
                    int element = quadTree.getElement(i);
                    Entry entry = zoneIds.get(element);
                    if (GeometryEngine.contains(entry.geometry, p, spatialReference)) {
                        currentEntry = entry;
                        break;
                    }
                }
                if (currentEntry == null) {
                    throw new RuntimeException("no matching geometries found");
                }
            }
            if (GeometryEngine.contains(currentEntry.geometry, p, spatialReference)) {
                currentSegment.add(p);
                if (index == points.size()) {
//                    log.info("Last segment is complete");
                    double[] doubles = currentSegment
                            .stream()
                            .flatMapToDouble(pp -> DoubleStream.of(pp.getY(), pp.getX()))
                            .toArray();
                    sameZoneSegments.add(new SameZoneSegment(currentEntry.zoneId, doubles));
                }
            } else {
                index--;
                double[] doubles = currentSegment
                        .stream()
                        .flatMapToDouble(pp -> DoubleStream.of(pp.getY(), pp.getX()))
                        .toArray();
                sameZoneSegments.add(new SameZoneSegment(currentEntry.zoneId, doubles));
                currentSegment = new ArrayList<>();
                currentEntry = null;
            }
        }
        return sameZoneSegments;
    }

    ArrayList<SameZoneSegment> query(double[] line) {

        Polyline polyline = new Polyline();
        ArrayList<Point> points = new ArrayList<>(line.length / 2);
        for (int i = 0; i < line.length - 1; i += 2) {
            Point p = new Point(line[i + 1], line[i]);
            points.add(p);
        }
//        log.info("Got " + points.size() + " points");
        polyline.startPath(points.get(0));
        for (int i = 1; i < points.size(); i += 1) {
            polyline.lineTo(points.get(i));
        }
//        log.info("Got " + polyline.getSegmentCount() + " segments");
        QuadTree.QuadTreeIterator iterator = quadTree.getIterator(polyline, 0);

        ArrayList<Entry> potentiallyMatchingEntries = new ArrayList<>();

        for (int i = iterator.next(); i >= 0; i = iterator.next()) {
            int element = quadTree.getElement(i);
            Entry entry = zoneIds.get(element);
            potentiallyMatchingEntries.add(entry);
        }

//        log.info("Got " + potentiallyMatchingEntries.size() + " potentially matching geometries");

        ArrayList<SameZoneSegment> sameZoneSegments = new ArrayList<>();
        ArrayList<Point> currentSegment = new ArrayList<>();
        Entry currentEntry = null;
        // 1. find next matching geometry
        // 2. match against same geometry until it doesn't match anymore
        // 3. for every match, add to currentSegment
        // 4. when it doesn't match anymore, save currentSegment to sameZoneSegments and start new one
        // 5. goto 1.
        int index = 0;
        while (index < points.size()) {
            Point p = points.get(index++);
//            log.info("Processing point {} at index {}", p.toString(), index);
            if (currentEntry == null) {
//                log.info("currentEntry is null");
                // find next matching geometry
                for (Entry e : potentiallyMatchingEntries) {
                    if (GeometryEngine.contains(e.geometry, p, spatialReference)) {
                        currentEntry = e;
//                        log.info("Found next matching geometry: {}", e.zoneId.getId());
                        break;
                    }
                }
                if (currentEntry == null) {
                    throw new RuntimeException("no matching geometries found");
                }
            }
            if (GeometryEngine.contains(currentEntry.geometry, p, spatialReference)) {
                currentSegment.add(p);
//                log.info("Adding point {} to current segment", p.toString());
                if (index == points.size()) {
//                    log.info("Last segment is complete");
                    double[] doubles = currentSegment
                            .stream()
                            .flatMapToDouble(pp -> DoubleStream.of(pp.getY(), pp.getX()))
                            .toArray();
                    sameZoneSegments.add(new SameZoneSegment(currentEntry.zoneId, doubles));
                }
            } else {
                index--;
                double[] doubles = currentSegment
                        .stream()
                        .flatMapToDouble(pp -> DoubleStream.of(pp.getY(), pp.getX()))
                        .toArray();

                sameZoneSegments.add(new SameZoneSegment(currentEntry.zoneId, doubles));
                currentSegment = new ArrayList<>();
                currentEntry = null;
//                log.info("Current segment is complete, starting new one");
            }
        }

        return sameZoneSegments;
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
        List<String> unknownZones = new ArrayList<>();
        features.forEach(f -> {
            String zoneIdName = f.getProperties(0).getValueString();
            ZoneId zoneId = null;
            try {
                zoneId = ZoneId.of(zoneIdName);
            } catch (Exception ex) {
                unknownZones.add(zoneIdName);
            }
            if (zoneId != null) {
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
                    log.debug("Adding zone {} to index", zoneIdName);
                    int index = indices.next();
                    quadTree.insert(index, env);
                    zoneIds.add(index, new Entry(zoneId, polygon));
                } else {
                    log.debug("Not adding zone {} to index because it's out of provided boundaries");
                }
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
