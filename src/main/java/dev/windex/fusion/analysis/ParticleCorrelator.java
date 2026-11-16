package dev.windex.fusion.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Correlates a *pair* of vanilla particle types in the same block over time.
 *  It is a hint, not proof of a spawner: torches, fires and mods can mimic it.
 */
public final class ParticleCorrelator {
    public record Block(int x, int y, int z) {}
    public record Evidence(Block block, int smoke, int flame) {}

    private static final class Sample {
        long start;
        long last;
        int smoke;
        int flame;
    }

    private final Map<Block, Sample> samples = new HashMap<>();

    public Evidence note(boolean isSmoke, double x, double y, double z,
                         long nowMs, int minEach, long windowMs) {
        Block key = new Block((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
        Sample s = samples.computeIfAbsent(key, k -> new Sample());
        if (s.start == 0 || nowMs - s.start > windowMs) {
            s.start = nowMs;
            s.flame = 0;
            s.smoke = 0;
        }
        if (isSmoke) s.smoke++; else s.flame++;
        s.last = nowMs;
        if (samples.size() > 1024) prune(nowMs, windowMs);
        if (s.smoke >= minEach && s.flame >= minEach) {
            return new Evidence(key, s.smoke, s.flame);
        }
        return null;
    }

    public void prune(long nowMs, long windowMs) {
        Iterator<Map.Entry<Block, Sample>> it = samples.entrySet().iterator();
        while (it.hasNext()) if (nowMs - it.next().getValue().last > windowMs) it.remove();
        if (samples.size() > 1024) samples.clear();
    }

    public void clear() { samples.clear(); }
}
