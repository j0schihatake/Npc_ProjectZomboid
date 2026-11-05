package personal.knox;

public final class KnoxTransferProbeTest {
    private static boolean actionInitialized;
    public static class BaseAction {}
    public static class Item {}
    public static class Container {}
    public static class Actor {
        public void QueueAction(BaseAction action) { throw new AssertionError("Probe queued an action"); }
    }
    public static class ActorSubclass extends Actor {}
    public static class Action extends BaseAction {
        static { actionInitialized = true; }
        public Action(Actor npc, int time, Item item, Container from, Container to) {
            throw new AssertionError("Probe constructed an action");
        }
    }
    public static class WrongConstructor extends BaseAction {
        public WrongConstructor(Actor npc, long time, Item item, Container from, Container to) {}
    }
    public static class NoQueue {}
    public static class NoQueueAction extends BaseAction {
        public NoQueueAction(NoQueue npc, int time, Item item, Container from, Container to) {}
    }
    public static class StaticQueue {
        public static void QueueAction(BaseAction action) { throw new AssertionError("Probe invoked a static method"); }
    }
    public static class StaticQueueAction extends BaseAction {
        public StaticQueueAction(StaticQueue npc, int time, Item item, Container from, Container to) {}
    }

    public static void run() {
        assert KnoxTransferProbe.inspect(null).equals("no_npc");
        assert KnoxTransferProbe.inspect(new Object()).equals("class_missing");
        assert check(new ActorSubclass(), Actor.class, Action.class).equals("public_api_present");
        assert !actionInitialized : "Reading public metadata must not initialize the action class";
        assert check(new Object(), Actor.class, Action.class).equals("npc_type_mismatch");
        assert check(new Actor(), Actor.class, Object.class).equals("action_type_mismatch");
        assert check(new Actor(), Actor.class, WrongConstructor.class).equals("constructor_missing");
        assert check(new NoQueue(), NoQueue.class, NoQueueAction.class).equals("queue_missing");
        assert check(new StaticQueue(), StaticQueue.class, StaticQueueAction.class).equals("queue_missing");
    }

    private static String check(Object npc, Class<?> actor, Class<?> action) {
        return KnoxTransferProbe.inspect(npc, actor, action, Item.class, Container.class, BaseAction.class);
    }
}
