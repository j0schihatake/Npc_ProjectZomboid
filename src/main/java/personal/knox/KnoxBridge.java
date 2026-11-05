package personal.knox;

import java.lang.reflect.Method;
import java.util.List;

/** Read-only bridge to Knox; no compile-time dependency on its jar. */
public final class KnoxBridge {
    private KnoxBridge() {}

    public static boolean available() { return instance() != null; }

    public static int worldNpcs() { return size("getWorldNpcs"); }
    public static int metaNpcs() { return size("getNpcs"); }
    public static int safehouses() { return size("getSafehouses"); }

    public static int groupMembers(Object player) {
        if (player == null) return 0;
        try {
            Object api = instance();
            if (api == null) return 0;
            Object group = api.getClass().getMethod("getGroup", player.getClass()).invoke(api, player);
            if (group == null) return 0;
            for (var name : new String[]{"getAllNpcGroupMemberObjects", "getAllNpcMembers"}) {
                try {
                    Object value = group.getClass().getMethod(name).invoke(group);
                    if (value instanceof java.util.List<?> list) return list.size();
                } catch (NoSuchMethodException ignored) { }
            }
        } catch (Throwable ignored) { }
        return 0;
    }

    public static String status() {
        var api = instance();
        if (api == null) return "knox=unavailable";
        return "knox=ok world=" + worldNpcs() + " meta=" + metaNpcs() + " safehouses=" + safehouses();
    }

    private static Object instance() {
        try {
            Class<?> type = Class.forName("knox.event.api.KnoxEventNpcAPI");
            return type.getMethod("getInstance").invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int size(String method) {
        try {
            Object api = instance();
            if (api == null) return 0;
            Object value = api.getClass().getMethod(method).invoke(api);
            return value instanceof List<?> list ? list.size() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
