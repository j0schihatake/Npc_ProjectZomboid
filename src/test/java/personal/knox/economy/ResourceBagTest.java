package personal.knox.economy;

import personal.knox.alife.FactionEconomy;

public final class ResourceBagTest {
    public static void run() {
        var bag = new ResourceBag(ResourceBag.Kind.FOOD, 3);
        assert bag.take(2) == 2 && bag.units() == 1;
        var economy = new FactionEconomy(0);
        assert bag.depositInto(economy) == 1;
        assert economy.get(FactionEconomy.Resource.FOOD) == 1 && bag.empty();
        assert new ResourceStation(ResourceStation.Profession.MEDIC, 3).output() == ResourceBag.Kind.MEDICAL;
        assert new ResourceStation(ResourceStation.Profession.COOK, 3).craft(3, 0).empty();
        assert !new ResourceStation(ResourceStation.Profession.ARMORER, 3).craft(4, 2).empty();
        try { new ResourceBag(null, 1); throw new AssertionError("Null kind accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new ResourceBag(ResourceBag.Kind.FOOD, 7); throw new AssertionError("Over capacity accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new ResourceStation(ResourceStation.Profession.COOK, 0); throw new AssertionError("Bad level accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Resource-bag tests passed: three kinds, deposit, station output, worker requirement and invalid data.");
    }
}
