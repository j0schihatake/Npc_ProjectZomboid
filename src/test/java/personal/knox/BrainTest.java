package personal.knox;

public final class BrainTest {
    public static void main(String[] args) {
        var api = PersonalKnoxAPI.getInstance();
        assert api.barricadeMissing(0, false) == 4;
        assert api.barricadeMissing(2, false) == 2;
        assert api.barricadeMissing(4, false) == 0;
        assert api.barricadeMissing(0, true) == 0;
        assert api.barricadeStockTarget(100, false) == 4;
        assert api.barricadeStockTarget(100, true) == 8;
        assert api.barricadeStockTarget(1, true) == 2;
        assert api.barricadeStockTarget(0, false) == 0;
        assert api.barricadeStatus(0, true, false, 0).equals("done");
        assert api.barricadeStatus(0, false, false, 0).equals("interrupted");
        assert api.barricadeStatus(1, true, true, 0).equals("danger");
        assert api.barricadeStatus(1, true, false, 1.99).equals("work");
        assert api.barricadeStatus(1, true, false, 2).equals("stalled");
        assert api.barricadeStatus(1, true, false, Double.NaN).equals("invalid");
        try { api.barricadeMissing(-1, false); throw new AssertionError("Invalid plank count accepted"); }
        catch (IllegalArgumentException expected) { }
        int rest = 0, work = 0, recreation = 0;
        for (int hour = 0; hour < 24; hour++) {
            assert api.baseSchedule(hour, "GUARD").equals("GUARD") : "Never remove guard hours";
            var schedule = api.baseSchedule(hour, "ANY");
            if (schedule.equals("REST")) rest++;
            else if (schedule.equals("WORK")) work++;
            else if (schedule.equals("RECREATION")) recreation++;
            else throw new AssertionError("Unknown schedule");
        }
        assert rest == 8 && work == 9 && recreation == 7;
        assert api.baseSchedule(12, "WORK").equals("RECREATION");
        assert api.baseSchedule(22, "ANY").equals("REST");
        try { api.baseSchedule(24, "ANY"); throw new AssertionError("Invalid hour accepted"); }
        catch (IllegalArgumentException expected) { }
        assert !api.allowRetreat(true, false) : "A squad member must not abandon the squad";
        assert api.allowRetreat(true, true) : "Group retreat must be possible";
        assert api.allowRetreat(false, false) : "A lone NPC may retreat";
        assert api.condition(0.1, 5, 90).equals("low_trust");
        assert api.condition(0.1, 60, 90).equals("low_morale");
        assert api.condition(0.8, 60, 90).equals("brave");
        int worried = api.chooseDialogue("status", 0.1, 60, 50, 0);
        int neutral = api.chooseDialogue("status", 0.8, 60, 50, 0);
        assert worried != neutral;
        assert api.dialogueText(worried).matches(".*[А-Яа-яЁё].*") : "Russian dialogue required";
        assert api.chooseDialogue("missing", 0.5, 50, 50, 0) == -1;
        assert api.chooseDialogue("status", 0.5, 50, 50, -1) > 0;
        assert api.targetLevel(0,0,1,3,4,0,5).equals("below");
        assert api.targetLevel(0,0,1,3,4,1,5).equals("same_floor");
        assert api.targetLevel(0,0,1,3,4,2,5).equals("above");
        assert api.targetLevel(0,0,1,30,40,0,5).equals("out_of_range");
        assert api.targetLevel(0,0,1,0,0,0,-1).equals("unknown");
        assert api.supplyPlan(0,0,0,false,true,false).equals("wait") : "No looting during nearby danger";
        assert api.supplyPlan(0,0,0,false,false,true).equals("wait") : "No looting while overloaded";
        assert api.supplyPlan(0,0,0,true,false,false).equals("essentials");
        assert api.supplyPlan(2,1,1,false,false,false).equals("essentials") : "Keep at least two clean dressings";
        assert api.supplyPlan(2,1,2,false,false,false).equals("weapons");
        assert api.supplyPlan(2,1,2,true,false,false).equals("none");
        assert api.supplyCategoryNeeded("medicine",2,1,0,true);
        assert !api.supplyCategoryNeeded("food",2,1,0,true);
        assert !api.supplyCategoryNeeded("water",2,1,0,true);
        assert !api.supplyCategoryNeeded("weapons",2,1,0,true);
        assert api.supplyCategoryNeeded("food",1,1,2,true);
        assert api.supplyCategoryNeeded("water",2,0,2,true);
        assert api.supplyCategoryNeeded("weapons",2,1,2,false);
        assert !api.supplyCategoryNeeded(null,0,0,0,false);
        assert !api.supplyCategoryNeeded("tools",0,0,0,false);
        assert api.isCleanBandage("Base.RippedSheets",true,false);
        assert api.isCleanBandage("Base.AlcoholBandage",true,false);
        assert !api.isCleanBandage("Base.RippedSheetsDirty",true,false);
        assert !api.isCleanBandage("Base.Bandage",true,true);
        assert !api.isCleanBandage("Base.Bandage",false,false);
        assert api.chooseDialogue("plan_supplies", 0.1, 10, 10, 0) > 0 : "Fallback to default template";
        assert api.chooseDialogue("plan_house", 0.5, 50, 50, 0) > 0;
        assert api.chooseDialogue("plan_barricade", 0.5, 50, 50, 0) > 0;
        assert api.chooseDialogue("help_call", 0.5, 50, 50, 0) > 0 : "Help call dialogue required";
        assert api.chooseDialogue("help_rush", 0.5, 50, 50, 0) > 0 : "Help rush dialogue required";
        assert api.chooseDialogue("smalltalk", 0.5, 50, 50, 0) > 0 : "Smalltalk dialogue required";
        assert api.chooseDialogue("find_valuable", 0.5, 50, 50, 0) > 0 : "Find dialogue required";
        assert api.groupChoice(1, 5, 1, 0).equals("hold") : "Weighted consensus";
        assert api.groupChoice(1, 1, 5, 0).equals("retreat") : "Tie retreats";
        assert api.groupChoice(1, 5, 1, 1).equals("attack") : "Leader override";
        assert api.itemObjection(0, 0.7, 0.1, 100, 2, 0) > 0 : "Hungry NPC objects";
        assert api.itemObjection(0, 0.7, 0.1, 100, 0, 0) == 0 : "Full slots do not object";
        var mobility = api.mobilityPlan(20, true, false, 0.4, true, true,
                "Base.Plank=2;Base.RippedSheets=2;Base.Nails=3", true, true, true, 0);
        assert mobility.startsWith("action=CRAFT aid=PersonalKnox.Crutch projected=45 reason=field_craft") : mobility;
        var mobilityReady = api.mobilityPlan(20, true, false, 0.4, true, true,
                "PersonalKnox.Crutch=1;Base.Plank=2", false, true, true, 0);
        assert mobilityReady.startsWith("action=USE_READY aid=PersonalKnox.Crutch") : mobilityReady;
        var mobilityBlocked = api.mobilityPlan(20, true, false, 0.4, true, true,
                "Base.Plank=2;Base.RippedSheets=2;Base.Nails=3", true, false, true, 0);
        assert mobilityBlocked.startsWith("action=ABORT_DANGER") : mobilityBlocked;
        try { api.mobilityPlan(20, true, false, 0.4, true, true, "bad", true, true, true, 0); throw new AssertionError("Bad supply accepted"); }
        catch (IllegalArgumentException expected) { }
        ReactionTest.run();
        SupplyEligibilityTest.run();
        KnoxTransferProbeTest.run();
        KnoxRosterTest.run();
        ThreatDirectionTest.run();
        WeaponChoiceTest.run();
        PlayerCareTest.run();
        UpkeepPolicyTest.run();
        HelpPolicyTest.run();
        GroupDecisionTest.run();
        ShotPriorityTest.run();
        LineOfFireTest.run();
        TransportPolicyTest.run();
        SurveyTest.run();
        ReloadPolicyTest.run();
        LootBagTest.run();
        ItemNeedPolicyTest.run();
        DiplomacyTest.run();
        EvacuationPolicyTest.run();
        personal.knox.base.BaseTest.run();
        personal.knox.medical.FieldMobilityTest.run();
        personal.knox.quest.QuestTest.run();
        personal.knox.quest.QuestLogTest.run();
        personal.knox.quest.SiegePolicyTest.run();
        personal.knox.quest.QuestDeliveryTest.run();
        personal.knox.quest.QuestBoardTest.run();
        personal.knox.quest.DialogueSessionTest.run();
        personal.knox.quest.QuestFactoryTest.run();
        personal.knox.quest.QuestServiceTest.run();
        personal.knox.quest.QuestAssignmentTest.run();
        personal.knox.npc.own.OwnNpcLayerTest.run();
        personal.knox.npc.own.OwnNpcScheduleTest.run();
        personal.knox.npc.own.OwnNpcBodyAdapterTest.run();
        personal.knox.npc.own.OwnNpcCodecTest.run();
        personal.knox.npc.own.OwnNpcPopulationCodecTest.run();
        personal.knox.npc.own.OwnNpcTargetZCodecTest.run();
        personal.knox.npc.own.OwnNpcTraversalTest.run();
        personal.knox.npc.own.OwnNpcPathfinderTest.run();
        personal.knox.npc.own.OwnNpcTraversalGridTest.run();
        personal.knox.npc.own.OwnNpcCombatTest.run();
        personal.knox.npc.own.OwnNpcToolUseTest.run();
        personal.knox.npc.own.OwnNpcDismantleJobTest.run();
        personal.knox.npc.own.OwnNpcPopulationTest.run();
        personal.knox.npc.own.OwnNpcPromotionTest.run();
        personal.knox.npc.own.OwnNpcSpawnerTest.run();
        personal.knox.npc.own.NpcIdentityTest.run();
        personal.knox.economy.ResourceBagTest.run();
        SurrenderPolicyTest.run();
        BetrayalRiskTest.run();
        personal.knox.alife.AlifeTest.run();
        personal.knox.npc.NpcTest.run();
        System.out.println("Java tests passed: squad policy, Russian dialogues, floor/range classification, supply priorities, field mobility aids and bridge.");
    }
}
