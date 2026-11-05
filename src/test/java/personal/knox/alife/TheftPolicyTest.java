package personal.knox.alife;

public final class TheftPolicyTest {
    public static void run() {
        assert !TheftPolicy.isTheft(true, false, false);
        assert !TheftPolicy.isTheft(false, true, false);
        assert !TheftPolicy.isTheft(false, false, true);
        assert TheftPolicy.isTheft(false, false, false);
        assert TheftPolicy.relationDelta(true, false, 5) == 0;
        assert TheftPolicy.relationDelta(false, true, 0) == -3;
        assert TheftPolicy.relationDelta(false, false, 1) == -16;
        assert TheftPolicy.escalatesToWar(-40, 0);
        assert TheftPolicy.escalatesToWar(0, 3);
        assert !TheftPolicy.escalatesToWar(0, 2);
        try { TheftPolicy.relationDelta(false, false, -1); throw new AssertionError("Negative offences accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Theft tests passed: ownership, permission, placing, relation loss and war escalation.");
    }
}
