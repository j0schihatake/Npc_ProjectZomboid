package personal.knox.alife;

public final class DynamicRelationsTest {
    public static void run() {
        var relations = new DynamicRelations();
        assert relations.standing("a", "b") == 0;
        relations.onRequest("a", "b", true);
        assert relations.standing("a", "b") == 15 && relations.standing("b", "a") == 15 : "Symmetric and positive";
        relations.onRequest("a", "b", false);
        assert relations.standing("a", "b") == -5 : "Ignored request hurts";
        relations.onAlliance("a", "b");
        assert relations.standing("a", "b") == 60;
        relations.onBetrayal("a", "b");
        assert relations.standing("a", "b") == 30;
        relations.set("a", "b", 100);
        relations.shift("a", "b", 50);
        assert relations.standing("a", "b") == 100 : "Clamp high";
        relations.set("a", "b", -100);
        relations.shift("a", "b", -50);
        assert relations.standing("a", "b") == -100 : "Clamp low";
        try { relations.standing("a", "a"); throw new AssertionError("Self relation accepted"); }
        catch (IllegalArgumentException expected) { }
        try { relations.set("", "b", 1); throw new AssertionError("Empty faction accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Dynamic-relations tests passed: symmetric, requests, alliance, betrayal, clamps and invalid pairs.");
    }
}
