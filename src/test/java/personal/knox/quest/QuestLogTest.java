package personal.knox.quest;

public final class QuestLogTest {
    public static void run() {
        var log = new QuestLog();
        var q = new Quest("q1", Quest.Kind.SEARCH, "own", "key", 2, Quest.State.OFFERED);
        log.offer(q);
        assert log.get("q1").state() == Quest.State.OFFERED;
        log.accept("q1");
        assert log.active().size() == 1;
        log.complete("q1");
        assert log.active().isEmpty() && log.get("q1").state() == Quest.State.COMPLETED;
        var q2 = new Quest("q2", Quest.Kind.RESCUE, "ally", "survivor", 3, Quest.State.OFFERED);
        log.offer(q2);
        log.decline("q2");
        assert log.get("q2").state() == Quest.State.DECLINED;
        var markerLog = new QuestLog();
        var marked = new Quest("qm", Quest.Kind.SPECIAL_ZOMBIE, "own", "key-zombie", 1, Quest.State.OFFERED);
        markerLog.offer(marked);
        markerLog.accept("qm");
        markerLog.setMarker(new QuestMarker("qm", 10, 20, 0, "Key zombie"));
        assert markerLog.marker("qm").label().equals("Key zombie");
        markerLog.complete("qm");
        assert markerLog.marker("qm") == null : "Completed marker removed";
        assert markerLog.completed().size() == 1 && markerLog.active().isEmpty();
        assert markerLog.failed().isEmpty();
        try { log.offer(q); throw new AssertionError("Duplicate quest accepted"); }
        catch (IllegalArgumentException expected) { }
        try { log.accept("missing"); throw new AssertionError("Unknown quest accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Quest-log tests passed: offer, accept, complete, decline, active list, duplicates and unknown ids.");
    }
}
