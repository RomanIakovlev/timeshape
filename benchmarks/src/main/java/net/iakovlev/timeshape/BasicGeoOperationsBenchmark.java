package net.iakovlev.timeshape;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Measures the two steps a coordinate lookup is made of, in isolation: narrowing the world down to
 * a handful of candidate polygons, and testing a coordinate against one polygon.
 * <p>
 * The engine is initialized without geometry acceleration, so the containment benchmarks walk every
 * edge of the polygon. {@link AcceleratedGeometryBenchmark} covers the indexed variant.
 */
public class BasicGeoOperationsBenchmark {
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        TimeZoneEngine engine = TimeZoneEngine.initialize();
        Index index;
        Index.Grid grid;

        /** Berlin, well inside a single large polygon. */
        double lon = 13.31;
        double lat = 52.52;
        /**
         * Jerusalem, where several time zones overlap. Needed because around Berlin every polygon
         * whose bounding box covers the point also contains it, which would leave the non-matching
         * benchmark below with an empty list and nothing to measure.
         */
        double crowdedLon = 35.2;
        double crowdedLat = 31.95;

        ArrayList<Index.Entry> entries;
        PreparedPolygon matchingGeometry;
        ArrayList<PreparedPolygon> nonMatchingGeometries = new ArrayList<>();

        @Setup
        public void setup() {
            try {
                Field indexField = engine.getClass().getDeclaredField("index");
                indexField.setAccessible(true);
                index = (Index) indexField.get(engine);
                Field gridField = index.getClass().getDeclaredField("grid");
                gridField.setAccessible(true);
                grid = (Index.Grid) gridField.get(index);
                Field zoneIdsField = index.getClass().getDeclaredField("zoneIds");
                zoneIdsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                ArrayList<Index.Entry> zoneIds = (ArrayList<Index.Entry>) zoneIdsField.get(index);
                entries = zoneIds;
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            for (Index.Entry entry : entries) {
                PreparedPolygon geometry = entry.geometry;
                if (geometry.contains(lon, lat)) {
                    matchingGeometry = geometry;
                }
                boolean crowdedIsInBoundingBox = crowdedLon >= geometry.minX && crowdedLon <= geometry.maxX
                        && crowdedLat >= geometry.minY && crowdedLat <= geometry.maxY;
                if (crowdedIsInBoundingBox && !geometry.contains(crowdedLon, crowdedLat)) {
                    nonMatchingGeometries.add(geometry);
                }
            }
            if (matchingGeometry == null || nonMatchingGeometries.isEmpty()) {
                throw new IllegalStateException("Benchmark points no longer exercise what they are meant to");
            }
        }
    }

    /** Narrowing a coordinate down to the polygons that may contain it. */
    @Benchmark
    public void testQuadTree(BenchmarkState state, Blackhole blackhole) {
        int cell = state.grid.cellOf(state.lon, state.lat);
        for (int i = state.grid.firstPolygonOf(cell), end = state.grid.lastPolygonOf(cell); i < end; i++) {
            blackhole.consume(state.grid.polygonAt(i));
        }
    }

    @Benchmark
    public void testSearchInNonMatchingGeometry(BenchmarkState state, Blackhole blackhole) {
        for (PreparedPolygon g : state.nonMatchingGeometries) {
            blackhole.consume(g.contains(state.crowdedLon, state.crowdedLat));
        }
    }

    @Benchmark
    public void testSearchInMatchingGeometry(BenchmarkState state, Blackhole blackhole) {
        blackhole.consume(state.matchingGeometry.contains(state.lon, state.lat));
    }

    @Benchmark
    public void testIndexQuery(BenchmarkState state, Blackhole blackhole) {
        blackhole.consume(state.index.query(state.lat, state.lon));
    }
}
