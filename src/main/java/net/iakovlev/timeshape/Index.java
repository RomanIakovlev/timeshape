package net.iakovlev.timeshape;

import com.esri.core.geometry.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.FeatureCollection;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class Index {
    static final class Entry {
        ZoneId zoneId;
        Polygon geometry;

        Entry(ZoneId zoneId, Polygon geometry) {
            this.zoneId = zoneId;
            this.geometry = geometry;
        }
    }

    private final ArrayList<Entry> zoneIds;
    private final SpatialReference spatialReference;
    private final QuadTree quadTree;
    private final QuadTree.QuadTreeIterator iterator;

    private Index(QuadTree quadTree, ArrayList<Entry> zoneIds) {
        int WGS84_WKID = 4326;
        this.quadTree = quadTree;
        this.zoneIds = zoneIds;
        this.spatialReference = SpatialReference.create(WGS84_WKID);
        this.iterator = this.quadTree.getIterator();
    }

    List<ZoneId> getKnownZoneIds() {
        return zoneIds.stream().map(e -> e.zoneId).collect(Collectors.toList());
    }

    Optional<ZoneId> query(double latitude, double longitude) {
        Point point = new Point(longitude, latitude);
        iterator.resetIterator(point, 0);
        for (int i = iterator.next(); i >= 0; i = iterator.next()) {
            int element = quadTree.getElement(i);
            Entry entry = zoneIds.get(element);
            if (GeometryEngine.contains(entry.geometry, point, spatialReference)) {
                return Optional.of(entry.zoneId);
            }
        }
        return Optional.empty();
    }

    static Index build(FeatureCollection featureCollection) throws JsonProcessingException {
        QuadTree quadTree = new QuadTree(new Envelope2D(-180, -90, 180, 90), 8);
        Envelope2D env = new Envelope2D();
        ArrayList<Entry> zoneIds = new ArrayList<>(featureCollection.getFeatures().size());
        ObjectMapper om = new ObjectMapper();
        int index = -1;
        for (org.geojson.Feature f : featureCollection.getFeatures()) {
            index += 1;
            try {
                String polygonStr = om.writeValueAsString(f.getGeometry());
                MapGeometry mapGeom = OperatorImportFromGeoJson.local().execute(GeoJsonImportFlags.geoJsonImportDefaults, Geometry.Type.Polygon, polygonStr, null);
                mapGeom.getGeometry().queryEnvelope2D(env);
                quadTree.insert(index, env);
                zoneIds.add(index, new Entry(ZoneId.of(f.getProperties().get("tzid").toString()), (Polygon) mapGeom.getGeometry()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw e;
            }
        }
        return new Index(quadTree, zoneIds);
    }
}
