package net.iakovlev.timeshape;

import net.iakovlev.timeshape.TimeZoneEngine;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;

public class AcceleratedGeometryBenchmark {
    @State(Scope.Benchmark)
    public static class BenchmarkState {

        float[] cityCoordinates = {
                35.6762f, 139.6503f, // Tokyo, Japan
                31.2304f, 121.4737f, // Shanghai, China
                23.1291f, 113.2644f, // Guangzhou, China
                28.7041f, 77.1025f, // Delhi, India
                19.4326f, -99.1332f, // Mexico City, Mexico
                31.2304f, 121.4737f, // Sao Paulo, Brazil
                34.6937f, 135.5023f, // Osaka, Japan
                13.7563f, 100.5018f, // Bangkok, Thailand
                39.9042f, 116.4074f, // Beijing, China
                19.0760f, 72.8777f, // Mumbai, India
                40.7128f, -74.0060f, // New York City, USA
                22.5726f, 88.3639f, // Kolkata, India
                14.5995f, 120.9842f, // Manila, Philippines
                23.8103f, 90.4125f, // Dhaka, Bangladesh
                3.1390f, 101.6869f, // Kuala Lumpur, Malaysia
                30.0444f, 31.2357f, // Cairo, Egypt
                34.0522f, -118.2437f, // Los Angeles, USA
                31.5497f, 74.3436f, // Lahore, Pakistan
                6.5244f, 3.3792f, // Lagos, Nigeria
                55.7558f, 37.6173f // Moscow, Russia
        };

        TimeZoneEngine engineNoAcceleration = TimeZoneEngine.initialize(false);
        TimeZoneEngine engineWithAcceleration = TimeZoneEngine.initialize(true);
    }

    @Benchmark
    public void testAcceleratedEngine(BenchmarkState state, Blackhole blackhole) {
        for (int i = 0; i < state.cityCoordinates.length; i += 2) {
            blackhole.consume(
                    state.engineWithAcceleration.query(state.cityCoordinates[i], state.cityCoordinates[i + 1]));
        }

    }

    @Benchmark
    public void testNonAcceleratedEngine(BenchmarkState state, Blackhole blackhole) {
        for (int i = 0; i < state.cityCoordinates.length; i += 2) {
            blackhole.consume(
                    state.engineNoAcceleration.query(state.cityCoordinates[i], state.cityCoordinates[i + 1]));
        }
    }

}
