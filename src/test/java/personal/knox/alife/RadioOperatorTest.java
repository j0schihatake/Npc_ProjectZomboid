package personal.knox.alife;

public final class RadioOperatorTest {
    public static void run() {
        var op = new RadioOperator("npc1", "base1", 3, true);
        assert op.rangeMeters() == 500;
        assert op.canContact(400) && !op.canContact(600);
        assert op.interceptChance() > 0.5;
        var off = new RadioOperator("npc1", "base1", 5, false);
        assert off.rangeMeters() == 0 && !off.canContact(0) && off.interceptChance() == 0;
        try { new RadioOperator("", "base", 1, true); throw new AssertionError("Empty npc accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new RadioOperator("n", "base", 6, true); throw new AssertionError("Bad skill accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Radio-operator tests passed: range, contact, duty, interception and invalid data.");
    }
}
