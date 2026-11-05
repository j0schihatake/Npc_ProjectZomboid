package personal.knox.quest;

public final class QuestFactoryTest {
    public static void run() {
        var q = QuestFactory.create(Quest.Kind.RESCUE, "own", "survivor-1", 3);
        assert q.state() == Quest.State.OFFERED;
        assert q.id().equals("rescue-own-survivor-1");
        var again = QuestFactory.create(Quest.Kind.RESCUE, "own", "survivor-1", 3);
        assert q.equals(again) : "Deterministic quest id";
        try { QuestFactory.create(null, "own", "x", 1); throw new AssertionError("Null kind accepted"); }
        catch (IllegalArgumentException expected) { }
        try { QuestFactory.create(Quest.Kind.SEARCH, "", "x", 1); throw new AssertionError("Empty giver accepted"); }
        catch (IllegalArgumentException expected) { }
        try { QuestFactory.create(Quest.Kind.SEARCH, "own", "x", 0); throw new AssertionError("Zero stages accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Quest-factory tests passed: deterministic ids, offered state and invalid data.");
    }
}
