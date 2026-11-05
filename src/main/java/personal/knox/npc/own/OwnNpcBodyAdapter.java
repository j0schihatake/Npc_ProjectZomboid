package personal.knox.npc.own;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** Java-side body adapter via reflection: no compile-time dependency on Java 25 game classes. */
public final class OwnNpcBodyAdapter {
    private final Map<String, Object> bodies = new HashMap<>();
    private record PathState(double tx, double ty, java.util.List<OwnNpcPathfinder.Point> points, int index) { }
    private boolean unavailable;
    private final Map<String, PathState> pathCache = new HashMap<>();

    public int sync(OwnNpcLayer layer) {
        if (layer == null) throw new IllegalArgumentException("Missing layer");
        if (pathCache.size() > 128) pathCache.clear();
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
                move(agent.id(), body, agent.x(), agent.y(), agent.z(), agent.targetX(), agent.targetY(), layer.targetZ(agent.id()));
            }
            var stale = new HashSet<>(bodies.keySet());
            stale.removeAll(seen);
            for (var id : stale) {
                pathCache.remove(id);
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

    private void move(String id, Object body, double x, double y, double z, double tx, double ty, double targetZ) {
        double sx = x, sy = y;
        try {
            Object cell = currentCell();
            double bx = (double) body.getClass().getMethod("getX").invoke(body);
            double by = (double) body.getClass().getMethod("getY").invoke(body);
            if (Math.abs(targetZ - z) > 0.4) {
                double[] stair = findStairs(cell, bx, by, z);
                if (stair != null) {
                    if (Math.hypot(stair[0] - bx, stair[1] - by) < 0.9) {
                        z = targetZ;
                    } else {
                        tx = stair[0];
                        ty = stair[1];
                        pathCache.remove(id);
                    }
                }
            }
            if (!passable(cell, bx, by, z, x, y)) {
                var step = pathStep(id, cell, bx, by, z, tx, ty);
                if (step != null) {
                    sx = step[0];
                    sy = step[1];
                } else if (passable(cell, bx, by, z, x, by)) {
                    sy = by;
                } else if (passable(cell, bx, by, z, bx, y)) {
                    sx = bx;
                } else {
                    invoke(body, "setMoving", new Class<?>[]{boolean.class}, false);
                    return;
                }
            }
        } catch (Throwable ignored) { }
        if (Math.hypot(tx - sx, ty - sy) < 1.5 && Math.abs(targetZ - z) > 0.4) {
            z = targetZ;
            pathCache.remove(id);
        }
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

    private double[] pathStep(String id, Object cell, double bx, double by, double z, double tx, double ty) {
        var cached = pathCache.get(id);
        if (cached != null && (cached.tx() != tx || cached.ty() != ty)) {
            pathCache.remove(id);
            cached = null;
        }
        if (cached == null || cached.index() >= cached.points().size()) {
            try {
                var world = new OwnNpcTraversalGrid.World() {
                    public boolean passable(int x, int y) {
                        try {
                            return cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                                    .invoke(cell, x, y, (int) z) != null;
                        } catch (Throwable ignored) {
                            return false;
                        }
                    }
                    public boolean canStep(int fx, int fy, int tx2, int ty2) {
                        return passable2(cell, fx, fy, (int) z, tx2, ty2);
                    }
                    public OwnNpcTraversal.Kind traversalAt(int x, int y) { return null; }
                };
                var points = OwnNpcPathfinder.find((int) bx, (int) by, (int) tx, (int) ty,
                        new OwnNpcTraversalGrid(world), 128);
                if (points.size() > 1) {
                    pathCache.put(id, new PathState(tx, ty, points, 1));
                    cached = pathCache.get(id);
                }
            } catch (Throwable ignored) { }
        }
        if (cached != null && cached.index() < cached.points().size()) {
            int next = Math.min(cached.points().size(), cached.index() + 2);
            var point = cached.points().get(next - 1);
            pathCache.put(id, new PathState(cached.tx(), cached.ty(), cached.points(), next));
            return new double[]{point.x(), point.y()};
        }
        return null;
    }

    private static double[] findStairs(Object cell, double bx, double by, double z) {
        try {
            for (int x = (int) bx - 4; x <= (int) bx + 4; x++) {
                for (int y = (int) by - 4; y <= (int) by + 4; y++) {
                    Object square = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                            .invoke(cell, x, y, (int) z);
                    if (square == null) continue;
                    for (var name : new String[]{"HasStairs", "HasStairsBelow", "HasStairTop"}) {
                        try {
                            if ((boolean) square.getClass().getMethod(name).invoke(square)) {
                                return new double[]{x, y};
                            }
                        } catch (Throwable ignored) { }
                    }
                }
            }
        } catch (Throwable ignored) { }
        return null;
    }

    private static boolean passable2(Object cell, int fx, int fy, int z, int tx, int ty) {
        try {
            Class<?> squareClass = Class.forName("zombie.iso.IsoGridSquare");
            Object a = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                    .invoke(cell, fx, fy, z);
            Object b = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                    .invoke(cell, tx, ty, z);
            if (a == null || b == null) return false;
            boolean blocked = (boolean) a.getClass().getMethod("isBlockedTo", squareClass).invoke(a, b);
            if (!blocked) return true;
            openDoor(b);
            return !(boolean) a.getClass().getMethod("isBlockedTo", squareClass).invoke(a, b);
        } catch (Throwable ignored) {
            return false;
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
            boolean blocked = (boolean) a.getClass().getMethod("isBlockedTo", squareClass).invoke(a, b);
            if (!blocked) return true;
            openDoor(b);
            return !(boolean) a.getClass().getMethod("isBlockedTo", squareClass).invoke(a, b);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static void openDoor(Object square) {
        try {
            Object objects = square.getClass().getMethod("getObjects").invoke(square);
            int size = (int) objects.getClass().getMethod("size").invoke(objects);
            Class<?> doorClass = Class.forName("zombie.iso.objects.IsoDoor");
            for (int i = 0; i < size; i++) {
                Object object = objects.getClass().getMethod("get", int.class).invoke(objects, i);
                if (doorClass.isInstance(object)) {
                    object.getClass().getMethod("setOpen", boolean.class).invoke(object, true);
                    return;
                }
            }
        } catch (Throwable ignored) { }
    }

    private static void call(Object target, String name) {
        try { target.getClass().getMethod(name).invoke(target); } catch (Throwable ignored) { }
    }

    private static void invoke(Object target, String name, Class<?>[] types, Object... args) {
        try { target.getClass().getMethod(name, types).invoke(target, args); }
        catch (Throwable error) { System.out.println("[PersonalKnox] Own body " + name + ": " + error); }
    }
}
