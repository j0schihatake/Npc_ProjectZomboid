package personal.knox.npc.own;

public final class OwnNpcTargetZCodecTest {
    public static void run() {
        var values = new java.util.LinkedHashMap<String, Double>();
        values.put("n1", 0.0);
        values.put("n2", 1.0);
        var back = OwnNpcTargetZCodec.decode(OwnNpcTargetZCodec.encode(values));
        assert back.equals(values);
        try { OwnNpcTargetZCodec.decode("bad"); throw new AssertionError("Bad line accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Own-NPC target-Z codec tests passed: round-trip and invalid line.");
    }
}
