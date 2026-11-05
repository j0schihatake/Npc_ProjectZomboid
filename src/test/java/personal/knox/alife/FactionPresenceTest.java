package personal.knox.alife;

public final class FactionPresenceTest {
    public static void run() {
        var alive = new FactionPresence.State("raiders", 3, true, 100);
        var lost = FactionPresence.afterLoss(alive, 200);
        assert lost.members() == 0 && lost.lastActiveMillis() == 200;
        assert !FactionPresence.shouldRespawn(lost, 300, 1000);
        assert FactionPresence.shouldRespawn(lost, 1200, 1000);
        var back = FactionPresence.respawn(lost, 2, 1300);
        assert back.members() == 2 && back.hasBase() && back.lastActiveMillis() == 1300;
        try { new FactionPresence.State("", 1, false, 0); throw new AssertionError("Empty id accepted"); }
        catch (IllegalArgumentException expected) { }
        try { FactionPresence.shouldRespawn(lost, 100, 0); throw new AssertionError("Zero delay accepted"); }
        catch (IllegalArgumentException expected) { }
        try { FactionPresence.respawn(lost, 0, 100); throw new AssertionError("Zero members accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Faction-presence tests passed: loss, delayed respawn, base kept, invalid id/delay/members.");
    }
}
