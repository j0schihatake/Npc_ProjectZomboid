package personal.knox.npc.own;

import personal.knox.npc.NpcState;

public final class OwnNpcCodecTest {
    public static void run() {
        var state = new NpcState(88.5, 0.2, 0.3, 0.4, 0.7, true, false,
                NpcState.Role.MEDIC, NpcState.Task.WORK);
        var agent = new OwnNpcAgent("n1", "own", 1.5, 2.5, 0, 10, 11, 3, state);
        var text = OwnNpcCodec.encode(java.util.List.of(agent));
        var restored = OwnNpcCodec.decode(text);
        assert restored.size() == 1;
        assert restored.get(0).equals(agent) : restored.get(0);
        try { OwnNpcCodec.decode("bad"); throw new AssertionError("Bad line accepted"); }
        catch (IllegalArgumentException expected) { }
        try { OwnNpcCodec.encode(java.util.List.of(new OwnNpcAgent("a|b", "own", 0, 0, 0, 0, 0, 0, state))); throw new AssertionError("Separator in id accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Own-NPC codec tests passed: round-trip, invalid line and separator rejection.");
    }
}
