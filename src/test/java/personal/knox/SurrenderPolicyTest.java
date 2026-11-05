package personal.knox;

public final class SurrenderPolicyTest {
    public static void run() {
        assert SurrenderPolicy.considers(30, 1, 1, 3, 0.2) : "Hopeless enemy surrenders";
        assert !SurrenderPolicy.considers(100, 5, 0, 1, 1.0) : "Strong group does not";
        assert !SurrenderPolicy.considers(100, 0, 5, 5, 0.1) : "No enemies";
        assert SurrenderPolicy.relationAfterSpare(0) == 40 : "Mercy raises standing";
        assert SurrenderPolicy.relationAfterSpare(80) == 100 : "Standing clamps";
        try { SurrenderPolicy.considers(-1, 1, 1, 1, 0.5); throw new AssertionError("Bad health accepted"); }
        catch (IllegalArgumentException expected) { }
        try { SurrenderPolicy.considers(50, 1, 1, 1, 1.1); throw new AssertionError("Bad morale accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Surrender tests passed: pressure, hopeless/strong, no enemies and mercy standing.");
    }
}
