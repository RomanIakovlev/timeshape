/**
 * Created by roman on 04.04.2018 15:54.
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.esri.core.geometry.*;

public class Main {


    static class Entry {
        ZoneId zoneId;
        Polygon geometry;

        public Entry(ZoneId zoneId, Polygon geometry) {
            this.zoneId = zoneId;
            this.geometry = geometry;
        }
    }

    public static int WGS84_WKID = 4326;

    public static void main(String[] args) throws IOException {
        ObjectMapper om = new ObjectMapper();
        org.geojson.FeatureCollection fc = om.readValue(new File("C:\\Users\\roman\\Downloads\\timezones.geojson\\dist\\combined.json"), org.geojson.FeatureCollection.class);
        /*Map<Polygon, ZoneId> tzid = fc.getFeatures().stream().collect(Collectors.toMap(f -> {
            org.geojson.GeoJsonObject o = f.getGeometry();
            Polygon p = new Polygon();
            if (o instanceof org.geojson.MultiPolygon) {
                org.geojson.MultiPolygon m = (org.geojson.MultiPolygon) o;
                List<List<List<org.geojson.LngLatAlt>>> coordinates = m.getCoordinates();
                for (List<List<org.geojson.LngLatAlt>> l : coordinates) {

                }
            } else if (o instanceof org.geojson.Polygon) {
                org.geojson.Polygon poly = (org.geojson.Polygon)o;
                List<org.geojson.LngLatAlt> exteriorRing = poly.getExteriorRing();
                if (poly.getInteriorRings().size() > 0) {
                    System.out.println("got polygon with interior rings");
                } else {
                    System.out.println("got polygon without interior rings");
                }
            }
            return p;
        }, f -> ZoneId.of(f.getProperties().get("tzid").toString())));*/
        QuadTree qt = new QuadTree(new Envelope2D(-180, -90, 180, 90), 15);
        int index = -1;
        Envelope2D env = new Envelope2D();
        ArrayList<Entry> zoneIds = new ArrayList<>(fc.getFeatures().size());
        for (org.geojson.Feature f : fc.getFeatures()) {
            index += 1;
            try {
                String polygonStr = om.writeValueAsString(f.getGeometry());
                MapGeometry mapGeom = OperatorImportFromGeoJson.local().execute(GeoJsonImportFlags.geoJsonImportDefaults, Geometry.Type.Polygon, polygonStr, null);
                mapGeom.getGeometry().queryEnvelope2D(env);
                qt.insert(index, env);
                zoneIds.add(index, new Entry(ZoneId.of(f.getProperties().get("tzid").toString()), (Polygon) mapGeom.getGeometry()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        SpatialReference spatialReference = SpatialReference.create(WGS84_WKID);

        Point pt = new Point(25.549, 66.089);
        QuadTree.QuadTreeIterator iter = qt.getIterator(pt, 0);
        int i = iter.next();
        while (i >= 0) {
            int element = qt.getElement(i);
            Entry entry = zoneIds.get(element);
            if (GeometryEngine.contains(entry.geometry, pt, spatialReference)) {
                System.out.println(entry.zoneId);
                break;
            }
            i = iter.next();
            System.out.println("iteration step " + i);
        }
        /*
        fc.getFeatures().stream().forEach(f -> {
            if (f.getGeometry() instanceof org.geojson.Polygon) {
                org.geojson.Polygon poly = (org.geojson.Polygon)f.getGeometry();
                if (poly.getInteriorRings().size() > 1) {
                    System.out.println("got polygon with holes for " + f.getProperties().get("tzid").toString());
                }
            }
            if (f.getGeometry() instanceof org.geojson.MultiPolygon) {
                org.geojson.MultiPolygon poly = (org.geojson.MultiPolygon)f.getGeometry();
                if (poly.getCoordinates().size() > 0) {
                    System.out.println("got multi polygon for " + f.getProperties().get("tzid").toString());
                }
            }
        });*/
        //tzid.entrySet().stream().forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));

    }
}
