package personal.knox.npc.own;

import java.util.HashSet;

public final class NpcIdentityTest {
    public static void run() {
        var aliases = new HashSet<String>();
        var cultures = new HashSet<String>();
        for (int i = 0; i < 500; i++) {
            var id = "player-squad#" + i;
            var identity = NpcIdentityDatabase.generate(id);
            var again = NpcIdentityDatabase.generate(id);
            assert identity.equals(again) : "Identity must be deterministic for an id";
            assert !identity.displayName().isBlank() && !identity.profession().isBlank();
            assert !identity.skills().isEmpty() : "Profession must grant skills";
            assert identity.skills().values().stream().allMatch(level -> level > 0 && level <= 10);
            aliases.add(identity.displayName());
            cultures.add(identity.culture());
            assert identity.equals(NpcIdentity.parse(identity.csv())) : "Identity must survive CSV round-trip";
        }
        assert aliases.size() > 200 : "Name pool must produce varied names";
        assert cultures.contains("ru") && cultures.stream().anyMatch(c -> !c.equals("ru"))
                : "Both Russian and international names must be present";
        assert NpcIdentityDatabase.cultures().size() >= 5;
        try { NpcIdentity.parse("bad"); throw new AssertionError("Bad identity accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("NPC identity tests passed: deterministic names, RU/international pools, professions, skills and CSV.");
    }
}
