package net.iakovlev.timeshape;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;

public final class SameZoneSegment {
    final ZoneId zoneId;
    final double[] points;

    @Override
    public int hashCode() {
        return Objects.hash(zoneId, points);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof SameZoneSegment))
            return false;
        SameZoneSegment other = (SameZoneSegment) obj;
        return other.zoneId.equals(zoneId) && Arrays.equals(points, other.points);
    }

    SameZoneSegment(ZoneId zoneId, double[] points) {
        this.zoneId = zoneId;
        this.points = points;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("lat,lon\n");
        for (int i = 0; i < points.length - 1; i += 2) {
            b.append(String.format("%s,%s\n", points[i], points[i+1]));
        }
        return String.format("%s (%d points): %s", zoneId.getId(), points.length/2, b.toString());
    }
}
