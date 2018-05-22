package net.iakovlev.timeshape;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.iakovlev.timeshape.proto.Geojson;
import org.geojson.FeatureCollection;

import java.io.IOException;
import java.io.InputStream;

final class DataReader {
    static FeatureCollection readGeoJson(InputStream src) throws IOException {
        ObjectMapper om = new ObjectMapper();
        return om.readValue(src, FeatureCollection.class);
    }

    static Geojson.FeatureCollection readProto(InputStream src) throws IOException {
        return Geojson.FeatureCollection.parseFrom(src);
    }
}
