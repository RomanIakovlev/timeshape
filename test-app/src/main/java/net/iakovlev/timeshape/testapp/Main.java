package net.iakovlev.timeshape.testapp;

import net.iakovlev.timeshape.SameZoneSegment;
import net.iakovlev.timeshape.SameZoneSpan;
import net.iakovlev.timeshape.TimeZoneEngine;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.vm.VM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.System.out;

public class Main {
    static public void main(String[] args) {
        long start = System.currentTimeMillis();
        TimeZoneEngine engine = TimeZoneEngine.initialize();
        long total = System.currentTimeMillis() - start;
        out.println("initialization took " + total + " milliseconds");
        out.println(engine.query(52.52, 13.40));

        double latStart = 52.52;
        double lonStart = 13.40;
        double latEnd = 56.52;
        double lonEnd = 16.40;

        int steps = 200;

        double latStep = Math.abs(latEnd - latStart) / steps;
        double lonStep = Math.abs(lonEnd - lonStart) / steps;

        ArrayList<Double> pointsList = new ArrayList<>();

        for (int i = 0; i < steps; i++) {
            pointsList.add(latStart + latStep * i);
            pointsList.add(lonStart + lonStep * i);
        }

        double[] points = pointsList.stream().mapToDouble(Double::doubleValue).toArray();

        ArrayList<SameZoneSegment> query = engine.query(points);
        ArrayList<SameZoneSegment> query1 = engine.query1(points);
        ArrayList<SameZoneSpan> query2 = engine.query2(points);

        query.forEach(p -> out.println(p.toString()));
        out.println("\n\n\n\n\n\n\n\n");
        query1.forEach(p -> out.println(p.toString()));
        out.println("\n\n\n\n\n\n\n\n");
        query2.forEach(p -> out.println(p.toString()));
        out.println("Results are equal: " + query.equals(query1));
        /*out.println(VM.current().details());
        out.println(GraphLayout.parseInstance(engine).toFootprint());*/
    }
}
