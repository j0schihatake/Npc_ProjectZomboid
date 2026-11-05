package personal.knox;

public final class PlayerCareTest {
    public static void run() {
        var care = new PlayerCare();
        assert care.decide(1000, 100, 0, 0, 0, 0, false, false, true, true, true).equals("none");
        assert care.decide(1000, 100, 1, 0, 0, 0, false, false, true, true, true).equals("bandage");
        care.done("bandage", 1000);
        assert care.decide(2000, 100, 1, 0, 0, 0, false, false, true, true, true).equals("none")
                : "Action cooldown must apply";
        assert care.decide(5000, 100, 0, 50, 0, 0, false, false, true, true, true).equals("painkiller");
        assert care.decide(5000, 100, 0, 0, .3, 0, false, false, true, true, true).equals("drink");
        assert care.decide(5000, 100, 0, 0, 0, .3, false, false, true, true, true).equals("eat");
        assert care.decide(5000, 100, 1, 0, .3, .3, false, false, true, true, true).equals("bandage")
                : "Bleeding outranks needs";

        var busy = new PlayerCare();
        assert busy.decide(1000, 100, 1, 50, .5, .5, true, false, true, true, true).equals("none")
                : "Never interrupt a timed action";

        var danger = new PlayerCare();
        assert danger.decide(1000, 100, 1, 50, .5, .5, false, true, true, true, true).equals("bandage")
                : "Bandaging is allowed under threat";
        assert danger.decide(5000, 100, 0, 50, .5, .5, false, true, true, true, true).equals("none")
                : "No pills or food while threatened";

        var flags = new PlayerCare();
        assert flags.decide(1000, 100, 1, 50, .5, .5, false, false, false, false, false).equals("none");
        assert flags.decide(1000, 100, 0, 0, .5, 0, false, false, false, false, false).equals("none");
        assert flags.decide(1000, 100, 0, 0, .5, 0, false, false, false, true, false).equals("drink");
        assert flags.decide(1000, 100, 0, 0, 0, .5, false, false, false, false, true).equals("eat");

        var retry = new PlayerCare();
        assert retry.decide(1000, 100, 0, 0, .5, 0, false, false, false, true, false).equals("drink");
        retry.unavailable("drink", 1000);
        assert retry.decide(2000, 100, 0, 0, .5, 0, false, false, false, true, false).equals("none");
        assert retry.decide(31000, 100, 0, 0, .5, 0, false, false, false, true, false).equals("drink");
        retry.done("drink", 31000);
        retry.reset();
        assert retry.retryAt("drink") == Long.MIN_VALUE;

        var bounds = new PlayerCare();
        assert bounds.decide(1000, 100, 0, 0, .20, 0, false, false, false, true, false).equals("drink");
        assert bounds.decide(1000, 100, 0, 0, .19, 0, false, false, false, true, false).equals("none");
        assert bounds.decide(1000, 100, 0, 0, 0, .25, false, false, false, false, true).equals("eat");
        assert bounds.decide(1000, 100, 0, 0, 0, .24, false, false, false, false, true).equals("none");
        assert bounds.decide(1000, 100, 0, 39.9, 0, 0, false, false, true, false, false).equals("none");
        assert bounds.decide(1000, 100, 0, 40, 0, 0, false, false, true, false, false).equals("painkiller");
        assert bounds.decide(1000, 100, 0, Double.NaN, 0, 0, false, false, true, false, false).equals("none");
        assert bounds.decide(-1, 100, 0, 0, 0, 0, false, false, true, true, true).equals("none");
        System.out.println("Player care tests passed: priorities, busy, danger, flags, cooldowns, retries, thresholds.");
    }
}
