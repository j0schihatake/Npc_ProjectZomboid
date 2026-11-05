package personal.knox.alife;

public final class RaidTest {
    public static void run() {
        var raid = new Raid("raid-1", "raiders", Raid.TargetKind.BASE, "farm", 20, 100, Raid.State.MARCHING);
        assert raid.size() == 20;
        assert raid.withState(Raid.State.BATTLE).state() == Raid.State.BATTLE;
        try { new Raid("x", "a", Raid.TargetKind.BASE, "b", 9, 0, Raid.State.MARCHING); throw new AssertionError("Raid below 10 accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new Raid("x", "a", Raid.TargetKind.BASE, "b", 31, 0, Raid.State.MARCHING); throw new AssertionError("Raid above 30 accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new Raid("", "a", Raid.TargetKind.BASE, "b", 10, 0, Raid.State.MARCHING); throw new AssertionError("Empty id accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Raid-object tests passed: valid size, state transition and invalid data.");
    }
}
