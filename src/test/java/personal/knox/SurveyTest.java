package personal.knox;

public final class SurveyTest {
    private static Survey.Answer a(String option, double weight) { return new Survey.Answer(option, weight); }

    public static void run() {
        assert Survey.result(java.util.List.of(), null, "hold").equals("hold") : "Empty survey falls back";
        assert Survey.result(java.util.List.of(a("attack", 2), a("hold", 1)), null, "hold").equals("attack");
        assert Survey.result(java.util.List.of(a("attack", 1), a("retreat", 2)), null, "hold").equals("retreat");
        assert Survey.result(java.util.List.of(a("attack", 1), a("retreat", 1)), null, "hold").equals("hold") : "Tie falls back";
        assert Survey.result(java.util.List.of(a("attack", 10)), "hold", "retreat").equals("hold") : "Leader override";
        assert Survey.result(java.util.List.of(a("tv", 2), a("music", 1)), null, "rest").equals("tv");
        try { a("", 1); throw new AssertionError("Empty option accepted"); }
        catch (IllegalArgumentException expected) { }
        try { a("tv", 0); throw new AssertionError("Zero weight accepted"); }
        catch (IllegalArgumentException expected) { }
        try { Survey.result(java.util.Arrays.asList((Survey.Answer) null), null, "hold"); throw new AssertionError("Null answer accepted"); }
        catch (IllegalArgumentException expected) { }
        try { Survey.result(java.util.List.of(a("tv", 1)), null, ""); throw new AssertionError("Empty fallback accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Survey tests passed: weighted options, leader override, tie fallback, empty survey and invalid input.");
    }
}
