package personal.knox.npc.own;

public final class OwnNpcCombatTest {
    public static void run() {
        var near = new OwnNpcCombat.Target("z1", 2, 0, 50, true);
        var far = new OwnNpcCombat.Target("z2", 20, 0, 50, true);
        var hidden = new OwnNpcCombat.Target("z3", 1, 0, 50, false);
        var target = OwnNpcCombat.nearest(java.util.List.of(far, hidden, near), 0, 0, 10);
        assert target.id().equals("z1");
        var hit = OwnNpcCombat.melee(target, 60);
        assert hit.damage() == 50 && hit.killed();
        var shot = OwnNpcCombat.ranged(target, 10, 3);
        assert shot.ammoLeft() == 2 && !shot.killed();
        try { OwnNpcCombat.ranged(target, 10, 0); throw new AssertionError("Ranged without ammo accepted"); }
        catch (IllegalStateException expected) { }
        try { OwnNpcCombat.nearest(null, 0, 0, 1); throw new AssertionError("Null targets accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Own-NPC combat tests passed: nearest visible, melee kill, ranged ammo and invalid input.");
    }
}
