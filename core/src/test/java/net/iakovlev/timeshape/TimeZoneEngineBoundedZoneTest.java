package net.iakovlev.timeshape;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;

@RunWith(JUnit4.class)
public class TimeZoneEngineBoundedZoneTest {

    @Test
    public void testSomeZones() {
        Set<ZoneId> timeZones = new HashSet<> ();
        timeZones.add (ZoneId.of("Europe/Berlin"));
        TimeZoneEngine engine = TimeZoneEngine.initialize (timeZones, true);
        assertEquals(Optional.of(ZoneId.of("Europe/Berlin")), engine.query(52.52, 13.40));
        
        timeZones.clear ();
        timeZones.add(ZoneId.of("America/Detroit"));
        engine = TimeZoneEngine.initialize (timeZones, true);
        assertEquals(Optional.empty (), engine.query(52.52, 13.40));

        timeZones.add(ZoneId.of("Europe/Berlin"));
        engine = TimeZoneEngine.initialize (timeZones, true);
        assertEquals(Optional.of(ZoneId.of("Europe/Berlin")), engine.query(52.52, 13.40));
    }
}
