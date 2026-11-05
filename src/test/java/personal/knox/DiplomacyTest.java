package personal.knox;

public final class DiplomacyTest {
    public static void run() {
        assert Diplomacy.adjust(0, Diplomacy.Action.GIFT) == 10;
        assert Diplomacy.adjust(0, Diplomacy.Action.AID) == 20;
        assert Diplomacy.adjust(0, Diplomacy.Action.TRADE) == 5;
        assert Diplomacy.adjust(0, Diplomacy.Action.BETRAYAL) == -30;
        assert Diplomacy.adjust(0, Diplomacy.Action.ATTACK) == -40;
        assert Diplomacy.adjust(95, Diplomacy.Action.AID) == 100 : "Clamp high";
        assert Diplomacy.adjust(-95, Diplomacy.Action.BETRAYAL) == -100 : "Clamp low";
        assert Diplomacy.stance(50) == Diplomacy.Stance.ALLIED;
        assert Diplomacy.stance(0) == Diplomacy.Stance.NEUTRAL;
        assert Diplomacy.stance(-1) == Diplomacy.Stance.TENSE;
        assert Diplomacy.stance(-40) == Diplomacy.Stance.HOSTILE;
        assert Diplomacy.acceptsAid(0);
        assert !Diplomacy.acceptsAid(-1);
        assert Diplomacy.canJoin(50);
        assert !Diplomacy.canJoin(-40);
        try { Diplomacy.stance(101); throw new AssertionError("Bad standing accepted"); }
        catch (IllegalArgumentException expected) { }
        try { Diplomacy.adjust(0, null); throw new AssertionError("Null action accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Diplomacy tests passed: gifts, aid, trade, betrayal, attack, clamps, stances and aid/join rules.");
    }
}
