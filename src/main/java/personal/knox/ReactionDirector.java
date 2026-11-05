package personal.knox;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded, event-edge based chatter. Does not issue movement or combat commands. */
public final class ReactionDirector {
    private static final long GROUP_COOLDOWN = 20_000;
    private static final long NPC_COOLDOWN = 90_000;
    private static final long TOPIC_COOLDOWN = 180_000;
    private static final long PENDING_LIFETIME = 30_000;
    private static final long NEED_REMINDER = 300_000;
    private final LinkedHashMap<String, State> states = new LinkedHashMap<>();
    private long lastGroupSpeech = Long.MIN_VALUE;

    private static final class State {
        double health = Double.NaN;
        double playerHealth = Double.NaN;
        int zombies;
        boolean rain, dark, corpses, bleeding;
        boolean hungry, thirsty, tired;
        long lastSpeech = Long.MIN_VALUE;
        String pending;
        int priority;
        long expires;
        final Map<String, Long> topics = new HashMap<>();
    }

    public void reset() { states.clear(); lastGroupSpeech = Long.MIN_VALUE; }
    public int trackedCount() { return states.size(); }

    private static boolean elapsed(long now, long then, long interval) {
        return then == Long.MIN_VALUE || (now >= then && now - then >= interval);
    }

    private static void offer(State state, String topic, int priority, long now) {
        if (!elapsed(now, state.topics.getOrDefault(topic, Long.MIN_VALUE), TOPIC_COOLDOWN)) return;
        if (state.pending == null || now > state.expires || priority > state.priority) {
            state.pending = topic;
            state.priority = priority;
            state.expires = now + PENDING_LIFETIME;
        }
    }

    public String observe(String key, long now, double health, double playerHealth, boolean bleeding,
                          int zombies, boolean rain, boolean dark, boolean corpses) {
        return observe(key, now, health, playerHealth, bleeding, zombies, rain, dark, corpses,
                Double.NaN, Double.NaN, Double.NaN);
    }

    private static boolean needs(double value, boolean wasActive, double on, double off) {
        return Double.isFinite(value) && value >= 0 && value <= 1 && value >= (wasActive ? off : on);
    }

    private static void offerNeed(State state, String topic, boolean active, int priority, long now) {
        if (active && elapsed(now, state.topics.getOrDefault(topic, Long.MIN_VALUE), NEED_REMINDER))
            offer(state, topic, priority, now);
    }

    public String observe(String key, long now, double health, double playerHealth, boolean bleeding,
                          int zombies, boolean rain, boolean dark, boolean corpses,
                          double hunger, double thirst, double fatigue) {
        if (key == null || key.isBlank() || now < 0 || !Double.isFinite(health + playerHealth)) return "";
        // Refresh insertion order; never retain an unbounded population of world NPCs.
        State state = states.remove(key);
        if (state == null) state = new State();
        states.put(key, state);
        while (states.size() > 128) states.remove(states.keySet().iterator().next());
        if (state.pending != null && now > state.expires) state.pending = null;
        if (zombies <= 0 && ("react_zombies".equals(state.pending) || "react_horde".equals(state.pending)))
            state.pending = null; // A target lost from sight must not produce a stale warning.
        // Explicit policy on the game's 0..1 scales, with hysteresis near the threshold.
        boolean hungry = needs(hunger, state.hungry, .25, .15);
        boolean thirsty = needs(thirst, state.thirsty, .20, .10);
        boolean tired = needs(fatigue, state.tired, .60, .45);
        if (("react_hunger".equals(state.pending) && !hungry)
                || ("react_thirst".equals(state.pending) && !thirsty)
                || ("react_fatigue".equals(state.pending) && !tired)) state.pending = null;

        if ((bleeding && !state.bleeding) || (!Double.isNaN(state.health) && health <= state.health - 3))
            offer(state, "react_injury", 100, now);
        if (!Double.isNaN(state.playerHealth) && playerHealth <= state.playerHealth - 3)
            offer(state, "react_player_injury", 95, now);
        if (zombies >= 5 && state.zombies < 5) offer(state, "react_horde", 90, now);
        else if (zombies > 0 && state.zombies == 0) offer(state, "react_zombies", 80, now);
        if (corpses && !state.corpses && zombies == 0) offer(state, "react_corpses", 40, now);
        if (rain && !state.rain && zombies == 0) offer(state, "react_rain", 20, now);
        if (dark && !state.dark && zombies == 0) offer(state, "react_dark", 10, now);
        if (zombies == 0 && !bleeding) {
            offerNeed(state, "react_thirst", thirsty, 60, now);
            offerNeed(state, "react_hunger", hungry, 55, now);
            offerNeed(state, "react_fatigue", tired, 50, now);
        }

        state.health = health;
        state.playerHealth = playerHealth;
        state.bleeding = bleeding;
        state.zombies = zombies;
        state.rain = rain;
        state.dark = dark;
        state.corpses = corpses;
        state.hungry = hungry;
        state.thirsty = thirsty;
        state.tired = tired;
        // Routine chatter must not compete with an ongoing fight or bleeding.
        if ((zombies > 0 || bleeding) && state.pending != null && state.priority < 80) state.pending = null;
        if (state.pending == null || !elapsed(now, lastGroupSpeech, GROUP_COOLDOWN)
                || !elapsed(now, state.lastSpeech, NPC_COOLDOWN)) return "";
        String result = state.pending;
        state.pending = null;
        state.lastSpeech = now;
        state.topics.put(result, now);
        lastGroupSpeech = now;
        return result;
    }
}
