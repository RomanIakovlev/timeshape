package net.iakovlev.timeshape.test;

import net.iakovlev.timeshape.TimeZoneEngine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@RunWith(JUnit4.class)
public class TimeZoneEngineTest {
    private static TimeZoneEngine engine = TimeZoneEngine.initialize();

    @Test
    public void testSomeZones() {
        assertEquals(engine.query(52.52, 13.40), Optional.of(ZoneId.of("Europe/Berlin")));
        assertEquals(engine.query(56.49771, 84.97437), Optional.of(ZoneId.of("Asia/Tomsk")));
        assertEquals(engine.query(-33.459229, -70.645348), Optional.of(ZoneId.of("America/Santiago")));
        assertEquals(engine.query(56.01839, 92.86717), Optional.of(ZoneId.of("Asia/Krasnoyarsk")));
        assertEquals(engine.query(5.345317, -4.024429), Optional.of(ZoneId.of("Africa/Abidjan")));
        assertEquals(engine.query(40.785091, -73.968285), Optional.of(ZoneId.of("America/New_York")));
        assertEquals(engine.query(-33.865143, 151.215256), Optional.of(ZoneId.of("Australia/Sydney")));
    }

    @Test
    public void testBoundariesAndMultiPolygons() {
        assertEquals(engine.query(51.4457, 4.9248), Optional.of(ZoneId.of("Europe/Amsterdam")));
        assertEquals(engine.query(51.4457, 4.9250), Optional.of(ZoneId.of("Europe/Brussels")));
        assertEquals(engine.query(51.4437,4.9186), Optional.of(ZoneId.of("Europe/Brussels")));
        assertEquals(engine.query(51.4438,4.9181), Optional.of(ZoneId.of("Europe/Amsterdam")));
    }

    @Test
    public void testWorld() {
        Set<String> engineZoneIds = engine.getKnownZoneIds().stream().map(ZoneId::getId).collect(Collectors.toSet());
        assertTrue(java.time.ZoneId.getAvailableZoneIds().containsAll(engineZoneIds));
    }
}
