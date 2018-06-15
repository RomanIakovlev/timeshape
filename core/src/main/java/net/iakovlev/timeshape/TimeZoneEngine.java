package net.iakovlev.timeshape;

import com.esri.core.geometry.Envelope2D;
import net.iakovlev.timeshape.proto.Geojson;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
     *
     * @param latitude  latitude part of query
     * @param longitude longitude part of query
     * @return {@link Optional<ZoneId>#of(ZoneId)} if input corresponds
     * to some zone, or {@link Optional#empty()} otherwise.
     */
    public Optional<ZoneId> query(double latitude, double longitude) {
        return index.query(latitude, longitude);
    }

    /**
     * Returns all the time zones that can be looked up.
     *
     * @return all the time zones that can be looked up.
     */
    public List<ZoneId> getKnownZoneIds() {
        return index.getKnownZoneIds();
    }

    /**
     * Creates a new instance of {@link TimeZoneEngine} and initializes it.
     * This is a blocking long running operation.
     *
     * @return an initialized instance of {@link TimeZoneEngine}
     */
    public static TimeZoneEngine initialize() {
        return initialize(-90, -180, 90, 180);
    }

    /**
     * Creates a new instance of {@link TimeZoneEngine} and initializes it.
     * This is a blocking long running operation.
     *
     * @return an initialized instance of {@link TimeZoneEngine}
     */
    public static TimeZoneEngine initialize(double minlat, double minlon, double maxlat, double maxlon) {
        try (InputStream resourceAsStream = TimeZoneEngine.class.getResourceAsStream("/output.pb.7z");
             SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(IOUtils.toByteArray(resourceAsStream));
             SevenZFile f = new SevenZFile(channel)) {
            Stream<Geojson.Feature> featureStream = StreamSupport.stream(f.getEntries().spliterator(), false).map(n -> {
                try {
                    SevenZArchiveEntry nextEntry = f.getNextEntry();
                    if (nextEntry != null) {
                        byte[] e = new byte[(int) nextEntry.getSize()];
                        f.read(e);
                        return Geojson.Feature.parseFrom(e);
                    } else {
                        throw new RuntimeException("Data entry is not found in 7z file");
                    }
                } catch (NullPointerException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            Envelope2D boundaries = new Envelope2D(minlon, minlat, maxlon, maxlat);
            return new TimeZoneEngine(
                    Index.build(
                            featureStream,
                            (int) f.getEntries().spliterator().getExactSizeIfKnown(),
                            boundaries));
        } catch (NullPointerException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
