package personal.knox;

public final class EvacuationPolicyTest {
    private static EvacuationPolicy.Situation s(int zombies, int rescuers, int armed, boolean car, double risk) {
        return new EvacuationPolicy.Situation(zombies, rescuers, armed, car, risk);
    }

    public static void run() {
        assert EvacuationPolicy.decide(s(2, 3, 1, false, 0)) == EvacuationPolicy.Decision.COME_TO_YOU : "Few zombies";
        assert EvacuationPolicy.decide(s(8, 3, 1, false, 0)) == EvacuationPolicy.Decision.YOU_COME_TO_US : "Mid threat";
        assert EvacuationPolicy.decide(s(30, 3, 1, false, 0)) == EvacuationPolicy.Decision.REFUSE : "Too many";
        assert EvacuationPolicy.decide(s(0, 0, 0, false, 0)) == EvacuationPolicy.Decision.REFUSE : "No rescuers";
        assert EvacuationPolicy.decide(s(7, 3, 1, true, 0)) == EvacuationPolicy.Decision.COME_TO_YOU : "Vehicle adds strength";
        assert EvacuationPolicy.decide(s(12, 3, 1, false, 0.9)) != EvacuationPolicy.Decision.COME_TO_YOU : "Route risk lowers strength";
        assert EvacuationPolicy.decide(s(7, 3, 3, false, 0)) == EvacuationPolicy.Decision.COME_TO_YOU : "Armed rescuers add strength";
        try { s(1, 1, 2, false, 0); throw new AssertionError("More armed than rescuers accepted"); }
        catch (IllegalArgumentException expected) { }
        try { s(1, 1, 1, false, 1.1); throw new AssertionError("Bad risk accepted"); }
        catch (IllegalArgumentException expected) { }
        try { EvacuationPolicy.decide(null); throw new AssertionError("Null situation accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Evacuation tests passed: come, you-come, refuse, no rescuers, vehicle, armed and route risk.");
    }
}
