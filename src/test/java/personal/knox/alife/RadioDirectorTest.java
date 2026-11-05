package personal.knox.alife;

public final class RadioDirectorTest {
    public static void run() {
        var enemy = RadioDirector.contact("raiders", -30, 0, 10);
        assert enemy.stance() == RadioContact.Stance.ENEMY && enemy.kind() == RadioContact.Kind.WARNING;
        var neutral = RadioDirector.contact("survivors", 0, 0, 10);
        assert neutral.stance() == RadioContact.Stance.NEUTRAL && neutral.kind() == RadioContact.Kind.QUESTION;
        var ally = RadioDirector.contact("ally", 50, 0, 10);
        assert ally.stance() == RadioContact.Stance.ALLY && ally.kind() == RadioContact.Kind.STORY;
        var allyChat = RadioDirector.contact("ally", 50, 90, 10);
        assert allyChat.stance() == RadioContact.Stance.ALLY && !allyChat.text().isBlank();
        try { RadioDirector.contact("g", 101, 0, 0); throw new AssertionError("Bad relation accepted"); }
        catch (IllegalArgumentException expected) { }
        try { RadioDirector.contact("g", 0, 100, 0); throw new AssertionError("Bad roll accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Radio-director tests passed: enemy warning, neutral question, ally chat/joke/story and invalid data.");
    }
}
