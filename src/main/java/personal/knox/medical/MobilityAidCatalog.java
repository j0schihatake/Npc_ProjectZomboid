package personal.knox.medical;

import java.util.List;
import java.util.Map;

/** Deterministic field-craftable mobility aids and their material costs. */
public final class MobilityAidCatalog {
    public static final String CRUTCH = "PersonalKnox.Crutch";
    public static final String CANE = "PersonalKnox.Cane";
    public static final String SPLINT = "PersonalKnox.Splint";
    public static final String SUPPORT = "PersonalKnox.ImprovisedSupport";

    public record Aid(String itemId, int mobilityBoost, int minMobility, int maxMobility,
                      boolean legInjuryOnly, boolean fieldCraftable, boolean toolRequired,
                      Map<String, Integer> materials) {
        public Aid {
            if (itemId == null || itemId.isBlank())
                throw new IllegalArgumentException("Missing aid id");
            if (mobilityBoost <= 0 || minMobility < 0 || maxMobility > 100 || minMobility > maxMobility)
                throw new IllegalArgumentException("Invalid aid range");
            if (materials == null) throw new IllegalArgumentException("Missing materials");
            for (var entry : materials.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()
                        || entry.getValue() == null || entry.getValue() <= 0)
                    throw new IllegalArgumentException("Invalid material cost");
            }
            materials = Map.copyOf(materials);
        }

        public boolean compatible(PatientMobilityAssessment patient) {
            return patient != null && patient.mobilityScore() >= minMobility
                    && patient.mobilityScore() <= maxMobility
                    && (!legInjuryOnly || patient.legInjured());
        }

        public int projectedMobility(PatientMobilityAssessment patient) {
            return Math.min(100, (int) Math.round(patient.mobilityScore()) + mobilityBoost);
        }

        public int materialCount() {
            int total = 0;
            for (int value : materials.values()) total += value;
            return total;
        }
    }

    private static final List<Aid> CRAFTABLE = List.of(
            new Aid(CRUTCH, 25, 0, 55, false, true, true,
                    Map.of("Base.Plank", 1, "Base.RippedSheets", 1, "Base.Nails", 2)),
            new Aid(CANE, 15, 35, 75, false, true, false,
                    Map.of("Base.Stick", 1, "Base.RippedSheets", 1)),
            new Aid(SPLINT, 18, 0, 50, true, true, true,
                    Map.of("Base.Plank", 2, "Base.RippedSheets", 2, "Base.DuctTape", 1)),
            new Aid(SUPPORT, 10, 20, 65, false, true, false,
                    Map.of("Base.Stick", 2, "Base.Rope", 1))
    );

    private MobilityAidCatalog() {}

    public static List<Aid> craftable() { return CRAFTABLE; }

    /** Best already available aid, independent of crafting materials. */
    public static Aid readyItem(Map<String, Integer> supplies, PatientMobilityAssessment patient) {
        if (supplies == null) throw new IllegalArgumentException("Missing supplies");
        Aid best = null;
        for (var aid : CRAFTABLE) {
            if (!aid.compatible(patient) || supplies.getOrDefault(aid.itemId(), 0) <= 0) continue;
            if (best == null || aid.mobilityBoost() > best.mobilityBoost()) best = aid;
        }
        return best;
    }
}
