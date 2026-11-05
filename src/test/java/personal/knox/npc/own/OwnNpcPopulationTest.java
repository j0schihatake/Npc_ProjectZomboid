package personal.knox.npc.own;

public final class OwnNpcPopulationTest {
    public static void run() {
        var population = new OwnNpcPopulation();
        population.add(new OwnNpcPopulation.Group("base", "own", OwnNpcPopulation.Group.Kind.BASE, 200, 0, 0, 0, 0));
        population.add(new OwnNpcPopulation.Group("raiders", "raiders", OwnNpcPopulation.Group.Kind.ROAMING, 1000, 100, 100, 0, 0));
        assert population.groupCount() == 2 && population.totalMembers() == 1200;
        assert population.tick(10, 8) == 1 : "Budget limits movements";
        assert population.group("raiders").x() < 100 : "Group moved";
        try { population.add(new OwnNpcPopulation.Group("base", "own", OwnNpcPopulation.Group.Kind.BASE, 1, 0, 0, 0, 0)); throw new AssertionError("Duplicate accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new OwnNpcPopulation.Group("g", "f", OwnNpcPopulation.Group.Kind.BASE, -1, 0, 0, 0, 0); throw new AssertionError("Negative members accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Own-NPC population tests passed: 1200 strategic members, budget, movement and invalid data.");
    }
}
