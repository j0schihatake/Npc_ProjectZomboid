package personal.knox.npc.own;

public final class OwnNpcPromotionTest {
    public static void run() {
        var population = new OwnNpcPopulation();
        population.add(new OwnNpcPopulation.Group("base", "own", OwnNpcPopulation.Group.Kind.BASE, 200, 0, 0, 0, 0));
        var layer = new OwnNpcLayer();
        assert OwnNpcPromotion.promote(population, layer, "base", 100) == OwnNpcLayer.MAX_NPCS;
        assert layer.count() == OwnNpcLayer.MAX_NPCS;
        assert OwnNpcPromotion.promote(population, layer, "base", 10) == 0 : "Active limit holds";
        try { OwnNpcPromotion.promote(population, layer, "missing", 1); throw new AssertionError("Unknown group accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Own-NPC promotion tests passed: 200 strategic -> 64 active, cap and unknown group.");
    }
}
