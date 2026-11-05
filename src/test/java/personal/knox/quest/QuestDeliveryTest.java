package personal.knox.quest;

public final class QuestDeliveryTest {
    public static void run() {
        var delivery = new QuestDelivery("Base.Generator", 2);
        assert delivery.remaining() == 2 && !delivery.complete();
        assert delivery.deliver(1) == 1 && delivery.remaining() == 1;
        assert delivery.deliver(5) == 1 && delivery.complete();
        assert delivery.deliver(1) == 0;
        try { new QuestDelivery("", 1); throw new AssertionError("Empty item accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new QuestDelivery("x", 0); throw new AssertionError("Zero count accepted"); }
        catch (IllegalArgumentException expected) { }
        try { delivery.deliver(-1); throw new AssertionError("Negative delivery accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("Quest-delivery tests passed: exact item, partial delivery, completion and invalid data.");
    }
}
