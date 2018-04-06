package net.iakovlev.timeshape;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.geojson.FeatureCollection;

import java.io.IOException;
import java.io.InputStream;

final class DataLoader {
    static FeatureCollection readData(InputStream src) throws IOException {
        ObjectMapper om = new ObjectMapper();
        return om.readValue(src, FeatureCollection.class);
    }
}
