package dev.windex.fusion.analysis;

/** Dependency-free smoke tests: javac -d /tmp/windex-test ... && java -cp ... AnalysisSelfTest */
public final class AnalysisSelfTest {
    public static void main(String[] args) {
        ParticleCorrelator p = new ParticleCorrelator();
        long now = 100_000;
        for (int i = 0; i < 3; i++) {
            if (p.note(true, 13.2, 35.1, -8.5, now+i, 3, 25_000) != null)
                throw new AssertionError("single particle type flagged as spawner");
        }
        for (int i = 0; i < 2; i++) {
            if (p.note(false, 13.8, 35.7, -8.1, now+10+i, 3, 25_000) != null)
                throw new AssertionError("insufficient flame samples");
        }
        ParticleCorrelator.Evidence candidate =
                p.note(false, 13.8, 35.3, -8.2, now+13, 3, 25_000);
        if (candidate == null || candidate.block().z() != -9)
            throw new AssertionError("expected same-block paired particles");
        if (p.note(false, 13.8, 35.3, -8.2, now+40_000, 3, 25_000) != null)
            throw new AssertionError("old evidence not expired");

        Projection.Target target = Projection.surface(-31, 49, 3, (x,z)->80+x-z);
        if (target.x() != -11 || target.z() != 16 || target.y() != 53)
            throw new AssertionError("surface projection shifted from evidence column");

        LightCorrelator activity = new LightCorrelator();
        if (activity.note(-2,4,1000,10) != 1 || activity.note(-2,4,1001,10) != 2
                || activity.note(-2,4,12_000,10) != 1)
            throw new AssertionError("light window counting broken");

        Markers markers = new Markers();
        markers.upsert(Markers.Kind.SUS, "test", 3,64,3,70,"direct",10000);
        if (markers.nearby(0,0,20).size() != 1 || markers.nearby(0,0,2).size() != 0)
            throw new AssertionError("marker distance filtering broken");
        System.out.println("AnalysisSelfTest: all checks passed");
    }
}
