package personal.knox;

public final class LootBagTest {
    public static void run() {
        var bag = new LootBag("bag-1");
        assert bag.state() == LootBag.State.CARRIED;
        bag.drop(10, 20, 0);
        assert bag.state() == LootBag.State.DROPPED && bag.x() == 10 && bag.y() == 20;
        bag.recover();
        assert bag.state() == LootBag.State.RECOVERED;
        bag.deliver();
        assert bag.state() == LootBag.State.DELIVERED;
        try { bag.drop(0, 0, 0); throw new AssertionError("Delivered bag dropped"); }
        catch (IllegalStateException expected) { }
        var lost = new LootBag("bag-2");
        lost.lose();
        assert lost.state() == LootBag.State.LOST;
        try { lost.recover(); throw new AssertionError("Lost bag recovered"); }
        catch (IllegalStateException expected) { }
        try { new LootBag(""); throw new AssertionError("Empty id accepted"); }
        catch (IllegalArgumentException expected) { }
        var bad = new LootBag("bag-3");
        try { bad.drop(Double.NaN, 0, 0); throw new AssertionError("NaN position accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Loot-bag tests passed: carry, drop, recover, deliver, loss and invalid transitions.");
    }
}
