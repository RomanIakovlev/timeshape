package net.iakovlev.timeshape;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents contiguous span of points belonging to the same set of time zones
 */
public final class SameZoneSpan {
    public Set<ZoneId> getZoneIds() {
        return new HashSet<>(zoneIds);
    }

    /**
     * Last index in the
     * @return
     */
    public int getEndIndex() {
        return endIndex;
    }

    private final Set<ZoneId> zoneIds;
    private final int endIndex;

    @Override
    public int hashCode() {
        return Objects.hash(zoneIds, endIndex);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof SameZoneSpan))
            return false;
        SameZoneSpan other = (SameZoneSpan) obj;
        return other.endIndex == endIndex && other.zoneIds.equals(zoneIds);
    }

    @Override
    public String toString() {
        return String.format("%s: end index %d", zoneIds, endIndex);
    }

    SameZoneSpan(Set<ZoneId> zoneIds, int endIndex) {
        this.zoneIds = new HashSet<>(zoneIds);
        this.endIndex = endIndex;
    }

    static SameZoneSpan fromIndexEntries(List<Index.Entry> entries, int index) {
        return new SameZoneSpan(entries.stream().map(e -> e.zoneId).collect(Collectors.toSet()), index);
    }
}
