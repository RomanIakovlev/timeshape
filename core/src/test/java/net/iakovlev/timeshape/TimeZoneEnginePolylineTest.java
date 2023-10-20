package net.iakovlev.timeshape;

import net.iakovlev.timeshape.SameZoneSpan;
import net.iakovlev.timeshape.TimeZoneEngine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static junit.framework.TestCase.assertEquals;

@RunWith(JUnit4.class)
public class TimeZoneEnginePolylineTest {
    private static TimeZoneEngine engine = TimeZoneEngine.initialize(true);

    @Test
    public void test2points() {
        assertEquals(engine.queryPolyline(new double[]{52.52, 13.40, 56.52, 16.40}),
                Arrays.asList(
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Europe/Berlin"))), 1),
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Europe/Stockholm"))), 3)));
    }

    @Test
    public void test3points() {
        assertEquals(engine.queryPolyline(new double[]{52.52, 13.40, 54.52, 15.40, 56.52, 16.40}),
                Arrays.asList(
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Europe/Berlin"))), 1),
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Etc/GMT-1"))), 3),
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Europe/Stockholm"))), 5)));
    }

    @Test
    public void testSpanWithMultiPoints() {
        assertEquals(engine.queryPolyline(new double[]{52.52, 13.40, 52.53, 13.30, 54.52, 15.40, 56.52, 16.40, 56.52, 16.40}),
                Arrays.asList(
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Europe/Berlin"))), 3),
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Etc/GMT-1"))), 5),
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Europe/Stockholm"))), 9)));
    }

    @Test
    public void testNonMatchingPoints() {
        assertEquals(
                Arrays.asList(
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Etc/GMT-12"))), 5),
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Europe/Berlin"))), 9),
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Etc/GMT-1"))), 11),
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Europe/Stockholm"))), 13)),
                engine.queryPolyline(new double[]{80, 180, 80, 180, 80, 180, 52.52, 13.40, 52.53, 13.30, 54.52, 15.40, 56.52, 16.40}));
    }

    @Test
    public void testNonMatchingLastPoints() {
        assertEquals(
                Arrays.asList(
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Europe/Vilnius"))), 3),
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Europe/Minsk"))), 5),
                        new SameZoneSpan(new HashSet<>(Collections.singletonList(ZoneId.of("Etc/GMT-12"))), 9)
                ), 
                engine.queryPolyline(new double[]{54.89, 23.91, 55.13, 25.57, 54.29, 28.32, 80, 180, 80, 180}));
    }
}