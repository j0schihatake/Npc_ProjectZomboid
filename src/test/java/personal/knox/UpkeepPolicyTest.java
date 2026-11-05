package personal.knox;

public final class UpkeepPolicyTest {
    public static void run() {
        assert UpkeepPolicy.needsRepair(0, 10);
        assert UpkeepPolicy.needsRepair(9, 10);
        assert !UpkeepPolicy.needsRepair(10, 10);
        assert !UpkeepPolicy.needsRepair(11, 10) : "Condition above maximum is not repaired";
        assert !UpkeepPolicy.needsRepair(0, 0) : "Items without condition are ignored";
        assert !UpkeepPolicy.needsRepair(-1, 10);
        assert UpkeepPolicy.ammoToAdd(0, 10) == 30;
        assert UpkeepPolicy.ammoToAdd(10, 10) == 20;
        assert UpkeepPolicy.ammoToAdd(30, 10) == 0;
        assert UpkeepPolicy.ammoToAdd(100, 10) == 0;
        assert UpkeepPolicy.ammoToAdd(0, 1000) == 300 : "Reserve is capped";
        assert UpkeepPolicy.ammoToAdd(0, 0) == 0;
        assert UpkeepPolicy.ammoToAdd(-1, 10) == 0;
        assert UpkeepPolicy.ammoToAdd(0, 1001) == 0;
        assert UpkeepPolicy.needsMagazine(0);
        assert !UpkeepPolicy.needsMagazine(1);
        System.out.println("Upkeep tests passed: durability threshold, ammo reserve cap, magazine need, invalid data.");
    }
}
