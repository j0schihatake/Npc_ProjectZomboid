package personal.knox;

public final class ReactionTest {
    private static String observe(ReactionDirector director, String key, long time, double hp, int enemies, boolean rain) {
        return director.observe(key,time,hp,100,false,enemies,rain,false,false);
    }

    private static String need(ReactionDirector director, long time, int enemies, double hunger, double thirst, double fatigue) {
        return director.observe("need",time,100,100,false,enemies,false,false,false,hunger,thirst,fatigue);
    }

    public static void run() {
        var director = new ReactionDirector();
        assert observe(director,"a",100_000,100,0,false).isEmpty() : "No unsolicited generic chatter";
        assert observe(director,"a",101_000,100,1,true).equals("react_zombies") : "Danger beats weather";
        assert observe(director,"a",400_000,100,1,true).isEmpty() : "Steady conditions aren't new events";
        assert observe(director,"a",401_000,95,1,true).equals("react_injury") : "Health loss is an event";
        assert observe(director,"b",402_000,100,5,false).isEmpty() : "Squad shares an anti-spam cooldown";
        assert observe(director,"b",422_000,100,5,false).equals("react_horde") : "A fresh pending warning may be delivered";

        director.reset();
        assert observe(director,"a",100_000,100,0,true).equals("react_rain");
        assert observe(director,"a",130_000,100,1,true).isEmpty() : "One NPC may not spam different topics";
        assert observe(director,"a",191_000,100,1,true).isEmpty() : "Old danger warnings must expire";
        assert observe(director,"a",200_000,100,0,false).isEmpty();
        assert observe(director,"a",201_000,100,1,false).equals("react_zombies");
        assert observe(director,"a",300_000,100,0,false).isEmpty();
        assert observe(director,"a",301_000,100,1,false).isEmpty() : "Same-topic cooldown is longer";

        director.reset();
        assert director.observe("a",100_000,100,100,false,0,true,true,true).equals("react_corpses");
        director.reset();
        assert director.observe("a",100_000,100,100,false,0,false,false,false).isEmpty();
        assert director.observe("a",101_000,100,96,false,0,false,false,false).equals("react_player_injury");
        director.reset();
        for (int i=0;i<300;i++) observe(director,"npc"+i,100_000+i,100,0,false);
        assert director.trackedCount()==128 : "NPC tracking must remain bounded";
        director.reset();
        assert director.trackedCount()==0;
        assert director.observe("",100_000,100,100,false,0,true,false,false).isEmpty();
        assert director.observe("a",100_000,Double.NaN,100,false,0,true,false,false).isEmpty();
        director.reset();
        assert need(director,100_000,0,0,0,0).isEmpty();
        assert need(director,101_000,0,Double.NaN,-1,Double.POSITIVE_INFINITY).isEmpty();
        assert need(director,102_000,0,.3,0,0).equals("react_hunger");
        assert need(director,202_000,0,.3,0,0).isEmpty() : "Do not constantly repeat an unmet need";
        assert need(director,402_000,0,.2,0,0).equals("react_hunger") : "Hysteresis avoids threshold flicker";
        assert need(director,500_000,0,.1,0,0).isEmpty();
        assert need(director,800_000,0,.2,0,0).isEmpty() : "Recovered hunger must cross the start threshold again";
        assert need(director,801_000,0,.25,0,0).equals("react_hunger");
        director.reset();
        assert need(director,100_000,0,.5,.3,.8).equals("react_thirst") : "Thirst is most urgent among routine needs";
        assert need(director,120_000,0,.5,0,0).isEmpty();
        assert need(director,191_000,0,.5,0,0).equals("react_hunger") : "Still-current needs survive a competing topic";
        director.reset();
        assert observe(director,"a",100_000,100,0,true).equals("react_rain");
        assert need(director,101_000,0,.5,0,0).isEmpty();
        assert need(director,121_000,0,0,0,0).isEmpty() : "Do not say hungry after the NPC ate";
        director.reset();
        assert need(director,100_000,1,.5,.5,.9).equals("react_zombies");
        assert need(director,200_000,1,.5,.5,.9).isEmpty() : "No need chatter in an ongoing fight";
        assert need(director,300_000,0,0,0,.9).equals("react_fatigue");
        director.reset();
        assert director.observe("need",100_000,100,100,true,0,false,false,false,.8,.8,.9).equals("react_injury");
        assert director.observe("need",300_000,100,100,true,0,false,false,false,.8,.8,.9).isEmpty() : "Bleeding suppresses routine needs";
        var api=PersonalKnoxAPI.getInstance();
        for (String topic : new String[]{"react_zombies","react_horde","react_injury","react_player_injury","react_rain","react_dark","react_corpses","react_hunger","react_thirst","react_fatigue"}) {
            assert api.chooseDialogue(topic,0.1,20,80,0)>0 : "Missing Russian reaction: "+topic;
        }
        System.out.println("World reaction tests passed: perception, needs, priority, cooldowns, recovered needs, hysteresis, combat suppression, bounded memory.");
    }
}
