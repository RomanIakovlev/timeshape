package net.iakovlev.timeshape.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.iakovlev.timeshape.proto.Geojson;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.geojson.FeatureCollection;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipInputStream;

public class Main {

    /**
     * Provides access to source GeoJSON data. If argument is a path to the file on a local computer,
     * it's opened for reading. If it's not a existing file, the argument is interpreted as a version number
     * to download from Github
     * @param argument either a path on a local computer or a version number to download from Github
     * @return Stream with contents (zip-compressed) of source GeoJSON data
     * @throws IOException if incorrect local path is provided or there was a problem with downloading the file
     */
    private static InputStream createInputStream(String argument) throws IOException {
        if (Files.exists(Paths.get(argument))) {
            return new FileInputStream(argument);
        } else {
            String url = "https://github.com/evansiroky/timezone-boundary-builder/releases/download/" + argument + "/timezones-with-oceans.geojson.zip";
            return new URL(url).openStream();
        }
    }

    /**
     * Reads the zip-compressed input stream of GeoJSON, converts its data to proto and writes
     * each GeoJSON feature as a separate entry into a tar file, which is compressed with ZStandard
     * algorithm at the end
     * @param argument
     * @param outputPath
     */
    private static void writeTarZStdProto(String argument, String outputPath, String outputFile) {
        final Path path = Paths.get(outputPath, outputFile);
        if (Files.exists(path)) {
            System.err.println("File already exists, exiting without doing anything");
            return;
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(createInputStream(argument))) {
            zipInputStream.getNextEntry();
            Geojson.FeatureCollection featureCollection = Builder.buildProto(new ObjectMapper().readValue(zipInputStream, FeatureCollection.class));
            Files.createDirectories(Paths.get(outputPath));
            try (TarArchiveOutputStream out =
                         new TarArchiveOutputStream(
                                 new ZstdCompressorOutputStream(Files.newOutputStream(path), 22))) {
                for (Geojson.Feature feature : featureCollection.getFeaturesList()) {
                    TarArchiveEntry entry = new TarArchiveEntry(feature.getProperties(0).getValueString());
                    byte[] bytes = feature.toByteArray();
                    entry.setSize(bytes.length);
                    out.putArchiveEntry(entry);
                    out.write(bytes);
                    out.closeArchiveEntry();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't prepare the data", e);
        }
    }

    public static void main(String[] args) {
        String argument = args[0];
        String outputPath = args[1];
        String outputFile = args[2];
        writeTarZStdProto(argument, outputPath, outputFile);
    }
}
