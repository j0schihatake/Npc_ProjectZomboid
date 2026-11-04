package personal.knox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import me.zed_0xff.zombie_buddy.Exposer;

@Exposer.LuaClass
public final class PersonalKnoxAPI {
    private record Dialogue(int id, String topic, String condition, String text) {}
    private static final PersonalKnoxAPI INSTANCE = new PersonalKnoxAPI();
    private final List<Dialogue> dialogues;
    public static PersonalKnoxAPI getInstance() { return INSTANCE; }

    private PersonalKnoxAPI() {
        var loaded = new ArrayList<Dialogue>();
        try (var stream = PersonalKnoxAPI.class.getResourceAsStream("/personal-knox/dialogues.tsv")) {
            if (stream == null) throw new IllegalStateException("Missing dialogue resource");
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    var parts = line.split("\t", 4);
                    if (parts.length != 4) throw new IllegalArgumentException("Invalid dialogue row");
                    loaded.add(new Dialogue(Integer.parseInt(parts[0]), parts[1], parts[2], parts[3]));
                }
            }
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load PersonalKnox dialogues", error);
        }
        if (loaded.isEmpty()) throw new IllegalStateException("Empty dialogues");
        dialogues = List.copyOf(loaded);
    }

    public String condition(double morale, double relationship, double bravery) {
        return CompanionBrain.condition(morale, relationship, bravery);
    }

    public int chooseDialogue(String topic, double morale, double relationship, double bravery, int roll) {
        var condition = condition(morale, relationship, bravery);
        var matching = new ArrayList<Dialogue>();
        var fallback = new ArrayList<Dialogue>();
        for (var row : dialogues) {
            if (!row.topic.equals(topic)) continue;
            if (row.condition.equals(condition)) matching.add(row);
            if (row.condition.equals("default")) fallback.add(row);
        }
        var pool = matching.isEmpty() ? fallback : matching;
        return pool.isEmpty() ? -1 : pool.get(Math.floorMod(roll, pool.size())).id;
    }

    public String dialogueText(int id) {
        return dialogues.stream().filter(row -> row.id == id).map(Dialogue::text).findFirst().orElse("");
    }

    public String targetLevel(double x, double y, double z, double tx, double ty, double tz, double range) {
        return CompanionBrain.targetLevel(x, y, z, tx, ty, tz, range);
    }

    public boolean allowRetreat(boolean inSquad, boolean groupOrder) {
        return CompanionBrain.allowRetreat(inSquad, groupOrder);
    }

    public String supplyPlan(int food, int water, int medicine, boolean weapon, boolean danger, boolean overloaded) {
        return CompanionBrain.supplyPlan(food, water, medicine, weapon, danger, overloaded);
    }
}
