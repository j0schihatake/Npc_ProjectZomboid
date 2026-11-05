package personal.knox.npc.own;

public final class OwnNpcSpawnerTest {
    public static void run() {
        var population = new OwnNpcPopulation();
        population.add(new OwnNpcPopulation.Group("base", "own", OwnNpcPopulation.Group.Kind.BASE, 200, 0, 0, 0, 0));
        var layer = new OwnNpcLayer();
        assert OwnNpcSpawner.sync(population, layer, 0, 0, 50, 64) == 64;
        assert layer.count() == 64;
        assert OwnNpcSpawner.sync(population, layer, 1000, 1000, 50, 64) == 0;
        assert layer.count() == 0 : "Far group is demoted";
        assert OwnNpcSpawner.sync(population, layer, 10, 0, 50, 10) == 10;
        assert layer.count() == 10;
        try { OwnNpcSpawner.sync(population, layer, 0, 0, -1, 10); throw new AssertionError("Negative radius accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Own-NPC spawner tests passed: near promotes 64, far demotes, per-group limit and invalid radius.");
    }
}
