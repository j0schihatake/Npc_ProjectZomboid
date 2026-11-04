package personal.knox;

public final class BrainTest {
    public static void main(String[] args) {
        var api = PersonalKnoxAPI.getInstance();
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
        assert api.supplyPlan(2,1,1,false,false,false).equals("weapons");
        assert api.supplyPlan(2,1,1,true,false,false).equals("none");
        assert api.chooseDialogue("plan_supplies", 0.1, 10, 10, 0) > 0 : "Fallback to default template";
        System.out.println("Java tests passed: squad policy, Russian dialogues, floor/range classification, supply priorities.");
    }
}
