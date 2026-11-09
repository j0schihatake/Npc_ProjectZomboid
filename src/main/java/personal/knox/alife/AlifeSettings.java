package personal.knox.alife;

import java.util.LinkedHashMap;
import java.util.Map;

/** Twenty A-Life sandbox values; all bounded and readable from Lua. */
public final class AlifeSettings {
    private final Map<String, Integer> values = new LinkedHashMap<>();

    public AlifeSettings() {
        values.put("factionCount", 12);
        values.put("patrolCount", 4);
        values.put("eventFrequency", 60);
        values.put("raidFrequency", 90);
        values.put("ambushChance", 20);
        values.put("checkpointChance", 15);
        values.put("sniperChance", 5);
        values.put("punitiveChance", 10);
        values.put("baseRaidChance", 15);
        values.put("nearRadius", 80);
        values.put("farRadius", 300);
        values.put("budgetMs", 5);
        values.put("maxSquads", 64);
        values.put("memoryDays", 7);
        values.put("warningChance", 35);
        values.put("leaveChance", 20);
        values.put("regroupMorale", 30);
        values.put("woundedPercent", 35);
        values.put("radioRange", 120);
        values.put("diagnostics", 0);
    }

    public int get(String key) { return values.getOrDefault(key, 0); }

    public int set(String key, int value) {
        if (key == null || !values.containsKey(key) || value < 0 || value > 10000)
            throw new IllegalArgumentException("Invalid A-Life setting");
        values.put(key, value);
        return value;
    }

    public String snapshot() {
        var out = new StringBuilder();
        values.forEach((k, v) -> out.append(k).append('=').append(v).append(';'));
        return out.toString();
    }

    public int size() { return values.size(); }
}
