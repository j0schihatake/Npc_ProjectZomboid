package personal.knox;

public final class ShotPriorityTest {
    private static ShotPriority.Point p(double x, double y) { return new ShotPriority.Point(x, y); }

    public static void run() {
        var ally = p(0, 0);
        var shooter = p(20, 0);
        var closeToAlly = p(1, 0);
        var closeToShooter = p(18, 0);
        assert ShotPriority.choose(ally, shooter, java.util.List.of(closeToShooter, closeToAlly), 100) == 1
                : "Enemy closest to the wounded ally wins";
        assert ShotPriority.choose(ally, shooter, java.util.List.of(p(10, 0), p(0, 10)), 100) == 0
                : "Equal distance to ally falls back to the shooter";
        assert ShotPriority.choose(ally, shooter, java.util.List.of(p(200, 0)), 30) == -1 : "Out of range";
        assert ShotPriority.choose(ally, shooter, java.util.List.of(), 30) == -1;
        assert ShotPriority.choose(null, shooter, java.util.List.of(closeToAlly), 30) == -1;
        assert ShotPriority.choose(ally, shooter, java.util.List.of(closeToAlly), 0) == -1;
        assert ShotPriority.choose(ally, shooter, java.util.Arrays.asList(closeToAlly, null), 100) == 0 : "Null enemy is skipped";
        try { p(Double.NaN, 0); throw new AssertionError("NaN point accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Shot priority tests passed: ally-first target, shooter tie-break, range, empty, null and invalid points.");
    }
}
