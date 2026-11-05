package personal.knox.quest;

public final class QuestBoardTest {
    public static void run() {
        var board = new QuestBoard();
        var q = new Quest("q1", Quest.Kind.SEARCH, "own", "key", 1, Quest.State.OFFERED);
        board.offer("own", q);
        assert board.offers("own").size() == 1;
        assert board.take("own", "q1") != null;
        assert board.offers("own").isEmpty();
        assert board.take("own", "missing") == null;
        for (int i = 0; i < QuestBoard.MAX_OFFERS; i++) {
            board.offer("ally", new Quest("a" + i, Quest.Kind.RESOURCES, "ally", "x" + i, 1, Quest.State.OFFERED));
        }
        try { board.offer("ally", new Quest("a9", Quest.Kind.RESOURCES, "ally", "x", 1, Quest.State.OFFERED)); throw new AssertionError("Overflow accepted"); }
        catch (IllegalStateException expected) { }
        try { board.offer("", q); throw new AssertionError("Empty faction accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Quest-board tests passed: offers, take, empty, per-faction cap and invalid input.");
    }
}
