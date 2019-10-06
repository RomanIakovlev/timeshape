package net.iakovlev.timeshape;

import com.esri.core.geometry.*;
import net.iakovlev.timeshape.TimeZoneEngine;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class BasicGeoOperationsBenchmark {
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        TimeZoneEngine engine = TimeZoneEngine.initialize();
        Index index;
        QuadTree quadTree;
        Point p = new Point(13.31, 52.52);
        ArrayList<Index.Entry> entries;
        Geometry matchingGeometry;
        ArrayList<Geometry> nonMatchingGeometries = new ArrayList<>();
        SpatialReference spatialReference = SpatialReference.create(4326);
        @Setup
        public void setup() {
            try {
                Field indexField = engine.getClass().getDeclaredField("index");
                indexField.setAccessible(true);
                index = (Index) indexField.get(engine);
                Field quadTreeField = index.getClass().getDeclaredField("quadTree");
                quadTreeField.setAccessible(true);
                quadTree = (QuadTree) quadTreeField.get(index);
                Field zoneIdsField = index.getClass().getDeclaredField("zoneIds");
                zoneIdsField.setAccessible(true);
                entries = (ArrayList<Index.Entry>) zoneIdsField.get(index);
                QuadTree.QuadTreeIterator iterator = quadTree.getIterator(p, 0);
                for (int i = iterator.next(); i >= 0; i = iterator.next()) {
                    int element = quadTree.getElement(i);
                    Index.Entry entry = entries.get(element);
                    if (GeometryEngine.contains(entry.geometry, p, spatialReference)) {
                        matchingGeometry = entry.geometry;
                    } else {
                        nonMatchingGeometries.add(entry.geometry);
                    }

                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

    }


    @Benchmark
    public void testQuadTree(BenchmarkState state, Blackhole blackhole) {
        QuadTree.QuadTreeIterator iterator = state.quadTree.getIterator(state.p, 0);
        for (int i = iterator.next(); i >= 0; i = iterator.next()) {
            blackhole.consume(state.quadTree.getElement(i));
        }
    }

    @Benchmark
    public void testSearchInNonMatchingGeometry(BenchmarkState state, Blackhole blackhole) {
        for (Geometry g : state.nonMatchingGeometries) {
            blackhole.consume(GeometryEngine.contains(g, state.p, state.spatialReference));
        }
    }

    @Benchmark
    public void testSearchInMatchingGeometry(BenchmarkState state, Blackhole blackhole) {
        blackhole.consume(GeometryEngine.contains(state.matchingGeometry, state.p, state.spatialReference));
    }

    @Benchmark
    public void testIndexQuery(BenchmarkState state, Blackhole blackhole) {
        blackhole.consume(state.index.query(state.p.getY(), state.p.getX()));
    }
}
