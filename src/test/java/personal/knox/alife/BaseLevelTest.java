package personal.knox.alife;

public final class BaseLevelTest {
    public static void run() {
        assert !BaseLevel.selfSufficient(4) && BaseLevel.selfSufficient(5) && BaseLevel.selfSufficient(7);
        assert BaseLevel.externalNeed(1) > BaseLevel.externalNeed(4);
        assert BaseLevel.externalNeed(5) == 0;
        assert BaseLevel.beds(1) == 2 && BaseLevel.beds(7) == 8;
        assert BaseLevel.workbenches(1) == 0 && BaseLevel.workbenches(7) == 3;
        assert BaseLevel.chests(7) > BaseLevel.chests(1);
        assert BaseLevel.gardens(7) > BaseLevel.gardens(1);
        assert BaseLevel.animalPens(1) == 0 && BaseLevel.animalPens(7) >= 2;
        try { BaseLevel.selfSufficient(0); throw new AssertionError("Zero level accepted"); }
        catch (IllegalArgumentException expected) { }
        try { BaseLevel.selfSufficient(8); throw new AssertionError("Level 8 accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Base-level tests passed: 1-7, self-sufficiency, external need and production growth.");
    }
}
