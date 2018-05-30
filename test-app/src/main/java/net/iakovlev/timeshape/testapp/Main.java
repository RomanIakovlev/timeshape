package net.iakovlev.timeshape.testapp;

import net.iakovlev.timeshape.TimeZoneEngine;

public class Main {
    static public void main(String[] args) {
        TimeZoneEngine engine = TimeZoneEngine.initialize(4.8237, 47.0599, 15.2486, 55.3300);
        System.out.println(engine.query(52.52, 13.40));
    }
}
