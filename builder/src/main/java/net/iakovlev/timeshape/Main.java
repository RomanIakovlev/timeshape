package net.iakovlev.timeshape;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.iakovlev.timeshape.proto.Geojson;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.geojson.FeatureCollection;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

public class Main {

    static void writeSevenZProto(String inputPath, String outputPath) {
        try (InputStream stream = new FileInputStream(inputPath)) {
            ZipInputStream zipInputStream = new ZipInputStream(stream);
            zipInputStream.getNextEntry();
            Geojson.FeatureCollection featureCollection = Builder.buildProto(new ObjectMapper().readValue(zipInputStream, FeatureCollection.class));
            File outputFile = new File(outputPath);
            try (SevenZOutputFile out = new SevenZOutputFile(outputFile)) {
                for (Geojson.Feature feature : featureCollection.getFeaturesList()) {
                    SevenZArchiveEntry entry = out.createArchiveEntry(outputFile, feature.getProperties(0).getValueString());
                    out.putArchiveEntry(entry);
                    out.write(feature.toByteArray());
                    out.closeArchiveEntry();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't load the data", e);
        }
    }

    public static void main(String[] args) {
        String inputPath = args[0];
        String outputPath = args[1];
        writeSevenZProto(inputPath, outputPath);
    }
}
