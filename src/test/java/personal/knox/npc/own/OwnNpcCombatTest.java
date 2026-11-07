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
        assert OwnNpcCombat.engagementRange(10, false, "clear") == 10;
        assert OwnNpcCombat.engagementRange(10, true, "clear") == 15;
        assert OwnNpcCombat.engagementRange(10, false, "rain") == 6.5;
        assert OwnNpcCombat.engagementRange(10, false, "storm") == 4.5;
        assert OwnNpcCombat.engagementRange(0, false, null) == 8;
        assert OwnNpcCombat.damage(1, 0, 10, 10, false) == 10;
        assert OwnNpcCombat.damage(1, 3, 10, 10, false) == 16;
        assert OwnNpcCombat.damage(1, 0, 0, 10, false) == 5;
        assert OwnNpcCombat.damage(5, 5, 10, 10, true) == 30;
        var away = OwnNpcCombat.retreatPoint(1, 0, 0, 0, 6, 2);
        assert Math.abs(away.x() - 3) < 1e-9 && Math.abs(away.y()) < 1e-9;
        var close = OwnNpcCombat.retreatPoint(8, 0, 0, 0, 6, 2);
        assert close.x() == 8 && close.y() == 0;
        System.out.println("Own-NPC combat tests passed: nearest visible, melee kill, ranged ammo and invalid input.");
    }
}
