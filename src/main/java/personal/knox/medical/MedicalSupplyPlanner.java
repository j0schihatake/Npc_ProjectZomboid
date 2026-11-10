package personal.knox.medical;

import java.util.Map;

/** Chooses a ready aid or a field-craft plan without touching the game world. */
public final class MedicalSupplyPlanner {
    public enum Action { NONE, USE_READY, CRAFT, ABORT_DANGER, DISABLED, NO_TOOL, NO_MATERIALS }

    public record Policy(boolean safe, boolean allowImprovised, int reserveMaterialsPercent) {
        public Policy {
            if (reserveMaterialsPercent < 0 || reserveMaterialsPercent > 100)
                throw new IllegalArgumentException("Invalid material reserve");
        }
    }

    public record Plan(Action action, String aidId, String reason, Map<String, Integer> cost,
                       int projectedMobility) {
        public Plan {
            if (action == null) throw new IllegalArgumentException("Missing action");
            cost = cost == null ? Map.of() : Map.copyOf(cost);
        }

        public boolean actionable() { return action == Action.USE_READY || action == Action.CRAFT; }
    }

    private MedicalSupplyPlanner() {}

    public static Plan plan(PatientMobilityAssessment patient, Map<String, Integer> supplies,
                            boolean hasTool, Policy policy) {
        if (patient == null || supplies == null || policy == null)
            throw new IllegalArgumentException("Missing mobility planning input");
        for (var entry : supplies.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()
                    || entry.getValue() == null || entry.getValue() < 0)
                throw new IllegalArgumentException("Invalid supply count");
        }
        if (!patient.needsAid()) return new Plan(Action.NONE, null, "mobility_sufficient", Map.of(),
                (int) Math.round(patient.mobilityScore()));
        if (!policy.safe()) return new Plan(Action.ABORT_DANGER, null, "danger", Map.of(),
                (int) Math.round(patient.mobilityScore()));

        var ready = MobilityAidCatalog.readyItem(supplies, patient);
        if (ready != null) return new Plan(Action.USE_READY, ready.itemId(), "ready_item", Map.of(),
                ready.projectedMobility(patient));

        if (!policy.allowImprovised())
            return new Plan(Action.DISABLED, null, "improvised_disabled", Map.of(),
                    (int) Math.round(patient.mobilityScore()));

        MobilityAidCatalog.Aid best = null;
        boolean toolBlocked = false;
        for (var aid : MobilityAidCatalog.craftable()) {
            if (!aid.fieldCraftable() || !aid.compatible(patient)) continue;
            if (aid.toolRequired() && !hasTool) { toolBlocked = true; continue; }
            if (!hasMaterials(supplies, aid.materials(), policy.reserveMaterialsPercent())) continue;
            if (best == null || better(aid, best, patient)) best = aid;
        }
        if (best != null)
            return new Plan(Action.CRAFT, best.itemId(), "field_craft", best.materials(),
                    best.projectedMobility(patient));
        if (toolBlocked)
            return new Plan(Action.NO_TOOL, null, "tool_missing", Map.of(),
                    (int) Math.round(patient.mobilityScore()));
        return new Plan(Action.NO_MATERIALS, null, "materials_missing", Map.of(),
                (int) Math.round(patient.mobilityScore()));
    }

    private static boolean better(MobilityAidCatalog.Aid candidate, MobilityAidCatalog.Aid current,
                                  PatientMobilityAssessment patient) {
        int candidateMobility = candidate.projectedMobility(patient);
        int currentMobility = current.projectedMobility(patient);
        if (candidateMobility != currentMobility) return candidateMobility > currentMobility;
        return candidate.materialCount() < current.materialCount();
    }

    private static boolean hasMaterials(Map<String, Integer> supplies, Map<String, Integer> cost,
                                        int reservePercent) {
        for (var entry : cost.entrySet()) {
            int available = supplies.getOrDefault(entry.getKey(), 0);
            int usable = available - (available * reservePercent) / 100;
            if (usable < entry.getValue()) return false;
        }
        return true;
    }
}
