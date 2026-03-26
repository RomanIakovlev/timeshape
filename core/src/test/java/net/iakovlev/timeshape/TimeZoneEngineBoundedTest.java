package net.iakovlev.timeshape;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;

@RunWith(JUnit4.class)
public class TimeZoneEngineBoundedTest {
    private static final TimeZoneEngine engine = TimeZoneEngine.initialize(47.0599, 4.8237, 55.3300, 15.2486, true);

    @Test
    public void testSomeZones() {
        assertEquals(Optional.of(ZoneId.of("Europe/Berlin")), engine.query(52.52, 13.40));
    }

    @Test
    public void testWorld() {
        List<ZoneId> knownZoneIds = engine.getKnownZoneIds();
        assertEquals(39, knownZoneIds.size());
    }
}
