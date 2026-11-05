package personal.knox.alife;

public final class AssaultResolverTest {
    public static void run() {
        var strong = AssaultResolver.resolve(30, 0.9, 2, 0.2, 50);
        assert strong.attackersWin() : "Large geared raid wins";
        var weak = AssaultResolver.resolve(10, 0.1, 20, 0.9, 10);
        assert !weak.attackersWin() : "Small weak raid loses";
        assert strong.attackerLosses() <= 30 && strong.defenderLosses() <= 2;
        try { AssaultResolver.resolve(9, 0.5, 2, 0.5, 0); throw new AssertionError("Raid below 10 accepted"); }
        catch (IllegalArgumentException expected) { }
        try { AssaultResolver.resolve(10, 1.1, 2, 0.5, 0); throw new AssertionError("Bad gear accepted"); }
        catch (IllegalArgumentException expected) { }
        try { AssaultResolver.resolve(10, 0.5, -1, 0.5, 0); throw new AssertionError("Negative defenders accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Assault tests passed: win/loss, bounded losses and invalid data.");
    }
}
