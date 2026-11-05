package personal.knox.alife;

import java.util.EnumMap;
import java.util.Map;

/** Abstract faction stock: production, consumption, expansion and combat readiness. */
public final class FactionEconomy {
    public enum Resource { FOOD, WATER, MEDS, AMMO, MATERIALS, FUEL }

    private final Map<Resource, Integer> stock = new EnumMap<>(Resource.class);
    private int population;

    public FactionEconomy(int population) {
        if (population < 0) throw new IllegalArgumentException("Invalid population");
        this.population = population;
        for (var resource : Resource.values()) stock.put(resource, 0);
    }

    public int population() { return population; }

    public int get(Resource resource) {
        if (resource == null) throw new IllegalArgumentException("Missing resource");
        return stock.get(resource);
    }

    public void add(Resource resource, int amount) {
        if (resource == null || amount < 0) throw new IllegalArgumentException("Invalid stock add");
        stock.put(resource, Math.min(10_000, get(resource) + amount));
    }

    public int consume(Resource resource, int amount) {
        if (resource == null || amount < 0) throw new IllegalArgumentException("Invalid stock use");
        int taken = Math.min(get(resource), amount);
        stock.put(resource, get(resource) - taken);
        return taken;
    }

    public boolean tick(int foodPerPerson, int waterPerPerson) {
        if (foodPerPerson < 0 || waterPerPerson < 0) throw new IllegalArgumentException("Invalid consumption");
        int foodNeed = population * foodPerPerson;
        int waterNeed = population * waterPerPerson;
        int food = consume(Resource.FOOD, foodNeed);
        int water = consume(Resource.WATER, waterNeed);
        return food < foodNeed || water < waterNeed;
    }

    public boolean canExpand() {
        return population >= 2 && get(Resource.FOOD) >= 50
                && get(Resource.MATERIALS) >= 30 && get(Resource.WATER) >= 30;
    }

    public double strength() {
        double food = Math.min(1, get(Resource.FOOD) / 50.0);
        double ammo = Math.min(1, get(Resource.AMMO) / 30.0);
        double meds = Math.min(1, get(Resource.MEDS) / 10.0);
        return (food + ammo + meds) / 3.0;
    }
}
