package personal.knox;

public final class LineOfFireTest {
    private static ShotPriority.Point p(double x, double y) { return new ShotPriority.Point(x, y); }

    public static void run() {
        var shooter = p(0, 0);
        var target = p(10, 0);
        assert LineOfFire.blocked(shooter, target, java.util.List.of(p(5, 0)), 0.5) : "Ally in the middle blocks";
        assert !LineOfFire.blocked(shooter, target, java.util.List.of(p(5, 2)), 0.5) : "Ally far from the line";
        assert !LineOfFire.blocked(shooter, target, java.util.List.of(p(-5, 0)), 0.5) : "Ally behind the shooter";
        assert LineOfFire.blocked(shooter, target, java.util.List.of(p(10, 0)), 0.5) : "Ally at the target";
        assert LineOfFire.blocked(shooter, target, java.util.List.of(p(5, 0.5)), 0.5) : "Boundary radius blocks";
        assert !LineOfFire.blocked(shooter, target, java.util.List.of(), 0.5);
        assert LineOfFire.blocked(shooter, target, null, 0.5) == false : "No allies means free line";
        assert LineOfFire.blocked(shooter, null, java.util.List.of(p(5, 0)), 0.5) : "Missing target fails closed";
        assert LineOfFire.blocked(shooter, target, java.util.List.of(p(5, 0)), -1) : "Invalid radius fails closed";
        assert LineOfFire.blocked(shooter, target, java.util.List.of(p(5, 0)), 0) : "Exact line blocks";
        System.out.println("Line-of-fire tests passed: mid-line, off-line, behind, target, radius, empty, invalid and fail-closed.");
    }
}
