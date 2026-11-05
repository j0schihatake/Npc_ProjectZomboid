package personal.knox.quest;

public final class QuestServiceTest {
    public static void run() {
        var service = new QuestService();
        var offered = service.offer("own", Quest.Kind.RESOURCES, "generator", 2);
        assert service.board().offers("own").size() == 1;
        assert service.accept("own", offered.id()) == 0;
        assert service.log().active().size() == 1;
        assert service.complete(offered.id()) > 0;
        assert service.log().completed().size() == 1;
        var second = service.offer("ally", Quest.Kind.SEARCH, "key", 1);
        assert service.decline("ally", second.id()) == 0;
        assert service.log().declined().size() == 1;
        var third = service.offer("ally", Quest.Kind.RESCUE, "survivor", 1);
        service.accept("ally", third.id());
        assert service.fail(third.id()) < 0;
        assert service.log().failed().size() == 1;
        try { service.accept("own", "missing"); throw new AssertionError("Unknown offer accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Quest-service tests passed: offer, accept, complete, decline, fail, journal and unknown offer.");
    }
}
