package personal.knox.npc.own;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** Java-side body adapter via reflection: no compile-time dependency on Java 25 game classes. */
public final class OwnNpcBodyAdapter {
    private final Map<String, Object> bodies = new HashMap<>();
    private final Map<String, NpcIdentity> identities = new HashMap<>();
    private record PathState(double tx, double ty, java.util.List<OwnNpcPathfinder.Point> points, int index) { }
    private boolean unavailable;
    private boolean matchPlayerClothes;
    public void setMatchPlayerClothes(boolean value) { matchPlayerClothes = value; }

    private final Map<String, PathState> pathCache = new HashMap<>();
    private final Map<String, Object> lootObjects = new HashMap<>();
    private final Map<String, double[]> lastAim = new HashMap<>();
    private final Map<String, String> holstered = new HashMap<>();
    private final Map<String, String> pendingEquipment = new HashMap<>();
    private final Map<String, String> lastWeapon = new HashMap<>();
    private volatile String startingWeapon = "melee";
    private volatile int factionRangedChance = 40;
    private volatile int factionMeleeChance = 40;
    private volatile boolean groupAlert = false;
    private final Map<String, Long> lastCare = new java.util.concurrent.ConcurrentHashMap<>();

    private long lastMoveLog;
    private long lastSyncLog;
    private int moveBudget = 8;
    private int moveSkip;
    private int syncBudgetMs = 8;
    private int syncCursor;
    private boolean preferRussianNames = false;
    private final Map<String, DismantleTask> dismantles = new HashMap<>();
    private record DismantleTask(OwnNpcDismantleJob job, Object object, double x, double y) { }

    public int sync(OwnNpcLayer layer) { return syncInternal(layer, false); }
    public int syncBudgeted(OwnNpcLayer layer) { return syncInternal(layer, true); }

    private int syncInternal(OwnNpcLayer layer, boolean budgeted) {
        if (layer == null) throw new IllegalArgumentException("Missing layer");
        if (pathCache.size() > 128) pathCache.clear();
        if (unavailable) return 0;
        long syncStarted = System.nanoTime();
        long moveNs = 0;
        try {
            Object cell = currentCell();
            if (cell == null) return 0;
            var seen = new HashSet<String>();
            double[] playerPos = playerPosition();
            var zombies = playerPos == null ? zombieSnapshot(cell)
                    : zombieSnapshotNear(cell, playerPos[0], playerPos[1], 40);
            if (layer.alarm() > 0) {
                groupAlert = true;
                try { layer.guardBase(); } catch (Throwable ignored) { }
            }
            var agents = layer.agents();
            for (var agent : agents) {
                seen.add(agent.id());
                if (bodies.get(agent.id()) == null) {
                    try {
                        Object created = create(cell, agent.id(), (int) agent.x(), (int) agent.y(),
                                (int) agent.z(), agent.factionId());
                        if (created != null) bodies.put(agent.id(), created);
                    } catch (Throwable spawnError) {
                        System.out.println("[PersonalKnox] Own body spawn skipped " + agent.id()
                                + ": " + spawnError.getMessage());
                    }
                }
            }
            int totalAgents = agents.size();
            int startAgent = totalAgents == 0 ? 0 : syncCursor % totalAgents;
            int processed = 0;
            long budgetNanos = syncBudgetMs * 1_000_000L;
            for (int k = 0; k < totalAgents; k++) {
                if (budgeted && processed > 0 && System.nanoTime() - syncStarted >= budgetNanos) break;
                var agent = agents.get((startAgent + k) % totalAgents);
                int agentIndex = k;
                processed++;
                Object body = bodies.get(agent.id());
                if (body != null) {
                    rememberWeapon(agent.id(), body);
                    syncHealth(layer, agent.id(), body);
                    boolean threatened = groupAlert || lastAim.containsKey(agent.id());
                    if (agent.state().bleeding() || agent.state().health() < 99) {
                        autoCare(agent.id(), body, layer);
                    }
                    if (threatened) {
                        attack(agent.id(), body, zombies);
                        autoWeapon(agent.id(), true);
                        retreat(layer, agent.id(), body, agent);
                    }
                    if (agent.state().hunger() >= 0.6 || agent.state().thirst() >= 0.6) {
                        autoConsume(layer, agent.id(), body, agent);
                    }
                }
                if (dismantles.containsKey(agent.id())) {
                    String state = tickDismantle(agent.id(), 2.0);
                    if (state.startsWith("DONE:")) {
                        System.out.println("[PersonalKnox][own-dismantle] " + agent.id() + " " + state);
                    }
                }
                if (layer.agent(agent.id()) == null) {
                    var dead = bodies.remove(agent.id());
                    if (dead != null) call(dead, "removeFromWorld");
                    pathCache.remove(agent.id());
                    dismantles.remove(agent.id());
                    continue;
                }
                if (body == null) continue;
                if (layer.orderOf(agent.id()) == OwnNpcOrder.LOOT) {
                    double[] loot = findLoot(cell, agent.x(), agent.y(), agent.z(), agent.id());
                    if (loot != null) {
                        layer.order(agent.id(), loot[0], loot[1]);
                        if (Math.hypot(agent.x() - loot[0], agent.y() - loot[1]) < 1.8) loot(agent.id(), body, layer);
                    }
                }
                if (agent.id().startsWith("own-faction") && layer.orderOf(agent.id()) == OwnNpcOrder.GUARD
                        && Math.hypot(agent.targetX() - agent.x(), agent.targetY() - agent.y()) < 1.2) {
                    double nx = agent.x() + java.util.concurrent.ThreadLocalRandom.current().nextDouble(-6, 6);
                    double ny = agent.y() + java.util.concurrent.ThreadLocalRandom.current().nextDouble(-6, 6);
                    layer.order(agent.id(), OwnNpcOrder.GUARD, nx, ny);
                }
                double tx = agent.targetX(), ty = agent.targetY();
                if (layer.orderOf(agent.id()) == OwnNpcOrder.FOLLOW) {
                    var group = layer.agents();
                    int index = 0;
                    for (int i = 0; i < group.size(); i++) if (group.get(i).id().equals(agent.id())) { index = i; break; }
                    double angle = group.isEmpty() ? 0 : (Math.PI * 2 * index / group.size());
                    tx += Math.cos(angle) * 1.4;
                    ty += Math.sin(angle) * 1.4;
                }
                double targetZ = layer.targetZ(agent.id());
                if ((Math.hypot(tx - agent.x(), ty - agent.y()) > 0.05 || Math.abs(targetZ - agent.z()) > 0.4)
                        && agentIndex >= moveSkip && agentIndex < moveSkip + moveBudget) {
                    long moveStarted = System.nanoTime();
                    move(agent.id(), body, agent.x(), agent.y(), agent.z(), tx, ty, targetZ);
                    moveNs += System.nanoTime() - moveStarted;
                }
            }
            if (totalAgents > 0) syncCursor = (startAgent + processed) % totalAgents;
            if (processed > 0) moveSkip = (moveSkip + Math.min(moveBudget, processed)) % processed;
            boolean anyThreat = !lastAim.isEmpty();
            if (anyThreat && !groupAlert) {
                groupAlert = true;
                int drawn = 0;
                String speaker = null;
                for (var agent : layer.agents()) {
                    if (draw(agent.id()).startsWith("drawn ")) drawn++;
                    if (speaker == null) speaker = agent.id();
                }
                String phrase = OwnNpcAlert.phrase(java.util.concurrent.ThreadLocalRandom.current().nextInt());
                if (speaker != null) speak(speaker, phrase);
                System.out.println("[PersonalKnox][own-alert] group weapon drawn=" + drawn + " phrase=" + phrase);
            } else if (!anyThreat) {
                groupAlert = false;
            }
            if (System.currentTimeMillis() - lastMoveLog >= 10_000L) {
                lastMoveLog = System.currentTimeMillis();
                for (var agent : layer.agents()) {
                    var body = bodies.get(agent.id());
                    if (body == null) continue;
                    System.out.println("[PersonalKnox][own-render] " + inspect(agent.id()));
                    try {
                        System.out.println("[PersonalKnox][own-body-move] " + agent.id()
                                + " body=" + body.getClass().getMethod("getX").invoke(body) + ","
                                + body.getClass().getMethod("getY").invoke(body)
                                + " agent=" + String.format(java.util.Locale.ROOT, "%.1f,%.1f", agent.x(), agent.y())
                                + " target=" + String.format(java.util.Locale.ROOT, "%.1f,%.1f", agent.targetX(), agent.targetY()));
                    } catch (Throwable ignored) { }
                    break;
                }
            }
            var stale = new HashSet<>(bodies.keySet());
            stale.removeAll(seen);
            for (var id : stale) {
                pathCache.remove(id);
                dismantles.remove(id);
                var body = bodies.remove(id);
                call(body, "removeFromWorld");
            }
            long elapsedMs = (System.nanoTime() - syncStarted) / 1_000_000L;
            if (elapsedMs >= 20 || System.currentTimeMillis() - lastSyncLog >= 60_000L) {
                lastSyncLog = System.currentTimeMillis();
                System.out.println("[PersonalKnox][perf] bodySyncMs=" + elapsedMs
                        + " moveMs=" + (moveNs / 1_000_000L)
                        + " processed=" + processed
                        + " bodies=" + bodies.size());
            }
            return bodies.size();
        } catch (Throwable error) {
            unavailable = true;
            System.out.println("[PersonalKnox] Own body adapter unavailable: " + error);
            return 0;
        }
    }

    public int count() { return bodies.size(); }
    public void setMoveBudget(int value) { moveBudget = Math.max(1, Math.min(64, value)); }
    public void setSyncBudgetMs(int value) { syncBudgetMs = Math.max(2, Math.min(50, value)); }
    public void setPreferRussianNames(boolean value) { preferRussianNames = value; }
    public String nearbyObstacle(String id) {
        Object body = bodies.get(id);
        if (body == null) return "";
        try {
            Object square = body.getClass().getMethod("getCurrentSquare").invoke(body);
            if (square == null) return "";
            Object objects = square.getClass().getMethod("getObjects").invoke(square);
            int size = (int) objects.getClass().getMethod("size").invoke(objects);
            for (int i = 0; i < size; i++) {
                Object object = objects.getClass().getMethod("get", int.class).invoke(objects, i);
                if (object == null) continue;
                try { return OwnNpcToolUse.classify(object.getClass().getName()).name(); }
                catch (IllegalArgumentException ignored) { }
            }
        } catch (Throwable ignored) { }
        return "";
    }

    public String plan(String id, String obstacleName) {
        try {
            var obstacle = OwnNpcToolUse.Obstacle.valueOf(obstacleName);
            var tools = tools(id);
            if (!OwnNpcToolUse.canDismantle(tools, obstacle)) return "";
            var plan = OwnNpcToolUse.plan(tools, obstacle, woodwork(id));
            return String.format(java.util.Locale.ROOT, "%d|%.1f|%.1f", plan.seconds(), plan.noise(), plan.wear());
        } catch (Throwable ignored) {
            return "";
        }
    }

    public String position(String id) {
        Object body = bodies.get(id);
        if (body == null) return "";
        try {
            return String.format(java.util.Locale.ROOT, "x=%.2f y=%.2f",
                    ((Number) body.getClass().getMethod("getX").invoke(body)).doubleValue(),
                    ((Number) body.getClass().getMethod("getY").invoke(body)).doubleValue());
        } catch (Throwable error) { return ""; }
    }

    public String debugFindObstacle(String id) {
        Object body = bodies.get(id);
        if (body == null) return "";
        try {
            Object cell = currentCell();
            double bx = ((Number) body.getClass().getMethod("getX").invoke(body)).doubleValue();
            double by = ((Number) body.getClass().getMethod("getY").invoke(body)).doubleValue();
            double bz = ((Number) body.getClass().getMethod("getZ").invoke(body)).doubleValue();
            var tools = tools(id);
            for (int dx = -6; dx <= 6; dx++) for (int dy = -6; dy <= 6; dy++) {
                Object square = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                        .invoke(cell, (int) bx + dx, (int) by + dy, (int) bz);
                if (square == null) continue;
                Object objects = square.getClass().getMethod("getObjects").invoke(square);
                int size = (int) objects.getClass().getMethod("size").invoke(objects);
                for (int i = 0; i < size; i++) {
                    Object object = objects.getClass().getMethod("get", int.class).invoke(objects, i);
                    if (object == null) continue;
                    OwnNpcToolUse.Obstacle obstacle;
                    try { obstacle = OwnNpcToolUse.classify(object.getClass().getName()); }
                    catch (IllegalArgumentException unknown) { continue; }
                    if (!OwnNpcToolUse.canDismantle(tools, obstacle)) continue;
                    body.getClass().getMethod("setX", float.class).invoke(body, (float) ((int) bx + dx) + 0.5f);
                    body.getClass().getMethod("setY", float.class).invoke(body, (float) ((int) by + dy) + 0.5f);
                    body.getClass().getMethod("setCurrentSquare", Class.forName("zombie.iso.IsoGridSquare"))
                            .invoke(body, square);
                    return obstacle.name();
                }
            }
        } catch (Throwable ignored) { }
        return "";
    }

    public String startDismantle(String id) {
        return startDismantle(id, Double.NaN, Double.NaN);
    }

    public String startDismantle(String id, double targetX, double targetY) {
        Object body = bodies.get(id);
        if (body == null) return "";
        try {
            Object square = null;
            Object cell = currentCell();
            if (cell != null && Double.isFinite(targetX + targetY)) {
                try {
                    square = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                            .invoke(cell, (int) targetX, (int) targetY,
                                    (int) ((Number) body.getClass().getMethod("getZ").invoke(body)).doubleValue());
                } catch (Throwable ignored) { }
            }
            if (square == null) square = body.getClass().getMethod("getCurrentSquare").invoke(body);
            if (square == null) return "";
            Object objects = square.getClass().getMethod("getObjects").invoke(square);
            int size = (int) objects.getClass().getMethod("size").invoke(objects);
            var tools = tools(id);
            for (int i = 0; i < size; i++) {
                Object object = objects.getClass().getMethod("get", int.class).invoke(objects, i);
                if (object == null) continue;
                OwnNpcToolUse.Obstacle obstacle;
                try { obstacle = OwnNpcToolUse.classify(object.getClass().getName()); }
                catch (IllegalArgumentException unknown) { continue; }
                if (!OwnNpcToolUse.canDismantle(tools, obstacle)) continue;
                if (obstacle == OwnNpcToolUse.Obstacle.DOOR) {
                    try { object.getClass().getMethod("setOpen", boolean.class).invoke(object, true); }
                    catch (Throwable ignored) { call(object, "removeFromWorld"); }
                    return "DOOR_OPEN";
                }
                double ox = ((Number) object.getClass().getMethod("getX").invoke(object)).doubleValue();
                double oy = ((Number) object.getClass().getMethod("getY").invoke(object)).doubleValue();
                dismantles.put(id, new DismantleTask(OwnNpcDismantleJob.start(tools, obstacle, woodwork(id)), object, ox, oy));
                return obstacle.name();
            }
        } catch (Throwable ignored) { }
        return "";
    }

    public String tickDismantle(String id, double seconds) {
        var task = dismantles.get(id);
        if (task == null) return "";
        try {
            Object body = bodies.get(id);
            if (body == null) { dismantles.remove(id); return ""; }
            double bx = ((Number) body.getClass().getMethod("getX").invoke(body)).doubleValue();
            double by = ((Number) body.getClass().getMethod("getY").invoke(body)).doubleValue();
            if (Math.hypot(bx - task.x(), by - task.y()) > 2.5) {
                dismantles.remove(id);
                return "CANCELLED";
            }
            task.job().tick(seconds);
            if (task.job().done()) {
                call(task.object(), "removeFromWorld");
                dismantles.remove(id);
                return "DONE:" + task.job().obstacle().name();
            }
            return String.format(java.util.Locale.ROOT, "%.1f", task.job().remaining());
        } catch (Throwable ignored) {
            return "";
        }
    }

    public String dismantleNearby(String id) {
        Object body = bodies.get(id);
        if (body == null) return "";
        try {
            Object square = body.getClass().getMethod("getCurrentSquare").invoke(body);
            if (square == null) return "";
            Object objects = square.getClass().getMethod("getObjects").invoke(square);
            int size = (int) objects.getClass().getMethod("size").invoke(objects);
            var tools = tools(id);
            for (int i = 0; i < size; i++) {
                Object object = objects.getClass().getMethod("get", int.class).invoke(objects, i);
                if (object == null) continue;
                OwnNpcToolUse.Obstacle obstacle;
                try {
                    obstacle = OwnNpcToolUse.classify(object.getClass().getName());
                } catch (IllegalArgumentException unknown) {
                    continue;
                }
                if (!OwnNpcToolUse.canDismantle(tools, obstacle)) continue;
                if (obstacle == OwnNpcToolUse.Obstacle.DOOR) {
                    try { object.getClass().getMethod("setOpen", boolean.class).invoke(object, true); }
                    catch (Throwable ignored) { call(object, "removeFromWorld"); }
                    return "DOOR_OPEN";
                }
                call(object, "removeFromWorld");
                return obstacle.name();
            }
        } catch (Throwable ignored) { }
        return "";
    }

    private int woodwork(String id) {
        Object body = bodies.get(id);
        if (body == null) return 0;
        try {
            Class<?> perks = Class.forName("zombie.characters.skills.PerkFactory$Perks");
            Object woodwork = perks.getField("Woodwork").get(null);
            return ((Number) body.getClass().getMethod("getPerkLevel", woodwork.getClass()).invoke(body, woodwork)).intValue();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public java.util.Set<OwnNpcToolUse.Tool> tools(String id) {
        Object body = bodies.get(id);
        if (body == null) return java.util.Set.of();
        try {
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            Object items = inventory.getClass().getMethod("getItems").invoke(inventory);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            var types = new java.util.ArrayList<String>();
            for (int i = 0; i < size; i++) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                try {
                    types.add(String.valueOf(item.getClass().getMethod("getFullType").invoke(item)));
                } catch (NoSuchMethodException missing) {
                    types.add(String.valueOf(item.getClass().getMethod("getType").invoke(item)));
                }
            }
            return OwnNpcToolUse.fromItems(types);
        } catch (Throwable ignored) {
            return java.util.Set.of();
        }
    }


    /** Bounded search at spawn time only, never a per-frame world scan. */
    public static double[] spawnPosition(Object player) {
        return spawnPosition(player, 0);
    }

    public static double[] spawnPosition(Object player, int offset) {
        if(player==null || offset < 0) throw new IllegalArgumentException("Missing player or offset");
        try {
            Object cell=currentCell();
            int x=(int)Math.floor(((Number)player.getClass().getMethod("getX").invoke(player)).doubleValue());
            int y=(int)Math.floor(((Number)player.getClass().getMethod("getY").invoke(player)).doubleValue());
            int z=(int)Math.floor(((Number)player.getClass().getMethod("getZ").invoke(player)).doubleValue());
            var squareAt=cell.getClass().getMethod("getGridSquare",int.class,int.class,int.class);
            int free = 0;
            for(int radius=1;radius<=4;radius++) for(int dx=-radius;dx<=radius;dx++) for(int dy=-radius;dy<=radius;dy++) {
                if(Math.max(Math.abs(dx),Math.abs(dy))!=radius) continue;
                Object square=squareAt.invoke(cell,x+dx,y+dy,z);
                if(square!=null && (boolean)square.getClass().getMethod("isFree",boolean.class).invoke(square,false)) {
                    if (free++ >= offset) return new double[]{x+dx+0.5,y+dy+0.5,z};
                }
            }
            throw new IllegalStateException("No free loaded square near player");
        } catch(ReflectiveOperationException error) { throw new IllegalStateException("Cannot select NPC spawn square",error); }
    }

    public boolean ready(String id) {
        Object body=bodies.get(id);
        if(body==null) return false;
        try {
            Object cell=currentCell();
            Object square=body.getClass().getMethod("getCurrentSquare").invoke(body);
            Object player=Class.forName("zombie.characters.IsoPlayer").getMethod("getInstance").invoke(null);
            return square!=null && body!=player
                    && ((java.util.Collection<?>)square.getClass().getMethod("getMovingObjects").invoke(square)).contains(body)
                    && (boolean)body.getClass().getMethod("hasActiveModel").invoke(body)
                    && !(boolean)body.getClass().getMethod("isDead").invoke(body);
        } catch(Exception error) { return false; }
    }

    public String worn(String id) {
        Object body = bodies.get(id);
        if (body == null) return "";
        try {
            Object worn = body.getClass().getMethod("getWornItems").invoke(body);
            int size = (int) worn.getClass().getMethod("size").invoke(worn);
            var names = new StringBuilder();
            for (int i = 0; i < size; i++) {
                Object entry = worn.getClass().getMethod("get", int.class).invoke(worn, i);
                Object item = entry.getClass().getMethod("getItem").invoke(entry);
                if (item == null) continue;
                if (names.length() > 0) names.append(',');
                names.append(item.getClass().getMethod("getType").invoke(item));
            }
            return names.toString();
        } catch (Exception error) { return ""; }
    }

    public Object body(String id) { return bodies.get(id); }

    public void debugAttack(String id) {
        Object body = bodies.get(id);
        if (body == null) return;
        try { attack(id, body, zombieSnapshot(currentCell())); } catch (Throwable ignored) { }
    }

    public String inspect(String id) {
        Object body=bodies.get(id);
        if(body==null) return "id="+id+" body=missing";
        try {
            Object cell=currentCell();Object square=body.getClass().getMethod("getCurrentSquare").invoke(body);
            return "id="+id+" ready="+ready(id)
                    +" registered="+((java.util.Collection<?>)cell.getClass().getMethod("getObjectList").invoke(cell)).contains(body)
                    +" square="+(square!=null)
                    +" squareMember="+(square!=null && ((java.util.Collection<?>)square.getClass().getMethod("getMovingObjects").invoke(square)).contains(body))
                    +" bodyType="+body.getClass().getName()
                    +" model="+body.getClass().getMethod("hasActiveModel").invoke(body)
                    +" render="+body.getClass().getMethod("getDoRender").invoke(body)
                    +" culled="+body.getClass().getMethod("isSceneCulled").invoke(body)
                    +" spriteInvisible="+body.getClass().getMethod("isSpriteInvisible").invoke(body)
                    +" alpha="+body.getClass().getMethod("getAlpha",int.class).invoke(body,0)
                    +" targetAlpha="+body.getClass().getMethod("getTargetAlpha",int.class).invoke(body,0)
                    +" angle="+body.getClass().getMethod("getDirectionAngle").invoke(body)
                    +" bodyXYZ="+body.getClass().getMethod("getX").invoke(body)+","+body.getClass().getMethod("getY").invoke(body)+","+body.getClass().getMethod("getZ").invoke(body);
        } catch(Exception error) { return "id="+id+" inspect_error="+error.getClass().getSimpleName(); }
    }

    private static Object currentCell() throws ReflectiveOperationException {
        Class<?> world = Class.forName("zombie.iso.IsoWorld");
        Object value = world.getField("instance").get(null);
        if (value == null) return null;
        try {
            return value.getClass().getField("currentCell").get(value);
        } catch (NoSuchFieldException missing) {
            return value.getClass().getMethod("getCell").invoke(value);
        }
    }

    public String consumeBandage(String id) {
        Object body = bodies.get(id);
        if (body == null) return "no-body";
        try {
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            Object items = inventory.getClass().getMethod("getItems").invoke(inventory);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            for (int i = 0; i < size; i++) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                String type = String.valueOf(item.getClass().getMethod("getFullType").invoke(item));
                if (!type.contains("Bandage")) continue;
                inventory.getClass().getMethod("Remove", Class.forName("zombie.inventory.InventoryItem"))
                        .invoke(inventory, item);
                return "bandaged " + type;
            }
            return "no-bandage";
        } catch (Throwable error) { return "bandage-error=" + error.getClass().getSimpleName(); }
    }

    public String supplyState(String id, OwnNpcLayer layer) {
        Object body = bodies.get(id);
        if (body == null) return "no-body";
        var agent = layer.agent(id);
        if (agent == null) return "no-agent";
        boolean food = false, drink = false;
        try {
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            Object items = inventory.getClass().getMethod("getItems").invoke(inventory);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            for (int i = 0; i < size; i++) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                String category = String.valueOf(item.getClass().getMethod("getCategory").invoke(item));
                String type = String.valueOf(item.getClass().getMethod("getFullType").invoke(item));
                if ("Food".equalsIgnoreCase(category)) food = true;
                if (type.contains("Water") || type.contains("Pop")
                        || "Drink".equalsIgnoreCase(category) || "Water".equalsIgnoreCase(category)
                        || "WaterContainer".equalsIgnoreCase(category)) drink = true;
            }
        } catch (Throwable ignored) { }
        if (agent.state().hunger() >= 0.6 && !food) return layer.supplies(id) > 0 ? "food-from-supplies" : "need-food";
        if (agent.state().thirst() >= 0.6 && !drink) return layer.supplies(id) > 0 ? "water-from-supplies" : "need-water";
        return "ok";
    }

    public String consumeDrink(String id) {
        Object body = bodies.get(id);
        if (body == null) return "no-body";
        try {
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            Object items = inventory.getClass().getMethod("getItems").invoke(inventory);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            for (int i = 0; i < size; i++) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                String category = String.valueOf(item.getClass().getMethod("getCategory").invoke(item));
                String type = String.valueOf(item.getClass().getMethod("getFullType").invoke(item));
                boolean drink = type.contains("Water") || type.contains("Pop")
                        || "Drink".equalsIgnoreCase(category) || "Water".equalsIgnoreCase(category)
                        || "Drinkable".equalsIgnoreCase(category) || "WaterContainer".equalsIgnoreCase(category);
                if (!drink) {
                    try {
                        double thirst = ((Number) item.getClass().getMethod("getThirstChange").invoke(item)).doubleValue();
                        drink = thirst < 0;
                    } catch (Throwable ignored) { }
                }
                if (!drink) continue;
                inventory.getClass().getMethod("Remove", Class.forName("zombie.inventory.InventoryItem"))
                        .invoke(inventory, item);
                return "drank " + type;
            }
            return "no-drink";
        } catch (Throwable error) { return "drink-error=" + error.getClass().getSimpleName(); }
    }

    public String consumeFood(String id) {
        Object body = bodies.get(id);
        if (body == null) return "no-body";
        try {
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            Object items = inventory.getClass().getMethod("getItems").invoke(inventory);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            for (int i = 0; i < size; i++) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                String category = String.valueOf(item.getClass().getMethod("getCategory").invoke(item));
                if (!"Food".equalsIgnoreCase(category)) continue;
                String type = String.valueOf(item.getClass().getMethod("getFullType").invoke(item));
                inventory.getClass().getMethod("Remove", Class.forName("zombie.inventory.InventoryItem"))
                        .invoke(inventory, item);
                return "ate " + type;
            }
            return "no-food";
        } catch (Throwable error) { return "eat-error=" + error.getClass().getSimpleName(); }
    }

    public String inventorySummary(String id) {
        Object body = bodies.get(id);
        if (body == null) return "";
        try {
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            Object items = inventory.getClass().getMethod("getItems").invoke(inventory);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            var counts = new java.util.LinkedHashMap<String, Integer>();
            for (int i = 0; i < size && counts.size() < 16; i++) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                String type = String.valueOf(item.getClass().getMethod("getFullType").invoke(item));
                counts.merge(type, 1, Integer::sum);
            }
            var out = new StringBuilder();
            counts.forEach((type, count) -> out.append(type).append('x').append(count).append(';'));
            return out.toString();
        } catch (Throwable error) { return ""; }
    }

    public int armedCount() {
        int armed = 0;
        for (var id : bodies.keySet()) {
            try {
                Object item = bodies.get(id).getClass().getMethod("getPrimaryHandItem").invoke(bodies.get(id));
                if (item != null) armed++;
            } catch (Throwable ignored) { }
        }
        return armed;
    }

    public void restoreEquipment(String text) {
        if (text == null || text.isBlank()) return;
        for (String row : text.split(";")) {
            int eq = row.indexOf('=');
            if (eq <= 0 || eq + 3 > row.length()) continue;
            String id = row.substring(0, eq);
            String marker = row.substring(eq + 1, eq + 3);
            String type = row.substring(eq + 3);
            if (!"P:".equals(marker) && !"H:".equals(marker)) continue;
            pendingEquipment.put(id, marker + type);
            Object body = bodies.get(id);
            if (body != null) applyEquipment(body, marker + type);
        }
    }

    public String lastWeapon(String id) { return lastWeapon.getOrDefault(id, ""); }

    private void autoCare(String id, Object body, OwnNpcLayer layer) {
        long now = System.currentTimeMillis();
        if (now - lastCare.getOrDefault(id, 0L) < 30000L) return;
        try {
            float health = ((Number) body.getClass().getMethod("getHealth").invoke(body)).floatValue();
            if (health < 0.35f) layer.addWounded(1);
            if (health < 0.6f) {
                String healed = layer.healWounded();
                if (healed.startsWith("healed")) {
                    layer.heal(id, 0.5);
                    lastCare.put(id, now);
                }
            }
        } catch (Throwable ignored) { }
    }

    private void rememberWeapon(String id, Object body) {
        try {
            Object item = body.getClass().getMethod("getPrimaryHandItem").invoke(body);
            if (item != null) lastWeapon.put(id, String.valueOf(item.getClass().getMethod("getFullType").invoke(item)));
        } catch (Throwable ignored) { }
    }

    private void applyEquipment(Object body, String value) {
        try {
            String marker = value.substring(0, 2);
            String type = value.substring(2);
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            Object items = inventory.getClass().getMethod("getItems").invoke(inventory);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            for (int i = 0; i < size; i++) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                if (!type.equals(String.valueOf(item.getClass().getMethod("getFullType").invoke(item)))) continue;
                if ("P:".equals(marker)) {
                    body.getClass().getMethod("setPrimaryHandItem", Class.forName("zombie.inventory.InventoryItem"))
                            .invoke(body, item);
                } else {
                    body.getClass().getMethod("setPrimaryHandItem", Class.forName("zombie.inventory.InventoryItem"))
                            .invoke(body, new Object[]{null});
                }
                return;
            }
        } catch (Throwable ignored) { }
    }

    public String equipmentSnapshot() {
        var out = new StringBuilder();
        for (var entry : bodies.entrySet()) {
            try {
                Object item = entry.getValue().getClass().getMethod("getPrimaryHandItem").invoke(entry.getValue());
                if (item != null) {
                    out.append(entry.getKey()).append("=P:")
                            .append(item.getClass().getMethod("getFullType").invoke(item)).append(';');
                } else {
                    String holster = holstered.get(entry.getKey());
                    if (holster != null) out.append(entry.getKey()).append("=H:").append(holster).append(';');
                }
            } catch (Throwable ignored) { }
        }
        return out.toString();
    }

    public String autoWeapon(String id, boolean threat) {
        return threat ? draw(id) : holster(id);
    }

    public String holster(String id) {
        Object body = bodies.get(id);
        if (body == null) return "no-body";
        try {
            Object item = body.getClass().getMethod("getPrimaryHandItem").invoke(body);
            if (item == null) return "no-weapon";
            String type = String.valueOf(item.getClass().getMethod("getFullType").invoke(item));
            holstered.put(id, type);
            body.getClass().getMethod("setPrimaryHandItem", Class.forName("zombie.inventory.InventoryItem"))
                    .invoke(body, new Object[]{null});
            return "holstered " + type;
        } catch (Throwable error) { return "holster-error=" + error.getClass().getSimpleName(); }
    }

    public String draw(String id) {
        Object body = bodies.get(id);
        if (body == null) return "no-body";
        String type = holstered.get(id);
        if (type == null) return "no-holstered";
        try {
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            Object items = inventory.getClass().getMethod("getItems").invoke(inventory);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            for (int i = 0; i < size; i++) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                if (!type.equals(String.valueOf(item.getClass().getMethod("getFullType").invoke(item)))) continue;
                body.getClass().getMethod("setPrimaryHandItem", Class.forName("zombie.inventory.InventoryItem"))
                        .invoke(body, item);
                holstered.remove(id);
                return "drawn " + type;
            }
            return "no-item-in-inventory";
        } catch (Throwable error) { return "draw-error=" + error.getClass().getSimpleName(); }
    }

    public String giveClothingFromPlayer(String id, Object player) {
        Object body = bodies.get(id);
        if (body == null || player == null) return "no-body";
        try {
            Object from = player.getClass().getMethod("getInventory").invoke(player);
            Object items = from.getClass().getMethod("getItems").invoke(from);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            for (int i = 0; i < size; i++) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                Object location = item.getClass().getMethod("getBodyLocation").invoke(item);
                if (location == null) location = item.getClass().getMethod("canBeEquipped").invoke(item);
                if (location == null) continue;
                Object inventory = body.getClass().getMethod("getInventory").invoke(body);
                Object added = inventory.getClass().getMethod("AddItem",
                        Class.forName("zombie.inventory.InventoryItem")).invoke(inventory, item);
                if (added == null) return "inventory-full";
                from.getClass().getMethod("Remove", Class.forName("zombie.inventory.InventoryItem"))
                        .invoke(from, item);
                body.getClass().getMethod("setWornItem", Class.forName("zombie.scripting.objects.ItemBodyLocation"),
                                Class.forName("zombie.inventory.InventoryItem"))
                        .invoke(body, location, added);
                return "wore " + added.getClass().getMethod("getFullType").invoke(added);
            }
            return "no-clothing";
        } catch (Throwable error) { return "wear-error=" + error.getClass().getSimpleName(); }
    }

    public String giveItemFromPlayer(String id, Object player, String type) {
        Object body = bodies.get(id);
        if (body == null || player == null || type == null) return "no-body";
        try {
            Object from = player.getClass().getMethod("getInventory").invoke(player);
            Object items = from.getClass().getMethod("getItems").invoke(from);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            for (int i = 0; i < size; i++) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                String full = String.valueOf(item.getClass().getMethod("getFullType").invoke(item));
                if (!type.equalsIgnoreCase(full)) continue;
                Object inventory = body.getClass().getMethod("getInventory").invoke(body);
                Object added = inventory.getClass().getMethod("AddItem",
                        Class.forName("zombie.inventory.InventoryItem")).invoke(inventory, item);
                if (added == null) return "inventory-full";
                from.getClass().getMethod("Remove", Class.forName("zombie.inventory.InventoryItem"))
                        .invoke(from, item);
                String category = String.valueOf(added.getClass().getMethod("getCategory").invoke(added));
                if ("Weapon".equalsIgnoreCase(category)) {
                    body.getClass().getMethod("setPrimaryHandItem", Class.forName("zombie.inventory.InventoryItem"))
                            .invoke(body, added);
                }
                return "gave " + full;
            }
            return "no-item";
        } catch (Throwable error) { return "give-error=" + error.getClass().getSimpleName(); }
    }

    public String giveWeaponFromPlayer(String id, Object player) {
        Object body = bodies.get(id);
        if (body == null || player == null) return "no-body";
        try {
            Object from = player.getClass().getMethod("getInventory").invoke(player);
            Object items = from.getClass().getMethod("getItems").invoke(from);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            Object chosen = null;
            for (int i = 0; i < size; i++) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                String category = String.valueOf(item.getClass().getMethod("getCategory").invoke(item));
                if ("Weapon".equalsIgnoreCase(category)) { chosen = item; break; }
            }
            if (chosen == null) return "no-weapon-in-player";
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            Object added = inventory.getClass().getMethod("AddItem",
                    Class.forName("zombie.inventory.InventoryItem")).invoke(inventory, chosen);
            if (added == null) return "inventory-full";
            body.getClass().getMethod("setPrimaryHandItem", Class.forName("zombie.inventory.InventoryItem"))
                    .invoke(body, added);
            from.getClass().getMethod("Remove", Class.forName("zombie.inventory.InventoryItem"))
                    .invoke(from, chosen);
            return String.valueOf(added.getClass().getMethod("getFullType").invoke(added));
        } catch (Throwable error) { return "give-error=" + error.getClass().getSimpleName(); }
    }

    public String autoEquip(String id) {
        Object body = bodies.get(id);
        if (body == null) return "no-body";
        try {
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            Object items = inventory.getClass().getMethod("getItems").invoke(inventory);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            Object best = null;
            int bestScore = -1;
            for (int i = 0; i < size; i++) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                String category = String.valueOf(item.getClass().getMethod("getCategory").invoke(item));
                if (!"Weapon".equalsIgnoreCase(category)) continue;
                int condition = ((Number) item.getClass().getMethod("getCondition").invoke(item)).intValue();
                int ammo = 0;
                try { ammo = ((Number) item.getClass().getMethod("getCurrentAmmoCount").invoke(item)).intValue(); }
                catch (Throwable ignored) { }
                int score = condition + ammo * 10;
                if (score > bestScore) { bestScore = score; best = item; }
            }
            if (best == null) return "no-weapon";
            body.getClass().getMethod("setPrimaryHandItem", Class.forName("zombie.inventory.InventoryItem"))
                    .invoke(body, best);
            return String.valueOf(best.getClass().getMethod("getFullType").invoke(best));
        } catch (Throwable error) { return "equip-error=" + error.getClass().getSimpleName(); }
    }

    public void setFactionChances(int ranged, int melee) {
        factionRangedChance = Math.max(0, Math.min(100, ranged));
        factionMeleeChance = Math.max(0, Math.min(100 - factionRangedChance, melee));
    }

    public String factionChances() { return factionRangedChance + "/" + factionMeleeChance; }

    public String factionKitRoll(String faction, int roll) {
        var kit = OwnNpcFactionKit.kit(faction, factionRangedChance, factionMeleeChance, roll);
        return "weapon=" + kit.weapon() + " accessory=" + kit.accessory();
    }

    public String startingWeapon() { return startingWeapon; }

    public void setStartingWeapon(String type) {
        String value = type == null ? "" : type.toLowerCase(java.util.Locale.ROOT);
        startingWeapon = switch (value) {
            case "firearm", "gun", "pistol" -> "firearm";
            case "none", "unarmed" -> "none";
            default -> "melee";
        };
    }

    public NpcIdentity identity(String id) { return id == null ? null : identities.get(id); }

    public String identitySummary(String id) {
        var value = identity(id);
        return value == null ? "" : value.summary();
    }

    public String identitySnapshot() {
        var text = new StringBuilder();
        for (var entry : new java.util.TreeMap<>(identities).entrySet()) {
            text.append(entry.getKey()).append('|').append(entry.getValue().csv()).append('\n');
        }
        return text.toString();
    }

    public int restoreIdentities(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) return 0;
        int restored = 0;
        for (var line : snapshot.split("\\R")) {
            int split = line.indexOf('|');
            if (split <= 0 || split == line.length() - 1) continue;
            try {
                identities.put(line.substring(0, split), NpcIdentity.parse(line.substring(split + 1)));
                restored++;
            } catch (IllegalArgumentException ignored) { }
        }
        return restored;
    }

    private static String professionConstant(String key) {
        return switch (key) {
            case "doctor" -> "DOCTOR";
            case "carpenter" -> "CARPENTER";
            case "farmer" -> "FARMER";
            case "mechanic" -> "MECHANICS";
            case "police" -> "POLICE_OFFICER";
            case "veteran" -> "VETERAN";
            case "chef" -> "CHEF";
            case "fisher" -> "FISHERMAN";
            case "guard" -> "SECURITY_GUARD";
            default -> "UNEMPLOYED";
        };
    }

    private static void applyIdentity(Object desc, NpcIdentity identity) {
        try {
            desc.getClass().getMethod("setForename", String.class).invoke(desc, identity.forename());
            desc.getClass().getMethod("setSurname", String.class).invoke(desc, identity.surname());
            desc.getClass().getMethod("setFemale", boolean.class).invoke(desc, "female".equals(identity.gender()));
            Class<?> professionClass = Class.forName("zombie.scripting.objects.CharacterProfession");
            Object profession = professionClass.getField(professionConstant(identity.professionKey())).get(null);
            desc.getClass().getMethod("setCharacterProfession", professionClass).invoke(desc, profession);
            Class<?> definitionClass = Class.forName("zombie.characters.professions.CharacterProfessionDefinition");
            Object definition = definitionClass.getMethod("getCharacterProfessionDefinition", professionClass)
                    .invoke(null, profession);
            if (definition != null) {
                desc.getClass().getMethod("setProfessionSkills", definitionClass).invoke(desc, definition);
            }
        } catch (Throwable ignored) { }
    }

    private static void applySkills(Object body, NpcIdentity identity) {
        try {
            Object xp = body.getClass().getMethod("getXp").invoke(body);
            Class<?> perkClass = Class.forName("zombie.characters.skills.PerkFactory$Perk");
            Class<?> factory = Class.forName("zombie.characters.skills.PerkFactory");
            var lookup = factory.getMethod("getPerkFromName", String.class);
            var setter = xp.getClass().getMethod("setXPToLevel", perkClass, int.class);
            for (var entry : identity.skills().entrySet()) {
                Object perk = lookup.invoke(null, entry.getKey());
                if (perk != null) setter.invoke(xp, perk, entry.getValue());
            }
        } catch (Throwable ignored) { }
    }

    private Object create(Object cell, String id, int x, int y, int z, String faction) throws Exception {
        Class<?> squareClass=Class.forName("zombie.iso.IsoGridSquare");
        Object square=cell.getClass().getMethod("getGridSquare",int.class,int.class,int.class).invoke(cell,x,y,z);
        if (square == null) {
            // A body can be requested before its exact square reaches the streamed set.
            search:
            for (int radius = 1; radius <= 4; radius++) {
                for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) continue;
                    Object candidate = cell.getClass().getMethod("getGridSquare",int.class,int.class,int.class)
                            .invoke(cell, x + dx, y + dy, z);
                    if (candidate != null) {
                        square = candidate;
                        x += dx;
                        y += dy;
                        System.out.println("[PersonalKnox] Own body spawn square fallback " + dx + "," + dy);
                        break search;
                    }
                }
            }
        }
        if(square==null) throw new IllegalStateException("NPC spawn square is not loaded");
        Class<?> playerClass = Class.forName("zombie.characters.IsoPlayer");
        Class<?> factory = Class.forName("zombie.characters.SurvivorFactory");
        Object desc = factory.getMethod("CreateSurvivor").invoke(null);
        var identity = identities.computeIfAbsent(id, key -> NpcIdentityDatabase.generate(key, preferRussianNames));
        applyIdentity(desc, identity);
        Constructor<?> ctor = null;
        for (var candidate : OwnNpcBodyType.forBase(playerClass).getConstructors()) {
            var p = candidate.getParameterTypes();
            if (p.length == 5 && p[0].isInstance(cell) && p[1].isInstance(desc)) { ctor = candidate; break; }
        }
        if (ctor == null) throw new IllegalStateException("No IsoPlayer constructor");
        Object body = ctor.newInstance(cell, desc, x, y, z);
        invoke(body, "setNpc", new Class<?>[]{boolean.class}, true);
        invoke(body, "setZombiesDontAttack", new Class<?>[]{boolean.class}, false);
        try {
            body.getClass().getField("playerIndex").setInt(body, 1);
            body.getClass().getField("serverPlayerIndex").setInt(body, 1);
        } catch (Throwable ignored) { }
        invoke(body, "setBlockMovement", new Class<?>[]{boolean.class}, false);
        invoke(body, "setUsername", new Class<?>[]{String.class}, identity.displayName());
        applySkills(body, identity);
        try {
            try {
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            inventory.getClass().getMethod("AddItem", String.class).invoke(inventory, "Base.Hammer");
            inventory.getClass().getMethod("AddItem", String.class).invoke(inventory, "Base.Screwdriver");
            inventory.getClass().getMethod("AddItem", String.class).invoke(inventory, "Base.Saw");
            boolean factionUnit = faction != null && !faction.isBlank() && !"own".equalsIgnoreCase(faction);
            var kit = OwnNpcFactionKit.kit(faction, factionRangedChance, factionMeleeChance,
                    java.util.concurrent.ThreadLocalRandom.current().nextInt(100));
            String remembered = lastWeapon.get(id);
            String weapon = remembered != null ? remembered : factionUnit ? kit.weapon() : switch (startingWeapon) {
                case "firearm" -> "Base.Pistol";
                case "none" -> "";
                default -> "Base.BaseballBat";
            };
            if (factionUnit) {
                try { inventory.getClass().getMethod("AddItem", String.class).invoke(inventory, kit.accessory()); }
                catch (Throwable ignored) { }
            }
            if (!weapon.isEmpty()) {
                Object item = inventory.getClass().getMethod("AddItem", String.class).invoke(inventory, weapon);
                body.getClass().getMethod("setPrimaryHandItem", Class.forName("zombie.inventory.InventoryItem"))
                        .invoke(body, item);
                if ("firearm".equals(startingWeapon)) {
                    try { item.getClass().getMethod("setCurrentAmmoCount", int.class).invoke(item, 12); }
                    catch (Throwable ignored) { }
                }
                System.out.println("[PersonalKnox] Own body starting weapon " + weapon);
            }
            System.out.println("[PersonalKnox] Own body starting tools added");
        } catch (Throwable ignored) { }
        if (!matchPlayerClothes || !dressLikePlayer(body)) {
            dress(body, desc, id);
            wear(body);
        }
            body.getClass().getMethod("setCurrentSquare",squareClass).invoke(body,square);
            // A non-null current square does not mean membership in the simulation.
            // IsoCell queues additions safely when its object list is being updated.
            cell.getClass().getMethod("addMovingObject",Class.forName("zombie.iso.IsoMovingObject")).invoke(cell,body);
            @SuppressWarnings("unchecked")
            var moving=(java.util.Collection<Object>)square.getClass().getMethod("getMovingObjects").invoke(square);
            if(!moving.contains(body)) moving.add(body);
            invoke(body,"setSceneCulled",new Class<?>[]{boolean.class},false);
            Class<?> models=Class.forName("zombie.core.skinnedmodel.ModelManager");
            models.getMethod("Add",Class.forName("zombie.characters.IsoGameCharacter"))
                    .invoke(models.getField("instance").get(null),body);
        } catch(Exception failure) {
            call(body,"removeFromWorld");call(body,"removeFromSquare");
            throw failure;
        }
        String pending = pendingEquipment.remove(id);
        if (pending != null) applyEquipment(body, pending);
        System.out.println("[PersonalKnox] Own body " + id + " at "
                + String.format(java.util.Locale.ROOT, "%.1f,%.1f,%.1f", (double) x, (double) y, (double) z));
        return body;
    }

    private static void wear(Object body) {
        try {
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            Class<?> locations = Class.forName("zombie.scripting.objects.ItemBodyLocation");
            String[][] items = {{"TSHIRT", "Base.Tshirt_WhiteLongSleeve"}, {"PANTS", "Base.Trousers"}, {"SHOES", "Base.Shoes_Black"}};
            for (var pair : items) {
                Object location = locations.getField(pair[0]).get(null);
                Object item = inventory.getClass().getMethod("AddItem", String.class).invoke(inventory, pair[1]);
                if (item != null) body.getClass().getMethod("setWornItem", location.getClass(),
                                Class.forName("zombie.inventory.InventoryItem"))
                        .invoke(body, location, item);
            }
            System.out.println("[PersonalKnox] Own body worn clothes applied");
        } catch (Throwable error) {
            System.out.println("[PersonalKnox] Own body wear failed: " + error);
        }
    }


    private boolean dressLikePlayer(Object body) {
        try {
            Object player = Class.forName("zombie.characters.IsoPlayer").getMethod("getInstance").invoke(null);
            if (player == null) return false;
            Object worn = player.getClass().getMethod("getWornItems").invoke(player);
            int size = (int) worn.getClass().getMethod("size").invoke(worn);
            if (size == 0) return false;
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            int copied = 0;
            for (int i = 0; i < size; i++) {
                Object entry = worn.getClass().getMethod("get", int.class).invoke(worn, i);
                Object item = entry.getClass().getMethod("getItem").invoke(entry);
                Object location = entry.getClass().getMethod("getLocation").invoke(entry);
                if (item == null) continue;
                String type = String.valueOf(item.getClass().getMethod("getFullType").invoke(item));
                Object added = inventory.getClass().getMethod("AddItem", String.class).invoke(inventory, type);
                if (added == null) continue;
                body.getClass().getMethod("setWornItem", location.getClass(),
                        Class.forName("zombie.inventory.InventoryItem")).invoke(body, location, added);
                copied++;
            }
            if (copied == 0) return false;
            System.out.println("[PersonalKnox] Own body clothes match player: " + copied);
            return true;
        } catch (Throwable ignored) { return false; }
    }

    private static void dress(Object body, Object desc, String id) {
        try {
            body.getClass().getMethod("Dressup", desc.getClass()).invoke(body, desc);
        } catch (Throwable ignored) { }
        for (var name : new String[]{"dressInRandomNonSillyOutfit", "dressInRandomOutfit"}) {
            try {
                body.getClass().getMethod(name).invoke(body);
                System.out.println("[PersonalKnox] Own body dressed via " + name);
                break;
            } catch (Throwable ignored) { }
        }
        try {
            Object player = Class.forName("zombie.characters.IsoPlayer").getMethod("getInstance").invoke(null);
            if (player != null) {
                Object bodyVisual = body.getClass().getMethod("getHumanVisual").invoke(body);
                int seed = id.hashCode();
                Class<?> colorType = Class.forName("zombie.core.ImmutableColor");
                java.lang.reflect.Constructor<?> color = colorType.getConstructor(float.class, float.class, float.class);
                float r = 0.55f + Math.floorMod(seed, 25) / 100f;
                float g = 0.55f + Math.floorMod(seed / 7, 25) / 100f;
                float b = 0.55f + Math.floorMod(seed / 13, 25) / 100f;
                Object skin = color.newInstance(r, g, b);
                bodyVisual.getClass().getMethod("setSkinColor", colorType).invoke(bodyVisual, skin);
                bodyVisual.getClass().getMethod("setHairColor", colorType).invoke(bodyVisual, skin);
                bodyVisual.getClass().getMethod("setNaturalHairColor", colorType).invoke(bodyVisual, skin);
                System.out.println("[PersonalKnox] Own body clothed and tinted " + id);
                return;
            }
        } catch (Throwable error) {
            System.out.println("[PersonalKnox] Own body visual fallback failed: " + error);
        }
        System.out.println("[PersonalKnox] Own body dress failed");
    }

    public String debugPlaceLoot(String id) {
        Object body = bodies.get(id);
        if (body == null) return "no-body";
        try {
            Object cell = currentCell();
            int bx = ((Number) body.getClass().getMethod("getX").invoke(body)).intValue();
            int by = ((Number) body.getClass().getMethod("getY").invoke(body)).intValue();
            int bz = ((Number) body.getClass().getMethod("getZ").invoke(body)).intValue();
            Throwable last = null;
            for (int dx = 0; dx <= 2; dx++) for (int dy = 0; dy <= 2; dy++) {
                Object square = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                        .invoke(cell, bx + dx, by + dy, bz);
                if (square == null) continue;
                try {
                    square.getClass().getMethod("AddWorldInventoryItem", String.class, float.class, float.class, float.class)
                            .invoke(square, "Base.Bread", 0.5f, 0.5f, 0f);
                    return "placed";
                } catch (Throwable error) { last = error; }
            }
            return "place-error=" + (last == null ? "no-square" : last.getClass().getSimpleName());
        } catch (Throwable error) { return "place-error=" + error.getClass().getSimpleName(); }
    }

    public String debugLoot(String id, OwnNpcLayer layer) {
        Object body = bodies.get(id);
        if (body == null) return "no-body";
        try {
            Object cell = currentCell();
            double bx = ((Number) body.getClass().getMethod("getX").invoke(body)).doubleValue();
            double by = ((Number) body.getClass().getMethod("getY").invoke(body)).doubleValue();
            double bz = ((Number) body.getClass().getMethod("getZ").invoke(body)).doubleValue();
            double[] point = findLoot(cell, bx, by, bz, id);
            if (point == null) return "no-container";
            loot(id, body, layer);
            return "looted";
        } catch (Throwable error) { return "loot-error=" + error.getClass().getSimpleName(); }
    }

    private double[] findLoot(Object cell, double cx, double cy, double cz, String id) {
        try {
            for (int radius = 1; radius <= 8; radius++) {
                for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) continue;
                    Object square = cell.getClass().getMethod("getGridSquare", int.class, int.class, int.class)
                            .invoke(cell, (int) cx + dx, (int) cy + dy, (int) cz);
                    if (square == null) continue;
                    Object objects = square.getClass().getMethod("getObjects").invoke(square);
                    int size = (int) objects.getClass().getMethod("size").invoke(objects);
                    for (int i = 0; i < size; i++) {
                        Object object = objects.getClass().getMethod("get", int.class).invoke(objects, i);
                        if (object == null) continue;
                        try {
                            Object container = object.getClass().getMethod("getContainer").invoke(object);
                            if (container != null) {
                                lootObjects.put(id, object);
                                return new double[]{(int) cx + dx + 0.5, (int) cy + dy + 0.5};
                            }
                        } catch (NoSuchMethodException ignored) { }
                    }
                }
            }
        } catch (Throwable ignored) { }
        return null;
    }

    private void loot(String id, Object body, OwnNpcLayer layer) {
        Object object = lootObjects.remove(id);
        if (object == null) return;
        try {
            Object container = object.getClass().getMethod("getContainer").invoke(object);
            Object items = container.getClass().getMethod("getItems").invoke(container);
            int size = (int) items.getClass().getMethod("size").invoke(items);
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            int moved = 0;
            for (int i = size - 1; i >= 0 && moved < 8; i--) {
                Object item = items.getClass().getMethod("get", int.class).invoke(items, i);
                if (item == null) continue;
                try {
                    Object added = inventory.getClass().getMethod("AddItem",
                            Class.forName("zombie.inventory.InventoryItem")).invoke(inventory, item);
                    if (added != null) {
                        container.getClass().getMethod("Remove",
                                Class.forName("zombie.inventory.InventoryItem")).invoke(container, item);
                        moved++;
                    }
                } catch (Throwable ignored) { }
            }
            if (moved > 0) {
                layer.addSupplies(id, moved);
                System.out.println("[PersonalKnox][own-loot] " + id + " items=" + moved);
            }
        } catch (Throwable ignored) { }
    }

    private static double[] playerPosition() {
        try {
            Object player = Class.forName("zombie.characters.IsoPlayer").getMethod("getInstance").invoke(null);
            if (player == null) return null;
            return new double[]{
                    ((Number) player.getClass().getMethod("getX").invoke(player)).doubleValue(),
                    ((Number) player.getClass().getMethod("getY").invoke(player)).doubleValue()};
        } catch (Throwable ignored) { return null; }
    }

    private static java.util.List<Object[]> zombieSnapshotNear(Object cell, double px, double py, double radius) {
        var result = new java.util.ArrayList<Object[]>();
        double radiusSq = radius * radius;
        try {
            Object zombies = cell.getClass().getMethod("getZombieList").invoke(cell);
            int size = (int) zombies.getClass().getMethod("size").invoke(zombies);
            for (int i = 0; i < size; i++) {
                Object zombie = zombies.getClass().getMethod("get", int.class).invoke(zombies, i);
                if (zombie == null || !zombie.getClass().getName().equals("zombie.characters.IsoZombie")) continue;
                double x = ((Number) zombie.getClass().getMethod("getX").invoke(zombie)).doubleValue();
                double y = ((Number) zombie.getClass().getMethod("getY").invoke(zombie)).doubleValue();
                double dx = x - px, dy = y - py;
                if (dx * dx + dy * dy > radiusSq) continue;
                result.add(new Object[]{zombie, x, y});
            }
        } catch (Throwable ignored) { }
        return result;
    }

    private static java.util.List<Object[]> zombieSnapshot(Object cell) {
        var result = new java.util.ArrayList<Object[]>();
        try {
            Object zombies = cell.getClass().getMethod("getZombieList").invoke(cell);
            int size = (int) zombies.getClass().getMethod("size").invoke(zombies);
            for (int i = 0; i < size; i++) {
                Object zombie = zombies.getClass().getMethod("get", int.class).invoke(zombies, i);
                if (zombie == null || !zombie.getClass().getName().equals("zombie.characters.IsoZombie")) continue;
                double x = ((Number) zombie.getClass().getMethod("getX").invoke(zombie)).doubleValue();
                double y = ((Number) zombie.getClass().getMethod("getY").invoke(zombie)).doubleValue();
                result.add(new Object[]{zombie, x, y});
            }
        } catch (Throwable ignored) { }
        return result;
    }

    private void attack(String id, Object body, java.util.List<Object[]> zombies) {
        try {
            double bx = ((Number) body.getClass().getMethod("getX").invoke(body)).doubleValue();
            double by = ((Number) body.getClass().getMethod("getY").invoke(body)).doubleValue();
            double bz = ((Number) body.getClass().getMethod("getZ").invoke(body)).doubleValue();
            Object weapon = body.getClass().getMethod("getPrimaryHandItem").invoke(body);
            boolean ranged = false;
            boolean scope = false;
            int ammo = 0;
            int remoteRange = 8;
            if (weapon != null) {
                try {
                    boolean isWeapon = (boolean) weapon.getClass().getMethod("IsWeapon").invoke(weapon);
                    ranged = isWeapon && (boolean) weapon.getClass().getMethod("isRanged").invoke(weapon);
                    if (ranged) {
                        ammo = ((Number) weapon.getClass().getMethod("getCurrentAmmoCount").invoke(weapon)).intValue();
                        remoteRange = ((Number) weapon.getClass().getMethod("getRemoteRange").invoke(weapon)).intValue();
                        String type = String.valueOf(weapon.getClass().getMethod("getFullType").invoke(weapon)).toLowerCase(java.util.Locale.ROOT);
                        scope = type.contains("scope") || type.contains("sniper");
                    }
                } catch (Throwable ignored) { }
            }
            String weather = "";
            try {
                Object world = Class.forName("zombie.iso.IsoWorld").getField("instance").get(null);
                weather = String.valueOf(world.getClass().getMethod("getWeather").invoke(world));
            } catch (Throwable ignored) { }
            double range = ranged && ammo > 0
                    ? OwnNpcCombat.engagementRange(remoteRange, scope, weather)
                    : 1.6;
            Object cell = currentCell();
            Object bodySquare = body.getClass().getMethod("getCurrentSquare").invoke(body);
            Object nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            double nearestX = 0, nearestY = 0;
            for (var entry : zombies) {
                Object zombie = entry[0];
                Object zombieSquare = zombie.getClass().getMethod("getCurrentSquare").invoke(zombie);
                double distance = zombieSquare == bodySquare
                        ? 0
                        : Math.hypot((double) entry[1] - bx, (double) entry[2] - by);
                if (distance > range) continue;
                double zz = ((Number) zombie.getClass().getMethod("getZ").invoke(zombie)).doubleValue();
                boolean clear = distance <= 1.5;
                try {
                    if (clear) throw new IllegalStateException("close-range line of sight");
                    Object result = Class.forName("zombie.iso.LosUtil")
                            .getMethod("lineClear", Class.forName("zombie.iso.IsoCell"),
                                    int.class, int.class, int.class, int.class, int.class, int.class, boolean.class)
                            .invoke(null, cell, (int) bx, (int) by, (int) bz,
                                    (int) entry[1], (int) entry[2], (int) zz, false);
                    String visibility = String.valueOf(result);
                    clear = visibility.equals("Clear") || visibility.equals("ClearThroughWindow")
                            || visibility.equals("ClearThroughOpenDoor");
                } catch (Throwable ignored) { }
                if (!clear) continue;
                try {
                    zombie.getClass().getMethod("setTarget", Class.forName("zombie.iso.IsoMovingObject"))
                            .invoke(zombie, body);
                } catch (Throwable ignored) { }
                if (distance < nearestDistance) {
                    nearest = zombie;
                    nearestDistance = distance;
                    nearestX = (double) entry[1];
                    nearestY = (double) entry[2];
                }
            }
            if (nearest == null) {
                lastAim.remove(id);
                return;
            }
            int weaponLevel = 1;
            int condition = 1;
            int conditionMax = 1;
            if (weapon != null) {
                try {
                    weaponLevel = ((Number) weapon.getClass().getMethod("getWeaponLevel").invoke(weapon)).intValue();
                    condition = ((Number) weapon.getClass().getMethod("getCondition").invoke(weapon)).intValue();
                    conditionMax = ((Number) weapon.getClass().getMethod("getConditionMax").invoke(weapon)).intValue();
                } catch (Throwable ignored) { }
            }
            int skill = skillLevel(body, ranged);
            double damage = OwnNpcCombat.damage(weaponLevel, skill, condition, conditionMax, ranged);
            if (ranged) {
                try {
                    weapon.getClass().getMethod("setCurrentAmmoCount", int.class).invoke(weapon, ammo - 1);
                } catch (Throwable ignored) { }
            }
            double zombieHealth = ((Number) nearest.getClass().getMethod("getHealth").invoke(nearest)).doubleValue();
            nearest.getClass().getMethod("setHealth", float.class)
                    .invoke(nearest, (float) Math.max(0, zombieHealth - damage));
            Object square = nearest.getClass().getMethod("getCurrentSquare").invoke(nearest);
            body.getClass().getMethod("setAttackTargetSquare", Class.forName("zombie.iso.IsoGridSquare"))
                    .invoke(body, square);
            body.getClass().getMethod("setAttackStarted", boolean.class).invoke(body, true);
            lastAim.put(id, new double[]{nearestX, nearestY, nearestDistance, ranged ? 1 : 0});
            System.out.println("[PersonalKnox][own-combat] target=" + nearest.getClass().getSimpleName()
                    + " damage=" + String.format(java.util.Locale.ROOT, "%.1f", damage) + " skill=" + skill + " ranged=" + ranged + " scope=" + scope
                    + " weather=" + weather + " range=" + String.format(java.util.Locale.ROOT, "%.1f", range)
                    + " distance=" + String.format(java.util.Locale.ROOT, "%.1f", nearestDistance));
        } catch (Throwable error) {
            System.out.println("[PersonalKnox][own-combat] error: " + error);
            error.printStackTrace(System.out);
        }
    }

    private static int skillLevel(Object body, boolean ranged) {
        try {
            Object perks = Class.forName("zombie.characters.skills.PerkFactory").getField("PerkList").get(null);
            int size = (int) perks.getClass().getMethod("size").invoke(perks);
            for (int i = 0; i < size; i++) {
                Object perk = perks.getClass().getMethod("get", int.class).invoke(perks, i);
                String name = String.valueOf(perk.getClass().getField("name").get(perk));
                if (ranged && name.equalsIgnoreCase("Firearm") || !ranged && name.equalsIgnoreCase("Axe")) {
                    return ((Number) body.getClass().getMethod("getPerkLevel",
                            Class.forName("zombie.characters.skills.PerkFactory$Perk")).invoke(body, perk)).intValue();
                }
            }
        } catch (Throwable ignored) { }
        return 0;
    }

    private void speak(String id, String text) {
        Object body = bodies.get(id);
        if (body == null) return;
        String value = personal.knox.TextCodec.forChat(text);
        try {
            body.getClass().getMethod("addLineChatElement", String.class, float.class, float.class, float.class)
                    .invoke(body, value, 1f, 1f, 1f);
        } catch (Throwable ignored) { }
    }

    private void autoConsume(OwnNpcLayer layer, String id, Object body, OwnNpcAgent agent) {
        if (agent.state().hunger() >= 0.6) {
            String food = consumeFood(id);
            if (food.startsWith("ate ")) {
                layer.relieve(id, 0.5, 0);
                layer.consumeSupply(id, 1);
                System.out.println("[PersonalKnox][own-eat] auto " + id + " " + food);
            } else if (layer.consumeBaseFood()) {
                layer.relieve(id, 0.5, 0);
                System.out.println("[PersonalKnox][own-eat] base " + id);
            }
        }
        if (agent.state().thirst() >= 0.6) {
            String drink = consumeDrink(id);
            if (drink.startsWith("drank ")) {
                layer.relieve(id, 0, 0.5);
                layer.consumeSupply(id, 1);
                System.out.println("[PersonalKnox][own-drink] auto " + id + " " + drink);
            } else if (layer.consumeBaseWater()) {
                layer.relieve(id, 0, 0.5);
                System.out.println("[PersonalKnox][own-drink] base " + id);
            }
        }
    }

    private void retreat(OwnNpcLayer layer, String id, Object body, OwnNpcAgent agent) {
        double[] aim = lastAim.remove(id);
        if (aim == null || aim[3] < 0.5 || aim[2] > 3.5) return;
        var order = layer.orderOf(id);
        if (order == OwnNpcOrder.HOLD || order == OwnNpcOrder.REST) return;
        try {
            var point = OwnNpcCombat.retreatPoint(agent.x(), agent.y(), aim[0], aim[1], 6, 2);
            if (point.x() != agent.x() || point.y() != agent.y()) {
                layer.order(id, point.x(), point.y());
                System.out.println("[PersonalKnox][own-combat] retreat=" + id
                        + " to=" + String.format(java.util.Locale.ROOT, "%.1f,%.1f", point.x(), point.y()));
            }
            double bx = ((Number) body.getClass().getMethod("getX").invoke(body)).doubleValue();
            double by = ((Number) body.getClass().getMethod("getY").invoke(body)).doubleValue();
            double angle = Math.toDegrees(Math.atan2(aim[1] - by, aim[0] - bx));
            try { body.getClass().getMethod("setDirectionAngle", float.class).invoke(body, (float) angle); }
            catch (Throwable ignored) { }
        } catch (Throwable ignored) { }
    }

    private static void syncHealth(OwnNpcLayer layer, String id, Object body) {
        try {
            Object damage = body.getClass().getMethod("getBodyDamage").invoke(body);
            double health = ((Number) damage.getClass().getMethod("getHealth").invoke(damage)).doubleValue();
            var agent = layer.agent(id);
            if (agent == null) return;
            double delta = agent.state().health() - health;
            if (delta > 0.5) layer.damage(id, delta);
        } catch (Throwable ignored) { }
    }

    private void move(String id, Object body, double x, double y, double z, double tx, double ty, double targetZ) {
        double sx = x, sy = y;
        try {
            Object cell = currentCell();
            double bx = ((Number) body.getClass().getMethod("getX").invoke(body)).doubleValue();
            double by = ((Number) body.getClass().getMethod("getY").invoke(body)).doubleValue();
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
                    String obstacle = startDismantle(id, tx, ty);
                    if (!obstacle.isEmpty())
                        System.out.println("[PersonalKnox][own-dismantle] " + id + " start " + obstacle);
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
            invoke(body, "setDirectionAngle", new Class<?>[]{float.class}, (float) Math.toDegrees(Math.atan2(dy, dx)));
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
            var point = cached.points().get(cached.index());
            pathCache.put(id, new PathState(cached.tx(), cached.ty(), cached.points(), cached.index() + 1));
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
