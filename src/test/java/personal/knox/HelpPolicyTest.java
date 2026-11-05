package personal.knox;

public final class HelpPolicyTest {
    public static void run() {
        assert HelpPolicy.interval(1) == 0 : "Off has no interval";
        assert HelpPolicy.interval(2) == 15 * HelpPolicy.MINUTE;
        assert HelpPolicy.interval(3) == 8 * HelpPolicy.MINUTE;
        assert HelpPolicy.interval(4) == 4 * HelpPolicy.MINUTE;
        assert HelpPolicy.interval(0) == 0 && HelpPolicy.interval(99) == 0 : "Unknown is off";
        assert !HelpPolicy.allowed(1, 1000, 0) : "Off never allows help";
        assert HelpPolicy.allowed(3, 1000, 0) : "First event is allowed";
        assert !HelpPolicy.allowed(3, 1000 + 7 * HelpPolicy.MINUTE, 1000) : "Respect the interval";
        assert HelpPolicy.allowed(3, 1000 + 8 * HelpPolicy.MINUTE, 1000);
        assert !HelpPolicy.allowed(3, -1, 0) && !HelpPolicy.allowed(3, 1000, -1);
        System.out.println("Help policy tests passed: off/rare/normal/often intervals, first event, cooldown and invalid time.");
    }
}
