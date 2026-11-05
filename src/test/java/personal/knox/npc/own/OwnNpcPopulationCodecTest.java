package personal.knox.npc.own;

public final class OwnNpcPopulationCodecTest {
    public static void run() {
        var group = new OwnNpcPopulation.Group("base", "own", OwnNpcPopulation.Group.Kind.BASE,
                200, 1, 2, 3, 4);
        var text = OwnNpcPopulationCodec.encode(java.util.List.of(group));
        var back = OwnNpcPopulationCodec.decode(text);
        assert back.size() == 1 && back.get(0).equals(group);
        try { OwnNpcPopulationCodec.decode("bad"); throw new AssertionError("Bad line accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Own-NPC population codec tests passed: group round-trip and invalid line.");
    }
}
