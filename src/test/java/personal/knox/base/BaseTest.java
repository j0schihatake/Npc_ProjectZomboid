package personal.knox.base;

public final class BaseTest {
    private static Workforce.Member m(String id, double health, boolean free, boolean wounded) {
        return new Workforce.Member(id, health, free, wounded);
    }

    public static void run() {
        var wounded = m("w", 80, true, true);
        var busy = m("b", 90, false, false);
        var weak = m("k", 30, true, false);
        var fit = m("f", 80, true, false);
        assert !Workforce.able(wounded) && !Workforce.able(busy) && !Workforce.able(weak);
        assert Workforce.able(fit);
        assert Workforce.available(java.util.List.of(wounded, busy, weak, fit)) == 1;
        assert Math.abs(Workforce.capacity(java.util.List.of(fit, m("g", 50, true, false))) - 1.3) < 1e-9;

        var tasks = java.util.List.of(new Workforce.Task("low", 1, 1), new Workforce.Task("high", 1, 10));
        var assignments = Workforce.allocate(java.util.List.of(fit, m("g", 50, true, false)), tasks);
        assert assignments.size() == 2;
        assert assignments.get(0).taskId().equals("high") : "Priority first";
        assert assignments.get(0).memberIds().size() == 1;

        assert Workforce.allocate(java.util.List.of(wounded), tasks).isEmpty() : "Wounded are not assigned";
        try { new Workforce.Member("", 50, true, false); throw new AssertionError("Empty id accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new Workforce.Task("x", 0, 1); throw new AssertionError("Zero workers accepted"); }
        catch (IllegalArgumentException expected) { }

        assert MedicalRest.needsRest(40, false, 0);
        assert MedicalRest.needsRest(100, true, 0);
        assert MedicalRest.needsRest(100, false, 0.8);
        assert !MedicalRest.needsRest(100, false, 0);
        assert MedicalRest.needsMedic(40, false);
        assert MedicalRest.needsMedic(100, true);
        assert MedicalRest.place(true, true, true) == MedicalRest.Place.BED;
        assert MedicalRest.place(false, true, true) == MedicalRest.Place.SOFA;
        assert MedicalRest.place(false, false, true) == MedicalRest.Place.CHAIR;
        assert MedicalRest.place(false, false, false) == MedicalRest.Place.FLOOR;
        assert MedicalRest.Place.BED.healPerHour() > MedicalRest.Place.FLOOR.healPerHour();
        System.out.println("Base tests passed: free/health allocation, priority, capacity, rest need, medic, place and healing.");
    }
}
