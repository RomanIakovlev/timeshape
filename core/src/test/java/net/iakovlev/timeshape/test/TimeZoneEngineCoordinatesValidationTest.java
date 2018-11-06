package net.iakovlev.timeshape.test;

import net.iakovlev.timeshape.TimeZoneEngine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.TestCase.assertEquals;

@RunWith(JUnit4.class)
public class TimeZoneEngineCoordinatesValidationTest {
    @Test
    public void testMinimumLatitudeOutOfBounds() {
        try {
            TimeZoneEngine.initialize(-100, 0, 0, 0);
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "minimum latitude -100.000000 is out of range: must be -90 <= latitude <= 90;");
        }
    }
    @Test
    public void testMinimumLongitudeOutOfBounds() {
        try {
            TimeZoneEngine.initialize(0, -190, 0, 0);
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "minimum longitude -190.000000 is out of range: must be -180 <= longitude <= 180;");
        }
    }
    @Test
    public void testMaximumLatitudeOutOfBounds() {
        try {
            TimeZoneEngine.initialize(0, 0, 100, 0);
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "maximum latitude 100.000000 is out of range: must be -90 <= latitude <= 90;");
        }
    }
    @Test
    public void testMaximumLongitudeOutOfBounds() {
        try {
            TimeZoneEngine.initialize(0, 0, 0, 190);
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "maximum longitude 190.000000 is out of range: must be -180 <= longitude <= 180;");
        }
    }
    @Test
    public void testInconsistentLatitudes() {
        try {
            TimeZoneEngine.initialize(0, 0, -1, 0);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "maximum latitude -1.000000 is less than minimum latitude 0.000000;");
        }
    }
    @Test
    public void testInconsistentLongitudes() {
        try {
            TimeZoneEngine.initialize(0, 0, 0, -1);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "maximum longitude -1.000000 is less than minimum longitude 0.000000;");
        }
    }
    @Test
    public void testMultipleErrors() {
        try {
            TimeZoneEngine.initialize(0, 0, -1, -1);
        } catch (IllegalArgumentException ex) {
            assertEquals(ex.getMessage(), "maximum latitude -1.000000 is less than minimum latitude 0.000000; maximum longitude -1.000000 is less than minimum longitude 0.000000;");
        }
    }

}
