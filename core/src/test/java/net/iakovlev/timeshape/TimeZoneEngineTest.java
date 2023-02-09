package net.iakovlev.timeshape;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.time.ZoneId;
import java.util.List;
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
        assertEquals(Optional.of(ZoneId.of("Europe/Berlin")), engine.query(52.52, 13.40));
        assertEquals(Optional.of(ZoneId.of("Asia/Tomsk")), engine.query(56.49771, 84.97437));
        assertEquals(Optional.of(ZoneId.of("America/Santiago")), engine.query(-33.459229, -70.645348));
        assertEquals(Optional.of(ZoneId.of("Asia/Krasnoyarsk")), engine.query(56.01839, 92.86717));
        assertEquals(Optional.of(ZoneId.of("Africa/Abidjan")), engine.query(5.345317, -4.024429));
        assertEquals(Optional.of(ZoneId.of("America/New_York")), engine.query(40.785091, -73.968285));
        assertEquals(Optional.of(ZoneId.of("Australia/Sydney")), engine.query(-33.865143, 151.215256));
        assertEquals(Optional.of(ZoneId.of("Etc/GMT+1")), engine.query(38.00, -15.2814));
        assertEquals(Optional.of(ZoneId.of("Asia/Shanghai")), engine.query(39.601, 79.201));
        assertEquals(Optional.of(ZoneId.of("Asia/Shanghai")), engine.query(27.45, 89.05));
        assertEquals(List.of(ZoneId.of("America/Ciudad_Juarez")), engine.queryAll(31.752, -106.457));
    }

    @Test
    public void testBoundariesAndMultiPolygons() {
        assertEquals(Optional.of(ZoneId.of("Europe/Amsterdam")), engine.query(51.4457, 4.9248));
        assertEquals(Optional.of(ZoneId.of("Europe/Brussels")), engine.query(51.4457, 4.9250));
        assertEquals(Optional.of(ZoneId.of("Europe/Brussels")), engine.query(51.4437,4.9186));
        assertEquals(Optional.of(ZoneId.of("Europe/Amsterdam")), engine.query(51.4438,4.9181));
    }

    @Test
    public void testMultipleTimeZonesInResponse() throws IOException {

        List<ZoneId> expected1 = new java.util.ArrayList<>();
        expected1.add(ZoneId.of("Africa/Juba"));
        expected1.add(ZoneId.of("Africa/Khartoum"));
        assertEquals(expected1, engine.queryAll(9.75, 28.45));

        List<ZoneId> expected2 = new java.util.ArrayList<>();
        expected2.add(ZoneId.of("America/Argentina/Rio_Gallegos"));
        expected2.add(ZoneId.of("America/Punta_Arenas"));
        assertEquals(expected2, engine.queryAll(-49.5, -73.3));

        List<ZoneId> expected3 = new java.util.ArrayList<>();
        expected3.add(ZoneId.of("America/La_Paz"));
        expected3.add(ZoneId.of("America/Porto_Velho"));
        assertEquals(expected3, engine.queryAll(-10.8, -65.35));

        List<ZoneId> expected4 = new java.util.ArrayList<>();
        expected4.add(ZoneId.of("America/Moncton"));
        expected4.add(ZoneId.of("America/New_York"));
        assertEquals(expected4, engine.queryAll(44.5, -67.15));

        List<ZoneId> expected5 = new java.util.ArrayList<>();
        expected5.add(ZoneId.of("Asia/Hebron"));
        expected5.add(ZoneId.of("Asia/Jerusalem"));
        assertEquals(expected5, engine.queryAll(31.95, 35.2));

        List<ZoneId> expected6 = new java.util.ArrayList<>();
        expected6.add(ZoneId.of("Asia/Shanghai"));
        expected6.add(ZoneId.of("Asia/Thimphu"));
        assertEquals(expected6, engine.queryAll(27.45, 89.05));

        List<ZoneId> expected = new java.util.ArrayList<>();
        expected.add(ZoneId.of("Asia/Shanghai"));
        expected.add(ZoneId.of("Asia/Urumqi"));
        assertEquals(expected, engine.queryAll(39.601, 79.201));

    }

    @Test
    public void testWorld() {
        Set<String> engineZoneIds = engine.getKnownZoneIds().stream().map(ZoneId::getId).collect(Collectors.toSet());
        assertTrue(java.time.ZoneId.getAvailableZoneIds().containsAll(engineZoneIds));
    }
}
