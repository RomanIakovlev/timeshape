package net.iakovlev.timeshape;

import net.iakovlev.timeshape.proto.Geojson;
import org.geojson.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Builder {
    private static Geojson.LineString geoJsonCoordinatesToProtoLineString(List<LngLatAlt> geoJsonCoordinates) {
        return Geojson.LineString.newBuilder().addAllCoordinates(geoJsonCoordinates.stream().map(lngLatAlt -> {
            Geojson.Position.Builder builder = Geojson.Position.newBuilder();
            builder.setLat((float) lngLatAlt.getLatitude());
            builder.setLon((float) lngLatAlt.getLongitude());
            return builder.build();
        }).collect(Collectors.toList())).build();
    }

    private static Geojson.Polygon geoJsonPolygonToProtoPolygon(List<List<LngLatAlt>> poly) {
        List<LngLatAlt> exteriorRing = poly.get(0);
        Geojson.LineString protoExteriorRing = geoJsonCoordinatesToProtoLineString(exteriorRing);
        Geojson.Polygon.Builder polygonBuilder = Geojson.Polygon.newBuilder();
        polygonBuilder.addCoordinates(protoExteriorRing);
        poly.subList(1, poly.size()).forEach(list -> polygonBuilder.addCoordinates(geoJsonCoordinatesToProtoLineString(list)));
        return polygonBuilder.build();
    }

    private static Geojson.MultiPolygon geoJsonMultiPolygonToProtoMultiPolygon(MultiPolygon multiPolygon) {
        Geojson.MultiPolygon.Builder builder = Geojson.MultiPolygon.newBuilder();
        multiPolygon.getCoordinates().forEach(polys -> builder.addCoordinates(geoJsonPolygonToProtoPolygon(polys)));
        return builder.build();
    }

    static Geojson.FeatureCollection buildProto(FeatureCollection featureCollection) throws IOException {
        Geojson.FeatureCollection.Builder featureCollectionBuilder = Geojson.FeatureCollection.newBuilder();
        for (org.geojson.Feature f : featureCollection.getFeatures()) {
            String tzid = f.getProperties().get("tzid").toString();
            Geojson.Feature.Builder featureBuilder = Geojson.Feature.newBuilder();
            featureBuilder.addProperties(Geojson.Property.newBuilder().setKey("tzid").setValueString(tzid));
            GeoJsonObject geoJsonObject = f.getGeometry();
            if (geoJsonObject instanceof Polygon) {
                Polygon poly = (Polygon) geoJsonObject;
                Stream.Builder<List<LngLatAlt>> s = Stream.builder();
                s.add(poly.getExteriorRing());
                poly.getInteriorRings().forEach(s::add);
                featureCollectionBuilder.addFeatures(
                        featureBuilder.setGeometry(
                                Geojson.Geometry.newBuilder().setPolygon(
                                        geoJsonPolygonToProtoPolygon(s.build().collect(Collectors.toList())))));
            } else if (geoJsonObject instanceof MultiPolygon) {
                MultiPolygon multiPolygon = (MultiPolygon) geoJsonObject;
                featureCollectionBuilder.addFeatures(featureBuilder.setGeometry(Geojson.Geometry.newBuilder().setMultiPolygon(
                        geoJsonMultiPolygonToProtoMultiPolygon(multiPolygon)
                )));
            } else {
                throw new RuntimeException("not implemented");
            }
        }
        return featureCollectionBuilder.build();
    }
}
