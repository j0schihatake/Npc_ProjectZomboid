package personal.knox;

public final class BetrayalRiskTest {
    private static BetrayalRisk.History h(int trust, int refused, int ignored, int gifts, int shared) {
        return new BetrayalRisk.History(trust, refused, ignored, gifts, shared);
    }

    public static void run() {
        assert BetrayalRisk.signal(BetrayalRisk.score(h(90, 0, 0, 2, 2))) == BetrayalRisk.Signal.CALM;
        assert BetrayalRisk.signal(BetrayalRisk.score(h(30, 1, 0, 0, 0))) == BetrayalRisk.Signal.WARNING;
        assert BetrayalRisk.signal(BetrayalRisk.score(h(10, 3, 2, 0, 0))) == BetrayalRisk.Signal.CRITICAL;
        assert BetrayalRisk.score(h(50, 0, 0, 5, 0)) < BetrayalRisk.score(h(50, 0, 0, 0, 0)) : "Gifts lower risk";
        assert BetrayalRisk.score(h(50, 2, 0, 0, 0)) > BetrayalRisk.score(h(50, 0, 0, 0, 0)) : "Refusals raise risk";
        assert BetrayalRisk.score(h(0, 20, 20, 0, 0)) == 100 : "Clamp high";
        assert BetrayalRisk.score(h(100, 0, 0, 20, 20)) == 0 : "Clamp low";
        try { h(-1, 0, 0, 0, 0); throw new AssertionError("Bad trust accepted"); }
        catch (IllegalArgumentException expected) { }
        try { BetrayalRisk.signal(101); throw new AssertionError("Bad score accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Betrayal tests passed: calm/warning/critical, gifts, refusals, clamps and invalid input.");
    }
}
