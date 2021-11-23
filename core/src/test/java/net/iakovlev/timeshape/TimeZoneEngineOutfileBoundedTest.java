package net.iakovlev.timeshape;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;

@RunWith(JUnit4.class)
public class TimeZoneEngineOutfileBoundedTest {
    private static TimeZoneEngine engine = null;

    @BeforeClass
    public static void initEngine() {
        try (InputStream resourceAsStream = new FileInputStream("./core/target/resource_managed/main/data.tar.zstd");
             TarArchiveInputStream f = new TarArchiveInputStream(new ZstdCompressorInputStream(resourceAsStream))) {
            engine = TimeZoneEngine.initialize(f);
        } catch (NullPointerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSomeZones() {
        assertEquals(Optional.of(ZoneId.of("Europe/Berlin")), engine.query(52.52, 13.40));
    }

}
