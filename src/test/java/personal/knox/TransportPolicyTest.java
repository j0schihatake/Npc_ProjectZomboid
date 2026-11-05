package personal.knox;

public final class TransportPolicyTest {
    public static void run() {
        assert TransportPolicy.mode(100, false, false, 0).equals("WALK".equals("WALK") ? TransportPolicy.Mode.WALK : null);
        assert !TransportPolicy.needsHelper(TransportPolicy.Mode.WALK);
        assert TransportPolicy.mode(70, true, false, 0) == TransportPolicy.Mode.SUPPORT : "Limping needs support";
        assert TransportPolicy.mode(20, false, false, 0) == TransportPolicy.Mode.CARRY : "Cannot walk";
        assert TransportPolicy.mode(30, false, true, 0) == TransportPolicy.Mode.CARRY : "Bleeding and low health";
        assert TransportPolicy.mode(70, false, true, 0) == TransportPolicy.Mode.SUPPORT : "Bleeding but can walk";
        assert TransportPolicy.mode(90, false, false, 0.9) == TransportPolicy.Mode.SUPPORT : "Exhausted";
        assert TransportPolicy.speedFactor(TransportPolicy.Mode.WALK) == 1.0;
        assert TransportPolicy.speedFactor(TransportPolicy.Mode.SUPPORT) < 1.0;
        assert TransportPolicy.speedFactor(TransportPolicy.Mode.CARRY) < TransportPolicy.speedFactor(TransportPolicy.Mode.SUPPORT);
        try { TransportPolicy.mode(-1, false, false, 0); throw new AssertionError("Negative health accepted"); }
        catch (IllegalArgumentException expected) { }
        try { TransportPolicy.mode(50, false, false, 1.1); throw new AssertionError("Bad fatigue accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Transport tests passed: walk, support, carry, helper need, speed penalty and invalid state.");
    }
}
