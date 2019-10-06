package net.iakovlev.timeshape;

import com.esri.core.geometry.Envelope;
import net.iakovlev.timeshape.proto.Geojson;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Class {@link TimeZoneEngine} is used to lookup the instance of
 * {@link java.time.ZoneId} based on latitude and longitude.
 */
public final class TimeZoneEngine {

    private final Index index;

    private final static double MIN_LAT = -90;
    private final static double MIN_LON = -180;
    private final static double MAX_LAT = 90;
    private final static double MAX_LON = 180;

    private final static Logger log = LoggerFactory.getLogger(TimeZoneEngine.class);

    private TimeZoneEngine(Index index) {
        this.index = index;
    }

    private static void validateCoordinates(double minLat, double minLon, double maxLat, double maxLon) {
        List<String> errors = new ArrayList<>();
        if (minLat < MIN_LAT || minLat > MAX_LAT) {
            errors.add(String.format(Locale.ROOT, "minimum latitude %f is out of range: must be -90 <= latitude <= 90;", minLat));
        }
        if (maxLat < MIN_LAT || maxLat > MAX_LAT) {
            errors.add(String.format(Locale.ROOT, "maximum latitude %f is out of range: must be -90 <= latitude <= 90;", maxLat));
        }
        if (minLon < MIN_LON || minLon > MAX_LON) {
            errors.add(String.format(Locale.ROOT, "minimum longitude %f is out of range: must be -180 <= longitude <= 180;", minLon));
        }
        if (maxLon < MIN_LON || maxLon > MAX_LON) {
            errors.add(String.format(Locale.ROOT, "maximum longitude %f is out of range: must be -180 <= longitude <= 180;", maxLon));
        }
        if (minLat > maxLat) {
            errors.add(String.format(Locale.ROOT, "maximum latitude %f is less than minimum latitude %f;", maxLat, minLat));
        }
        if (minLon > maxLon) {
            errors.add(String.format(Locale.ROOT, "maximum longitude %f is less than minimum longitude %f;", maxLon, minLon));
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", errors));
        }
    }

    private static Spliterator<TarArchiveEntry> makeSpliterator(TarArchiveInputStream f) {
        return new Spliterators.AbstractSpliterator<TarArchiveEntry>(Long.MAX_VALUE, 0) {
            @Override
            public boolean tryAdvance(Consumer<? super TarArchiveEntry> action) {
                try {
                    TarArchiveEntry entry = f.getNextTarEntry();
                    if (entry != null) {
                        action.accept(entry);
                        return true;
                    } else {
                        return false;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Queries the {@link TimeZoneEngine} for a {@link java.time.ZoneId}
     * based on geo coordinates.
     *
     * @param latitude  latitude part of query
     * @param longitude longitude part of query
     * @return {@code Optional<ZoneId>#of(ZoneId)} if input corresponds
     * to some zone, or {@link Optional#empty()} otherwise.
     */
    public List<ZoneId> query(double latitude, double longitude) {
        return index.query(latitude, longitude);
    }

    /**
     * Queries the {@link TimeZoneEngine} for a {@link java.time.ZoneId}
     * based on sequence of geo coordinates
     * @param points array of doubles representing the sequence of geo coordinates
     *               Must have the following shape: <code>{lat_1, lon_1, lat_2, lon_2, ..., lat_N, lon_N}</code>
     * @return Sequence of {@link SameZoneSpan}, where {@link SameZoneSpan#getEndIndex()} represents the last index
     * in the {@param points} array, which belong to the value of {@link SameZoneSpan#getZoneIds()}
     * E.g. for {@param points} == <code>{lat_1, lon_1, lat_2, lon_2, lat_3, lon_3}</code>, that is, a polyline of
     * 3 points: point_1, point_2, and point_3, and presuming point_1 belongs to Etc/GMT+1, point_2 belongs to Etc/GMT+2,
     * and point_3 belongs to Etc/Gmt+3, the result will be:
     * <code>{SameZoneSpan(Etc/Gmt+1, 1), SameZoneSpan(Etc/Gmt+2, 3), SameZoneSpan(Etc/Gmt+3, 5)}</code>
     */
    public List<SameZoneSpan> queryPolyline(double[] points) {
        return index.queryPolyline(points);
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
    public static TimeZoneEngine initialize(boolean accelerateGeometry) {
        return initialize(MIN_LAT, MIN_LON, MAX_LAT, MAX_LON, accelerateGeometry);
    }
    /**
     * Creates a new instance of {@link TimeZoneEngine} and initializes it.
     * This is a blocking long running operation.
     *
     * @return an initialized instance of {@link TimeZoneEngine}
     */
    public static TimeZoneEngine initialize() {
        return initialize(MIN_LAT, MIN_LON, MAX_LAT, MAX_LON, false);
    }

    /**
     * Creates a new instance of {@link TimeZoneEngine} and initializes it.
     * This is a blocking long running operation.
     *
     * @return an initialized instance of {@link TimeZoneEngine}
     */
    public static TimeZoneEngine initialize(double minLat, double minLon, double maxLat, double maxLon, boolean accelerateGeometry) {
        log.info("Initializing with bounding box: {}, {}, {}, {}", minLat, minLon, maxLat, maxLon);
        validateCoordinates(minLat, minLon, maxLat, maxLon);
        try (InputStream resourceAsStream = TimeZoneEngine.class.getResourceAsStream("/data.tar.zstd");
             TarArchiveInputStream f = new TarArchiveInputStream(new ZstdCompressorInputStream(resourceAsStream))) {
            Spliterator<TarArchiveEntry> tarArchiveEntrySpliterator = makeSpliterator(f);
            Stream<Geojson.Feature> featureStream = StreamSupport.stream(tarArchiveEntrySpliterator, false).map(n -> {
                try {
                    if (n != null) {
                        log.debug("Processing archive entry {}", n.getName());
                        byte[] e = new byte[(int) n.getSize()];
                        f.read(e);
                        return Geojson.Feature.parseFrom(e);
                    } else {
                        throw new RuntimeException("Data entry is not found in file");
                    }
                } catch (NullPointerException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            int numberOfTimezones = 449; // can't get number of entries from tar, need to set manually
            Envelope boundaries = new Envelope(minLon, minLat, maxLon, maxLat);
            return new TimeZoneEngine(
                    Index.build(
                            featureStream,
                            numberOfTimezones,
                            boundaries,
                            accelerateGeometry));
        } catch (NullPointerException | IOException e) {
            log.error("Unable to read resource file", e);
            throw new RuntimeException(e);
        }
    }
}
