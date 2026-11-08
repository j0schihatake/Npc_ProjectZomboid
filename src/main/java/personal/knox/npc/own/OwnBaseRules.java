package personal.knox.npc.own;

import java.util.LinkedHashMap;
import java.util.Map;

/** Configurable numeric rules for the base economy. */
public final class OwnBaseRules {
    private final Map<String, Integer> values = new LinkedHashMap<>();

    public OwnBaseRules() {
        values.put("foodCap", 200);
        values.put("waterCap", 200);
        values.put("medsCap", 50);
        values.put("materialsCap", 100);
        values.put("fuelCap", 60);
        values.put("ammoCap", 120);
        values.put("farmYield", 5);
        values.put("cookMeals", 2);
        values.put("waterYield", 5);
        values.put("fishYield", 2);
        values.put("huntYield", 3);
        values.put("scavengeFood", 2);
        values.put("scavengeMeds", 1);
        values.put("moraleGain", 1);
        values.put("moraleLoss", 2);
        values.put("healAmount", 50);
        values.put("buildRate", 4);
        values.put("generatorHoursPerFuel", 2);
        values.put("vehicleKmPerFuel", 5);
        values.put("dailyWater", 1);
    }

    public int get(String key) {
        Integer value = values.get(key);
        return value == null ? 0 : value;
    }

    public int set(String key, int value) {
        if (key == null || !values.containsKey(key) || value < 0 || value > 10000)
            throw new IllegalArgumentException("Invalid rule");
        values.put(key, value);
        return value;
    }

    public String snapshot() {
        var out = new StringBuilder();
        values.forEach((k, v) -> out.append(k).append('=').append(v).append(';'));
        return out.toString();
    }
}
