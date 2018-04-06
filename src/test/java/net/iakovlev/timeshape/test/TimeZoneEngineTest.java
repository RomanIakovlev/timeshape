package net.iakovlev.timeshape.test;

import static org.junit.jupiter.api.Assertions.*;

import net.iakovlev.timeshape.TimeZoneEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class TimeZoneEngineTest {
    TimeZoneEngine engine = TimeZoneEngine.initialize();

    @Test
    @DisplayName("Test some zones given their coordinates in different continents")
    void testSomeZones() {
        assertEquals(engine.query(52.52, 13.40), Optional.of(ZoneId.of("Europe/Berlin")));
        assertEquals(engine.query(56.49771, 84.97437), Optional.of(ZoneId.of("Asia/Tomsk")));
        assertEquals(engine.query(5.345317, -4.024429), Optional.of(ZoneId.of("Africa/Abidjan")));
        assertEquals(engine.query(40.785091, -73.968285), Optional.of(ZoneId.of("America/New_York")));
        assertEquals(engine.query(-33.865143, 151.215256), Optional.of(ZoneId.of("Australia/Sydney")));
    }

    @Test
    @DisplayName("Ensure engine contains only zone ids known by runtime")
    void testWorld() {
        Set<String> engineZoneIds = engine.getKnownZoneIds().stream().map(ZoneId::getId).collect(Collectors.toSet());
        assertTrue(java.time.ZoneId.getAvailableZoneIds().containsAll(engineZoneIds));
    }
}
