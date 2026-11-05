package personal.knox.alife;

public final class FactionEconomyTest {
    public static void run() {
        var economy = new FactionEconomy(2);
        assert economy.get(FactionEconomy.Resource.FOOD) == 0;
        economy.add(FactionEconomy.Resource.FOOD, 10);
        assert economy.get(FactionEconomy.Resource.FOOD) == 10;
        assert economy.consume(FactionEconomy.Resource.FOOD, 4) == 4;
        assert economy.get(FactionEconomy.Resource.FOOD) == 6;
        assert economy.consume(FactionEconomy.Resource.FOOD, 100) == 6;
        assert economy.tick(1, 1) : "No water means starving";
        economy.add(FactionEconomy.Resource.WATER, 10);
        economy.add(FactionEconomy.Resource.FOOD, 100);
        assert !economy.tick(1, 1);
        economy.add(FactionEconomy.Resource.MATERIALS, 30);
        economy.add(FactionEconomy.Resource.WATER, 30);
        assert economy.canExpand();
        economy.add(FactionEconomy.Resource.AMMO, 30);
        economy.add(FactionEconomy.Resource.MEDS, 10);
        assert economy.strength() > 0.9;
        try { economy.add(null, 1); throw new AssertionError("Null resource accepted"); }
        catch (IllegalArgumentException expected) { }
        try { economy.consume(FactionEconomy.Resource.FOOD, -1); throw new AssertionError("Negative use accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Faction-economy tests passed: stock, consumption, starvation, expansion, strength and invalid input.");
    }
}
