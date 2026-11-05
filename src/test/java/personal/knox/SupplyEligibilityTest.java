package personal.knox;

public final class SupplyEligibilityTest {
    public static void run() {
        var api = PersonalKnoxAPI.getInstance();
        assert api.isSupplyFood(-.08, true, false, false, false, false, false);
        assert api.isSupplyFood(-.15, true, false, false, true, true, false);
        assert !api.isSupplyFood(-.15, true, false, false, true, false, false);
        assert !api.isSupplyFood(-.08, true, true, false, false, false, false);
        assert !api.isSupplyFood(-.08, true, false, true, false, false, false);
        assert !api.isSupplyFood(-.08, false, false, false, false, false, false);
        assert !api.isSupplyFood(-.08, true, false, false, false, false, true);
        assert !api.isSupplyFood(0, true, false, false, false, false, false);
        assert !api.isSupplyFood(Double.NaN, true, false, false, false, false, false);
        assert api.isSupplyWater(.5, true, false);
        assert !api.isSupplyWater(0, true, false);
        assert !api.isSupplyWater(.5, true, true);
        assert !api.isSupplyWater(.5, false, false);
        assert !api.isSupplyWater(Double.POSITIVE_INFINITY, true, false);
        assert !api.isSupplyWater(-1, true, false);
        System.out.println("Supply readiness tests passed: edible food, cooked dangerous foods, known poison, clean nonempty water and invalid data.");
    }
}
