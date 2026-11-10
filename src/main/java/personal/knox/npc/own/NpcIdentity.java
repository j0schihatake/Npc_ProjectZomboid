package personal.knox.npc.own;

import java.util.LinkedHashMap;
import java.util.Map;

/** Persistent identity of an own NPC: name, culture, profession and starting skills. */
public record NpcIdentity(String culture, String gender, String forename, String surname,
                          String professionKey, String profession, Map<String, Integer> skills) {
    public NpcIdentity {
        if (culture == null || culture.isBlank()) throw new IllegalArgumentException("Missing culture");
        if (gender == null || gender.isBlank()) throw new IllegalArgumentException("Missing gender");
        if (forename == null || forename.isBlank() || surname == null || surname.isBlank())
            throw new IllegalArgumentException("Missing name");
        if (professionKey == null || professionKey.isBlank() || profession == null || profession.isBlank())
            throw new IllegalArgumentException("Missing profession");
        skills = skills == null ? Map.of() : Map.copyOf(skills);
        for (var entry : skills.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) throw new IllegalArgumentException("Bad skill");
            if (entry.getValue() == null || entry.getValue() < 0 || entry.getValue() > 10)
                throw new IllegalArgumentException("Bad skill level");
        }
    }

    public String displayName() { return forename + " " + surname; }

    public String summary() {
        var text = new StringBuilder(displayName()).append(" [").append(profession).append("]");
        if (!skills.isEmpty()) {
            text.append(" ");
            boolean first = true;
            for (var entry : skills.entrySet()) {
                if (!first) text.append(',');
                text.append(entry.getKey()).append(':').append(entry.getValue());
                first = false;
            }
        }
        return text.toString();
    }

    public String csv() {
        var skillText = new StringBuilder();
        for (var entry : skills.entrySet()) {
            if (skillText.length() > 0) skillText.append(';');
            skillText.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return culture + "|" + gender + "|" + forename + "|" + surname + "|"
                + professionKey + "|" + profession + "|" + skillText;
    }

    public static NpcIdentity parse(String csv) {
        if (csv == null) throw new IllegalArgumentException("Missing identity");
        var parts = csv.split("\\|", -1);
        if (parts.length != 7) throw new IllegalArgumentException("Bad identity row");
        var skills = new LinkedHashMap<String, Integer>();
        if (!parts[6].isBlank()) {
            for (var token : parts[6].split(";")) {
                var pair = token.split(":", 2);
                if (pair.length != 2) throw new IllegalArgumentException("Bad skill token");
                skills.put(pair[0], Integer.parseInt(pair[1]));
            }
        }
        return new NpcIdentity(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], skills);
    }
}
