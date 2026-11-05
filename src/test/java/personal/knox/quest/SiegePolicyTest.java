package personal.knox.quest;

public final class SiegePolicyTest {
    public static void run() {
        assert SiegePolicy.hordeSize(1) == 15 && SiegePolicy.hordeSize(7) == 45;
        assert SiegePolicy.requiredSurvivors(3) == 1;
        assert SiegePolicy.requiredSurvivors(6) == 2;
        assert SiegePolicy.requiredSurvivors(9) == 3;
        assert SiegePolicy.succeeded(1, 3) && !SiegePolicy.succeeded(0, 3);
        try { SiegePolicy.hordeSize(0); throw new AssertionError("Bad level accepted"); }
        catch (IllegalArgumentException expected) { }
        try { SiegePolicy.requiredSurvivors(0); throw new AssertionError("Empty group accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Siege tests passed: horde by level, one-third survivors, success and invalid data.");
    }
}
