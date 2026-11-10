package personal.knox.medical;

import java.util.Map;

public final class FieldMobilityTest {
    private FieldMobilityTest() {}

    private static PatientMobilityAssessment patient(double mobility, boolean leg, boolean walk, boolean sortie) {
        return new PatientMobilityAssessment(mobility, leg, false, 0.4, walk, sortie);
    }

    public static void run() {
        var severe = patient(20, true, true, true);
        var supplies = Map.of("Base.Plank", 2, "Base.RippedSheets", 2, "Base.Nails", 3);
        var plan = MedicalSupplyPlanner.plan(severe, supplies, true,
                new MedicalSupplyPlanner.Policy(true, true, 0));
        assert plan.action() == MedicalSupplyPlanner.Action.CRAFT : "Field crutch must be planned";
        assert plan.aidId().equals(MobilityAidCatalog.CRUTCH) : "Severe limp needs a crutch";
        assert plan.projectedMobility() == 45 : "Crutch must improve mobility";
        assert plan.cost().equals(Map.of("Base.Plank", 1, "Base.RippedSheets", 1, "Base.Nails", 2)) : "Exact cost";
        assert supplies.get("Base.Nails") == 3 : "Planning must not mutate supplies";

        var ready = MedicalSupplyPlanner.plan(severe,
                Map.of(MobilityAidCatalog.CRUTCH, 1, "Base.Plank", 2), false,
                new MedicalSupplyPlanner.Policy(true, true, 0));
        assert ready.action() == MedicalSupplyPlanner.Action.USE_READY : "Ready crutch is used first";
        assert ready.cost().isEmpty() && ready.aidId().equals(MobilityAidCatalog.CRUTCH);

        var noMaterials = MedicalSupplyPlanner.plan(severe, Map.of("Base.Plank", 1), true,
                new MedicalSupplyPlanner.Policy(true, true, 0));
        assert noMaterials.action() == MedicalSupplyPlanner.Action.NO_MATERIALS : "Missing materials are explicit";

        var danger = MedicalSupplyPlanner.plan(severe, supplies, true,
                new MedicalSupplyPlanner.Policy(false, true, 0));
        assert danger.action() == MedicalSupplyPlanner.Action.ABORT_DANGER : "Danger aborts crafting";

        var noTool = MedicalSupplyPlanner.plan(severe, supplies, false,
                new MedicalSupplyPlanner.Policy(true, true, 0));
        assert noTool.action() == MedicalSupplyPlanner.Action.NO_TOOL : "Missing tool is explicit";

        var mild = patient(60, false, true, true);
        var cane = MedicalSupplyPlanner.plan(mild,
                Map.of("Base.Stick", 1, "Base.RippedSheets", 1), false,
                new MedicalSupplyPlanner.Policy(true, true, 0));
        assert cane.action() == MedicalSupplyPlanner.Action.CRAFT && cane.aidId().equals(MobilityAidCatalog.CANE);

        var fine = patient(90, false, true, false);
        var none = MedicalSupplyPlanner.plan(fine, Map.of(), false,
                new MedicalSupplyPlanner.Policy(true, true, 0));
        assert none.action() == MedicalSupplyPlanner.Action.NONE : "Healthy NPC needs no aid";

        var reserved = MedicalSupplyPlanner.plan(severe, supplies, true,
                new MedicalSupplyPlanner.Policy(true, true, 100));
        assert reserved.action() == MedicalSupplyPlanner.Action.NO_MATERIALS : "Full reserve blocks crafting";

        var disabled = MedicalSupplyPlanner.plan(severe, supplies, true,
                new MedicalSupplyPlanner.Policy(true, false, 0));
        assert disabled.action() == MedicalSupplyPlanner.Action.DISABLED : "Improvised aid can be disabled";

        var splint = MedicalSupplyPlanner.plan(patient(10, true, false, true),
                Map.of("Base.Plank", 2, "Base.RippedSheets", 2, "Base.DuctTape", 1), true,
                new MedicalSupplyPlanner.Policy(true, true, 0));
        assert splint.action() == MedicalSupplyPlanner.Action.CRAFT : "Non-walking patient gets a splint";
        assert splint.aidId().equals(MobilityAidCatalog.SPLINT);

        try { new PatientMobilityAssessment(-1, true, false, 0, true, false); throw new AssertionError("Bad mobility accepted"); }
        catch (IllegalArgumentException expected) { }
        try { new MedicalSupplyPlanner.Policy(true, true, 101); throw new AssertionError("Bad reserve accepted"); }
        catch (IllegalArgumentException expected) { }
        try { MedicalSupplyPlanner.plan(null, Map.of(), true, new MedicalSupplyPlanner.Policy(true, true, 0)); throw new AssertionError("Null patient accepted"); }
        catch (IllegalArgumentException expected) { }
        try { MedicalSupplyPlanner.plan(severe, Map.of("Base.Plank", -1), true, new MedicalSupplyPlanner.Policy(true, true, 0)); throw new AssertionError("Negative supply accepted"); }
        catch (IllegalArgumentException expected) { }

        System.out.println("Field mobility tests passed: crutch craft, ready item, danger abort, missing tool/material, cane, splint, reserve and validation.");
    }
}
