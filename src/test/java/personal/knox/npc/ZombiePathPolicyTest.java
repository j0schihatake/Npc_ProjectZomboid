package personal.knox.npc;

public final class ZombiePathPolicyTest {
    public static void run() {
        var policy = new ZombiePathPolicy();
        assert !policy.ropeAllowed("z1", false) : "No-rope mode must deny rope";
        policy.setMode("WITH_ROPE");
        assert policy.ropeAllowed("z1", false) && !policy.ropeAllowed("z1", true);
        policy.setMode("RANDOM");
        int allowed = 0;
        for (int i = 0; i < 100; i++) if (policy.ropeAllowed("zombie#" + i, false)) allowed++;
        assert allowed >= 30 && allowed <= 70 : "Random rope access should be mixed";
        assert !policy.ropeAllowed("zombie#1", true) : "Crawlers never use rope";
        policy.setMode("NO_ROPE");
        assert policy.status().contains("no_rope");
        System.out.println("Zombie path tests passed: no-rope, with-rope, random and crawler exclusion.");
    }
}
