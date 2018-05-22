package net.iakovlev.timeshape;

import net.iakovlev.timeshape.proto.Geojson;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.tukaani.xz.SeekableFileInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

/**
 * Class {@link TimeZoneEngine} is used to lookup the instance of
 * {@link java.time.ZoneId} based on latitude and longitude.
 */
public final class TimeZoneEngine {

    private final Index index;

    private TimeZoneEngine(Index index) {
        this.index = index;
    }

    /**
     * Queries the {@link TimeZoneEngine} for a {@link java.time.ZoneId}
     * based on geo coordinates.
     * @param latitude latitude part of query
     * @param longitude longitude part of query
     * @return {@link Optional<ZoneId>#of(ZoneId)} if input corresponds
     * to some zone, or {@link Optional#empty()} otherwise.
     */
    public Optional<ZoneId> query(double latitude, double longitude) {
        return index.query(latitude, longitude);
    }

    /**
     * Returns all the time zones that can be looked up.
     * @return all the time zones that can be looked up.
     */
    public List<ZoneId> getKnownZoneIds() {
        return index.getKnownZoneIds();
    }

    /**
     * Creates a new instance of {@link TimeZoneEngine} and initializes it.
     * This is a blocking long running operation.
     * @return an initialized instance of {@link TimeZoneEngine}
     */
    public static TimeZoneEngine initialize() {
        try (SevenZFile f = new SevenZFile(new File(TimeZoneEngine.class.getResource("/output.pb.7z").getFile()))) {
            SevenZArchiveEntry nextEntry = f.getNextEntry();
            if (nextEntry != null) {
                byte[] e = new byte[(int)nextEntry.getSize()];
                f.read(e);
                return new TimeZoneEngine(Index.build(Geojson.FeatureCollection.parseFrom(e)));
            } else {
                throw new RuntimeException("Data entry is not found in 7z file");
            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load the data", e);
        }
    }
}
