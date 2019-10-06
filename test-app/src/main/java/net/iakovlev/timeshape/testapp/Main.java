package net.iakovlev.timeshape.testapp;

import net.iakovlev.timeshape.TimeZoneEngine;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.vm.VM;

import java.time.ZoneId;
import java.util.List;

import static java.lang.System.out;

public class Main {
    static public void main(String[] args) {
        long start = System.currentTimeMillis();
        TimeZoneEngine engine = TimeZoneEngine.initialize(true);
        long total = System.currentTimeMillis() - start;
        out.println("initialization took " + total + " milliseconds");
        out.println(engine.query(52.52, 13.40));
        out.println(VM.current().details());
        out.println(GraphLayout.parseInstance(engine).toFootprint());
        double lonMin = -180.0;
        double lonMax = 180.0;
        double latMin = -90.0;
        double latMax = 90.0;
        String prev = "";
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                double latitude = (latMax - latMin) / 100 * i;
                double longitude = (lonMax - lonMin) / 100 * i;
                final List<ZoneId> result = engine.query(latitude, longitude);
                if (result.size() > 1) {
                    String report = "Found multiple zones (" + result + ") for " + latitude + ", " + longitude;
                    if (!report.equals(prev)) {
                        out.println("Found multiple zones (" + result + ") for " + latitude + ", " + longitude);
                    }
                    prev = report;
                }
            }
        }
    }
}
