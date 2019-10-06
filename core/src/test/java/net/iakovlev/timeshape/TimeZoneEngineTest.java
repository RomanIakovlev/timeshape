package net.iakovlev.timeshape;

import net.iakovlev.timeshape.TimeZoneEngine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@RunWith(JUnit4.class)
public class TimeZoneEngineTest {
    private static TimeZoneEngine engine = TimeZoneEngine.initialize();

    @Test
    public void testSomeZones() {
        assertEquals(Collections.singletonList(ZoneId.of("Europe/Berlin")), engine.query(52.52, 13.40));
        assertEquals(Collections.singletonList(ZoneId.of("Asia/Tomsk")), engine.query(56.49771, 84.97437));
        assertEquals(Collections.singletonList(ZoneId.of("America/Santiago")), engine.query(-33.459229, -70.645348));
        assertEquals(Collections.singletonList(ZoneId.of("Asia/Krasnoyarsk")), engine.query(56.01839, 92.86717));
        assertEquals(Collections.singletonList(ZoneId.of("Africa/Abidjan")), engine.query(5.345317, -4.024429));
        assertEquals(Collections.singletonList(ZoneId.of("America/New_York")), engine.query(40.785091, -73.968285));
        assertEquals(Collections.singletonList(ZoneId.of("Australia/Sydney")), engine.query(-33.865143, 151.215256));
        assertEquals(Collections.singletonList(ZoneId.of("Etc/GMT+1")), engine.query(38.00, -15.2814));
    }

    @Test
    public void testBoundariesAndMultiPolygons() {
        assertEquals(Collections.singletonList(ZoneId.of("Europe/Amsterdam")), engine.query(51.4457, 4.9248));
        assertEquals(Collections.singletonList(ZoneId.of("Europe/Brussels")), engine.query(51.4457, 4.9250));
        assertEquals(Collections.singletonList(ZoneId.of("Europe/Brussels")), engine.query(51.4437,4.9186));
        assertEquals(Collections.singletonList(ZoneId.of("Europe/Amsterdam")), engine.query(51.4438,4.9181));
    }

    @Test
    public void testMultipleTimeZonesInResponse() {
        assertEquals(List.of(ZoneId.of("Asia/Shanghai"), ZoneId.of("Asia/Urumqi")), engine.query(39.601, 79.201));
    }

    @Test
    public void testWorld() {
        Set<String> engineZoneIds = engine.getKnownZoneIds().stream().map(ZoneId::getId).collect(Collectors.toSet());
        assertTrue(java.time.ZoneId.getAvailableZoneIds().containsAll(engineZoneIds));
    }
}
