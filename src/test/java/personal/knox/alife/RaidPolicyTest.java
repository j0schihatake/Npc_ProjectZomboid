package personal.knox.alife;

public final class RaidPolicyTest {
    public static void run() {
        var normal = new RaidPolicy.Parameters(10, 30, 90);
        assert RaidPolicy.size(0, normal) == 10;
        assert RaidPolicy.size(99, normal) == 25;
        assert RaidPolicy.size(50, normal) >= 10 && RaidPolicy.size(50, normal) <= 30;
        assert !RaidPolicy.due(1000, 0, normal);
        assert RaidPolicy.due(90 * 60_000L, 0, normal);
        try { new RaidPolicy.Parameters(9, 30, 90); throw new AssertionError("Raid below 10 accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new RaidPolicy.Parameters(10, 31, 90); throw new AssertionError("Raid above 30 accepted"); }
        catch (IllegalArgumentException expected) { }
        try { RaidPolicy.size(100, normal); throw new AssertionError("Bad roll accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Raid tests passed: 10-30 range, deterministic size, cadence and invalid data.");
    }
}
