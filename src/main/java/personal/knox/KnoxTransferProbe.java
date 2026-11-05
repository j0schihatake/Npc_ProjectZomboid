package personal.knox;

import java.lang.reflect.Modifier;

/** Public metadata only. Never constructs an action or invokes an NPC method. */
public final class KnoxTransferProbe {
    private KnoxTransferProbe() {}

    public static String inspect(Object npc) {
        if (npc == null) return "no_npc";
        try {
            var loader = npc.getClass().getClassLoader();
            return inspect(npc,
                    Class.forName("knox.event.npc.MetaNpcCharacter", false, loader),
                    Class.forName("knox.event.npc.actions.ISNpcInventoryTransferAction", false, loader),
                    Class.forName("zombie.inventory.InventoryItem", false, loader),
                    Class.forName("zombie.inventory.ItemContainer", false, loader),
                    Class.forName("knox.event.npc.actions.MetaBaseAction", false, loader));
        } catch (ClassNotFoundException error) {
            return "class_missing";
        } catch (LinkageError error) {
            return "linkage_unavailable";
        } catch (SecurityException error) {
            return "access_denied";
        }
    }

    static String inspect(Object npc, Class<?> actor, Class<?> transfer,
                          Class<?> item, Class<?> container, Class<?> baseAction) {
        if (!actor.isInstance(npc)) return "npc_type_mismatch";
        if (!baseAction.isAssignableFrom(transfer)) return "action_type_mismatch";
        try {
            transfer.getConstructor(actor, int.class, item, container, container);
        } catch (NoSuchMethodException error) {
            return "constructor_missing";
        }
        try {
            var queue = npc.getClass().getMethod("QueueAction", baseAction);
            if (Modifier.isStatic(queue.getModifiers())) return "queue_missing";
        } catch (NoSuchMethodException error) {
            return "queue_missing";
        }
        return "public_api_present";
    }
}
