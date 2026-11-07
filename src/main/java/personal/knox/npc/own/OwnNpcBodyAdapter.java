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
    private final Map<String, Object> lootObjects = new HashMap<>();
    private long lastMoveLog;
    private final Map<String, DismantleTask> dismantles = new HashMap<>();
    private record DismantleTask(OwnNpcDismantleJob job, Object object, double x, double y) { }

    public int sync(OwnNpcLayer layer) {
        if (layer == null) throw new IllegalArgumentException("Missing layer");
        if (pathCache.size() > 128) pathCache.clear();
        if (unavailable) return 0;
        try {
            Object cell = currentCell();
            if (cell == null) return 0;
            var seen = new HashSet<String>();
            var zombies = zombieSnapshot(cell);
            for (var agent : layer.agents()) {
                seen.add(agent.id());
                Object body = bodies.get(agent.id());
                if (body != null) {
                    syncHealth(layer, agent.id(), body);
                    attack(body, zombies);
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
                if (body == null) {
                    body = create(cell, agent.id(), (int) agent.x(), (int) agent.y(), (int) agent.z());
                    if (body == null) continue;
                    bodies.put(agent.id(), body);
                }
                if (layer.orderOf(agent.id()) == OwnNpcOrder.LOOT) {
                    double[] loot = findLoot(cell, agent.x(), agent.y(), agent.z(), agent.id());
                    if (loot != null) {
                        layer.order(agent.id(), loot[0], loot[1]);
                        if (Math.hypot(agent.x() - loot[0], agent.y() - loot[1]) < 1.8) loot(agent.id(), body, layer);
                    }
                }
                move(agent.id(), body, agent.x(), agent.y(), agent.z(), agent.targetX(), agent.targetY(), layer.targetZ(agent.id()));
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
            return bodies.size();
        } catch (Throwable error) {
            unavailable = true;
            System.out.println("[PersonalKnox] Own body adapter unavailable: " + error);
            return 0;
        }
    }

    public int count() { return bodies.size(); }
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

    private Object create(Object cell, String id, int x, int y, int z) throws Exception {
        Class<?> squareClass=Class.forName("zombie.iso.IsoGridSquare");
        Object square=cell.getClass().getMethod("getGridSquare",int.class,int.class,int.class).invoke(cell,x,y,z);
        if(square==null) throw new IllegalStateException("NPC spawn square is not loaded");
        Class<?> playerClass = Class.forName("zombie.characters.IsoPlayer");
        Class<?> factory = Class.forName("zombie.characters.SurvivorFactory");
        Object desc = factory.getMethod("CreateSurvivor").invoke(null);
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
            body.getClass().getField("playerIndex").setInt(body, -1);
            body.getClass().getField("serverPlayerIndex").setInt(body, -1);
        } catch (Throwable ignored) { }
        invoke(body, "setBlockMovement", new Class<?>[]{boolean.class}, false);
        invoke(body, "setUsername", new Class<?>[]{String.class}, "PK-" + id);
        try {
            try {
            Object inventory = body.getClass().getMethod("getInventory").invoke(body);
            inventory.getClass().getMethod("AddItem", String.class).invoke(inventory, "Base.Hammer");
            inventory.getClass().getMethod("AddItem", String.class).invoke(inventory, "Base.Screwdriver");
            System.out.println("[PersonalKnox] Own body starting tools added");
        } catch (Throwable ignored) { }
        dress(body, desc, id);
        wear(body);
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
                Object playerVisual = player.getClass().getMethod("getHumanVisual").invoke(player);
                Object bodyVisual = body.getClass().getMethod("getHumanVisual").invoke(body);
                Class<?> base = Class.forName("zombie.core.skinnedmodel.visual.BaseVisual");
                bodyVisual.getClass().getMethod("copyFrom", base).invoke(bodyVisual, playerVisual);
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

    private static void attack(Object body, java.util.List<Object[]> zombies) {
        try {
            double bx = ((Number) body.getClass().getMethod("getX").invoke(body)).doubleValue();
            double by = ((Number) body.getClass().getMethod("getY").invoke(body)).doubleValue();
            for (var entry : zombies) {
                double distance = Math.hypot((double) entry[1] - bx, (double) entry[2] - by);
                Object zombie = entry[0];
                if (distance <= 8) {
                    try {
                        zombie.getClass().getMethod("setTarget", Class.forName("zombie.iso.IsoMovingObject"))
                                .invoke(zombie, body);
                    } catch (Throwable ignored) { }
                }
                if (distance > 1.6) continue;
                Object damage = zombie.getClass().getMethod("getBodyDamage").invoke(zombie);
                damage.getClass().getMethod("ReduceHealth", float.class).invoke(damage, 10f);
                Object square = zombie.getClass().getMethod("getCurrentSquare").invoke(zombie);
                body.getClass().getMethod("setAttackTargetSquare", Class.forName("zombie.iso.IsoGridSquare"))
                        .invoke(body, square);
                body.getClass().getMethod("setAttackStarted", boolean.class).invoke(body, true);
                return;
            }
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
