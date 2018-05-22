package net.iakovlev.timeshape;

import net.iakovlev.timeshape.proto.Geojson;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

public class Main {
    public static void main(String[] args) {
        String inputPath = args[0];
        String outputPath = args[1];
        try (InputStream stream = new FileInputStream(inputPath)) {
            ZipInputStream zipInputStream = new ZipInputStream(stream);
            zipInputStream.getNextEntry();
            Geojson.FeatureCollection featureCollection = Builder.buildProto(DataReader.readGeoJson(zipInputStream));
            File outputFile = new File(outputPath);
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            try (SevenZOutputFile out = new SevenZOutputFile(outputFile)) {
                SevenZArchiveEntry entry = out.createArchiveEntry(outputFile, "timeshape.pb");
                out.putArchiveEntry(entry);
                out.write(featureCollection.toByteArray());
                out.closeArchiveEntry();
            }
            fileOutputStream.close();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't load the data", e);
        }
    }
}
