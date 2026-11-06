package personal.knox.npc.own;

import java.util.*;
import personal.knox.npc.NpcState;
import personal.knox.npc.NpcNeeds;

public final class OwnNpcScheduleTest {
    private static OwnNpcAgent npc(String id,boolean moving) {
        return new OwnNpcAgent(id,"own",0,0,2,moving?100:0,0,0,
                new NpcState(100,0.2,0.2,0.2,0.7,false,false,NpcState.Role.GUARD,NpcState.Task.GUARD));
    }
    public static void run() {
        var layer=new OwnNpcLayer();
        for(int i=0;i<64;i++) layer.spawn(npc("npc"+i,false));
        assert layer.tick(1,0).isEmpty();
        assert layer.agent("npc0").state().hunger()==0.2 : "No budget means no NPC work";
        var snapshot=layer.snapshotAgents();
        for(var npc:snapshot) assert Math.abs(npc.state().hunger()-(0.2+NpcNeeds.HUNGER_PER_HOUR))<1e-10
                : "Save includes deferred need time";
        Set<String> processed=new HashSet<>();
        for(int i=0;i<64;i++) {
            var events=layer.tick(0,1);
            assert events.size()==1;
            assert processed.add(events.get(0).agentId()) : "No NPC repeats before its peers";
        }
        assert processed.size()==64 : "All 64 NPCs get CPU time under budget 1";
        for(var npc:layer.agents()) assert Math.abs(npc.state().hunger()-snapshot.get(0).state().hunger())<1e-10;
        layer.remove("npc0");
        assert layer.tick(0,1).get(0).agentId().equals("npc1") : "Removal preserves the next turn";

        var movers=new OwnNpcLayer();
        for(int i=0;i<64;i++) movers.spawn(npc("moving"+i,true));
        Map<String,Integer> moves=new HashMap<>();Set<String> needs=new HashSet<>();
        for(int i=0;i<128;i++) {
            var events=movers.tick(0,1);
            assert events.size()==1 : "Two notifications must never exceed budget 1";
            var event=events.get(0);
            if(event.type()==OwnNpcEvent.Type.MOVED) moves.merge(event.agentId(),1,Integer::sum);
            else assert needs.add(event.agentId());
        }
        assert needs.size()==64 && moves.size()==64;
        for(int count:moves.values()) assert count==1 : "No dropped or duplicate movement event";
        movers.tick(0,1); // One NEEDS event, leaving MOVED pending for moving0.
        movers.remove("moving0");
        assert movers.tick(0,1).get(0).agentId().equals("moving1") : "Removed NPC has no stale notification";

        var time=new OwnNpcLayer();time.spawn(npc("old",false));time.setNeedRate(2);
        time.tick(1,0);time.spawn(npc("new",false));time.setNeedRate(4);time.tick(1,0);
        time.tick(0,2);
        assert Math.abs(time.agent("old").state().hunger()-(0.2+6*NpcNeeds.HUNGER_PER_HOUR))<1e-10;
        assert Math.abs(time.agent("new").state().hunger()-(0.2+4*NpcNeeds.HUNGER_PER_HOUR))<1e-10
                : "New NPCs do not age before birth; deferred time retains its original rate";
        time.order("old",OwnNpcOrder.FOLLOW,10,10);time.followTarget(10,10,5);
        var saved=npc("old",false);
        try {time.restore(List.of(saved,saved));throw new AssertionError("Duplicate accepted");}
        catch(IllegalArgumentException expected) { }
        assert time.count()==2 && time.orderOf("old")==OwnNpcOrder.FOLLOW : "Invalid restore is atomic";
        time.restore(List.of(saved));
        assert time.count()==1 && time.orderOf("old")==OwnNpcOrder.HOLD;
        assert time.targetZ("old")==2 && !time.targetZMap().containsKey("__follow__") : "No previous world targets";
        assert time.tick(0,1).get(0).agentId().equals("old");
        System.out.println("Own NPC scheduler: 64-agent fairness, strict budgets, deferred events/time, spawn/remove and atomic restore passed.");
    }
    public static void main(String[] args) { run(); }
}
