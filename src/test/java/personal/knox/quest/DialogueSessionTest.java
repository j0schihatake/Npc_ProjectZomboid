package personal.knox.quest;

public final class DialogueSessionTest {
    public static void run() {
        var dialogue = new DialogueSession("npc1", "own");
        dialogue.say("npc1", "How are you?");
        assert dialogue.history().size() == 1;
        dialogue.setOptions(java.util.List.of(
                new DialogueSession.Option("trade", "Trade", DialogueSession.Option.Kind.TRADE),
                new DialogueSession.Option("leave", "Leave", DialogueSession.Option.Kind.LEAVE)));
        assert dialogue.choose("trade").kind() == DialogueSession.Option.Kind.TRADE;
        assert dialogue.choose("missing") == null;
        dialogue.close();
        assert !dialogue.open();
        try { dialogue.say("npc1", "Hi"); throw new AssertionError("Closed dialogue accepted a line"); }
        catch (IllegalStateException expected) { }
        try { new DialogueSession("", "own"); throw new AssertionError("Empty NPC accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Dialogue tests passed: lines, options, choose, close and invalid input.");
    }
}
