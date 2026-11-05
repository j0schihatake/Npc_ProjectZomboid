package personal.knox;

public final class ReloadPolicyTest {
    public static void run() {
        assert ReloadPolicy.shouldReload(5, 15, 20, true, false, false);
        assert !ReloadPolicy.shouldReload(15, 15, 20, true, false, false) : "Full magazine";
        assert !ReloadPolicy.shouldReload(5, 15, 0, true, false, false) : "No reserve";
        assert !ReloadPolicy.shouldReload(5, 15, 20, false, false, false) : "Not armed";
        assert !ReloadPolicy.shouldReload(5, 15, 20, true, true, false) : "Busy";
        assert !ReloadPolicy.shouldReload(5, 15, 20, true, false, true) : "Driving";
        try { ReloadPolicy.shouldReload(-1, 15, 20, true, false, false); throw new AssertionError("Bad current accepted"); }
        catch (IllegalArgumentException expected) { }
        try { ReloadPolicy.shouldReload(5, 0, 20, true, false, false); throw new AssertionError("Bad max accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Reload tests passed: partial, full, no reserve, unarmed, busy, driving and invalid state.");
    }
}
