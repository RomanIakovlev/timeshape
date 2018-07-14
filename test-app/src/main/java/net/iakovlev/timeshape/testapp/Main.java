package net.iakovlev.timeshape.testapp;

import net.iakovlev.timeshape.TimeZoneEngine;
import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.vm.VM;

import static java.lang.System.out;

public class Main {
    static public void main(String[] args) {
        long start = System.currentTimeMillis();
        TimeZoneEngine engine = TimeZoneEngine.initialize();
        long total = System.currentTimeMillis() - start;
        out.println("initialization took " + total + " milliseconds");
        out.println(engine.query(52.52, 13.40));
        out.println(VM.current().details());
        out.println(GraphLayout.parseInstance(engine).toFootprint());
    }
}
