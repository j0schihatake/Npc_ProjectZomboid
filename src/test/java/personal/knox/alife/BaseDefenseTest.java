package personal.knox.alife;

public final class BaseDefenseTest {
    public static void run() {
        assert BaseDefense.guards(1, 10) == 2;
        assert BaseDefense.guards(7, 3) == 3 : "Guards never exceed population";
        assert BaseDefense.firearms(1, 3, 100) == 0;
        assert BaseDefense.firearms(5, 4, 40) == 4;
        assert BaseDefense.firearms(5, 4, 0) == 0;
        assert BaseDefense.gearScore(7, 100, 60) > BaseDefense.gearScore(2, 10, 5);
        try { BaseDefense.guards(0, 1); throw new AssertionError("Bad level accepted"); }
        catch (IllegalArgumentException expected) { }
        try { BaseDefense.gearScore(1, -1, 0); throw new AssertionError("Negative materials accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Base-defense tests passed: guard count, firearms, gear scaling and invalid data.");
    }
}
