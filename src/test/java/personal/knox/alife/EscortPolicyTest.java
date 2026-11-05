package personal.knox.alife;

public final class EscortPolicyTest {
    public static void run() {
        assert EscortPolicy.maxEscorts(10) == 0;
        assert EscortPolicy.maxEscorts(30) == 1;
        assert EscortPolicy.maxEscorts(60) == 2;
        assert EscortPolicy.maxEscorts(90) == 3;
        assert EscortPolicy.relationDelta(true, 0, 0, true) > 0;
        assert EscortPolicy.relationDelta(false, 1, 0, false) < -10;
        assert EscortPolicy.relationDelta(true, 0, 1, true) < -20 : "Death costs more";
        try { EscortPolicy.maxEscorts(101); throw new AssertionError("Bad relation accepted"); }
        catch (IllegalArgumentException expected) { }
        try { EscortPolicy.relationDelta(true, -1, 0, false); throw new AssertionError("Negative wounded accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Escort tests passed: relation gates, 0-3 escorts, return/wounded/death costs and invalid data.");
    }
}
