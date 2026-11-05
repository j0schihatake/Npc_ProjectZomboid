package personal.knox.alife;

public final class AlifeRuntimeTest {
    public static void run() {
        var runtime = new AlifeRuntime();
        runtime.submit("s1", "own", 1, 90, 2, "REGROUP", 10, 10, 0, 50, 50);
        assert runtime.squadCount() == 1;
        var summary = runtime.tick(1000, 10, 10, 0, 8);
        assert summary.contains("NEAR_SQUAD") : summary;
        runtime.submit("s1", "own", 1, 80, 2, "REGROUP", 12, 12, 0, 50, 50);
        assert runtime.squadCount() == 1;
        try { runtime.submit("s1", "own", 1, 90, 2, "BAD", 0, 0, 0, 0, 0); }
        catch (RuntimeException unexpected) { throw new AssertionError("Bad task should fall back", unexpected); }
        System.out.println("A-Life runtime tests passed: submit/update, tick summary and task fallback.");
    }
}
