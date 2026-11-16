package dev.windex.fusion.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Repeated light updates can suggest activity, but NEVER establish player presence. */
public final class LightCorrelator {
    public record Chunk(int x, int z) {}
    private static final class Sample {
        int count;
        long start;
        long last;
    }
    private final Map<Chunk, Sample> samples = new HashMap<>();

    public int note(int x, int z, long nowMs, int windowSeconds) {
        Chunk key = new Chunk(x, z);
        Sample s = samples.computeIfAbsent(key, k -> new Sample());
        if (s.start == 0 || nowMs - s.start > windowSeconds * 1000L) {
            s.start = nowMs;
            s.count = 0;
        }
        s.count++;
        s.last = nowMs;
        if (samples.size() > 300) prune(nowMs, windowSeconds * 1000L);
        return s.count;
    }

    public void prune(long now, long windowMs) {
        Iterator<Map.Entry<Chunk, Sample>> it = samples.entrySet().iterator();
        while (it.hasNext()) if (now - it.next().getValue().last > windowMs) it.remove();
        if (samples.size() > 300) samples.clear();
    }

    public void clear() { samples.clear(); }
}
