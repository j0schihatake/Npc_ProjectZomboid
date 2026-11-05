package personal.knox.npc.own;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** Java-side body adapter via reflection: no compile-time dependency on Java 25 game classes. */
public final class OwnNpcBodyAdapter {
    private final Map<String, Object> bodies = new HashMap<>();
    private boolean unavailable;

    public int sync(OwnNpcLayer layer) {
        if (layer == null) throw new IllegalArgumentException("Missing layer");
        if (unavailable) return 0;
        try {
            Object cell = currentCell();
            if (cell == null) return 0;
            var seen = new HashSet<String>();
            for (var agent : layer.agents()) {
                seen.add(agent.id());
                Object body = bodies.get(agent.id());
                if (body == null) {
                    body = create(cell, agent.id(), (int) agent.x(), (int) agent.y(), (int) agent.z());
                    if (body == null) continue;
                    bodies.put(agent.id(), body);
                }
                move(body, agent.x(), agent.y(), agent.z(), agent.targetX(), agent.targetY());
            }
            var stale = new HashSet<>(bodies.keySet());
            stale.removeAll(seen);
            for (var id : stale) {
                var body = bodies.remove(id);
                call(body, "removeFromWorld");
            }
            return bodies.size();
        } catch (Throwable error) {
            unavailable = true;
            System.out.println("[PersonalKnox] Own body adapter unavailable: " + error);
            return 0;
        }
    }

    public int count() { return bodies.size(); }

    private static Object currentCell() throws Exception {
        Class<?> world = Class.forName("zombie.iso.IsoWorld");
        Object value = world.getField("instance").get(null);
        if (value == null) return null;
        try {
            return value.getClass().getField("currentCell").get(value);
        } catch (NoSuchFieldException missing) {
            return value.getClass().getMethod("getCell").invoke(value);
        }
    }

    private Object create(Object cell, String id, int x, int y, int z) throws Exception {
        Class<?> playerClass = Class.forName("zombie.characters.IsoPlayer");
        Class<?> factory = Class.forName("zombie.characters.SurvivorFactory");
        Object desc = factory.getMethod("CreateSurvivor").invoke(null);
        Constructor<?> ctor = null;
        for (var candidate : playerClass.getConstructors()) {
            var p = candidate.getParameterTypes();
            if (p.length == 5 && p[0].isInstance(cell) && p[1].isInstance(desc)) { ctor = candidate; break; }
        }
        if (ctor == null) throw new IllegalStateException("No IsoPlayer constructor");
        Object body = ctor.newInstance(cell, desc, x, y, z);
        invoke(body, "setNpc", new Class<?>[]{boolean.class}, true);
        invoke(body, "setSceneCulled", new Class<?>[]{boolean.class}, false);
        invoke(body, "setBlockMovement", new Class<?>[]{boolean.class}, false);
        invoke(body, "setUsername", new Class<?>[]{String.class}, "PK-" + id);
        dress(body, desc);
        return body;
    }

    private static void dress(Object body, Object desc) {
        try {
            body.getClass().getMethod("Dressup", desc.getClass()).invoke(body, desc);
        } catch (Throwable ignored) { }
        try {
            Class<?> playerClass = Class.forName("zombie.characters.IsoPlayer");
            Object player = playerClass.getMethod("getInstance").invoke(null);
            if (player != null) {
                Object playerVisual = player.getClass().getMethod("getHumanVisual").invoke(player);
                Object bodyVisual = body.getClass().getMethod("getHumanVisual").invoke(body);
                if (playerVisual != null && bodyVisual != null) {
                    Class<?> base = Class.forName("zombie.core.skinnedmodel.visual.BaseVisual");
                    bodyVisual.getClass().getMethod("copyFrom", base).invoke(bodyVisual, playerVisual);
                    System.out.println("[PersonalKnox] Own body visual copied from player");
                    return;
                }
            }
        } catch (Throwable error) {
            System.out.println("[PersonalKnox] Own body visual copy failed: " + error);
        }
        for (var name : new String[]{"dressInRandomNonSillyOutfit", "dressInRandomOutfit"}) {
            try {
                body.getClass().getMethod(name).invoke(body);
                System.out.println("[PersonalKnox] Own body dressed via " + name);
                return;
            } catch (Throwable ignored) { }
        }
        System.out.println("[PersonalKnox] Own body dress failed");
    }

    private static void move(Object body, double x, double y, double z, double tx, double ty) {
        double sx = x, sy = y;
        try {
            Object cell = currentCell();
            double bx = (double) body.getClass().getMethod("getX").invoke(body);
            double by = (double) body.getClass().getMethod("getY").invoke(body);
            if (!passable(cell, bx, by, z, x, y)) {
                if (passable(cell, bx, by, z, x, by)) {
                    sy = by;
                } else if (passable(cell, bx, by, z, bx, y)) {
                    sx = bx;
                } else {
                    invoke(body, "setMoving", new Class<?>[]{boolean.class}, false);
                    return;
                }
            }
        } catch (Throwable ignored) { }
        invoke(body, "setX", new Class<?>[]{float.class}, (float) sx);
        invoke(body, "setY", new Class<?>[]{float.class}, (float) sy);
        invoke(body, "setZ", new Class<?>[]{float.class}, (float) z);
        double dx = tx - sx;
        double dy = ty - sy;
        if (Math.hypot(dx, dy) > 0.15) {
            invoke(body, "setDirectionAngle", new Class<?>[]{float.class}, (float) Math.atan2(dy, dx));
            invoke(body, "setMoving", new Class<?>[]{boolean.class}, true);
            invoke(body, "setAnimated", new Class<?>[]{boolean.class}, true);
        } else {
            invoke(body, "setMoving", new Class<?>[]{boolean.class}, false);
        }
    }

    private static boolean passable(Object cell, double x0, double y0, double z0, double x1, double y1) {
        try {
            Class<?> squareClass = Class.forName("zombie.iso.IsoGridSquare");
            Object a = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                    .invoke(cell, (int) x0, (int) y0, (int) z0);
            Object b = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                    .invoke(cell, (int) x1, (int) y1, (int) z0);
            if (a == null || b == null) return false;
            return !(boolean) a.getClass().getMethod("isBlockedTo", squareClass).invoke(a, b);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static void call(Object target, String name) {
        try { target.getClass().getMethod(name).invoke(target); } catch (Throwable ignored) { }
    }

    private static void invoke(Object target, String name, Class<?>[] types, Object... args) {
        try { target.getClass().getMethod(name, types).invoke(target, args); }
        catch (Throwable error) { System.out.println("[PersonalKnox] Own body " + name + ": " + error); }
    }
}
