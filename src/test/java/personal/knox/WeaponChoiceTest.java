package personal.knox;

import static personal.knox.WeaponChoice.Decision.*;
import static personal.knox.WeaponChoice.Role.*;

public final class WeaponChoiceTest {
    private static WeaponChoice.Profile melee(double damage, int condition, int skill) {
        return new WeaponChoice.Profile(MELEE,damage,damage,1,condition,10,1,1,skill,false);
    }
    private static WeaponChoice.Decision decide(WeaponChoice.Profile candidate, WeaponChoice.Profile old,
                                               boolean spare, double free) {
        return WeaponChoice.decide(candidate,old,true,false,spare,1,free,.1);
    }
    public static void run() {
        var knife = melee(1,10,5);
        var stronger = melee(2,10,5);
        assert decide(stronger,knife,false,10) == UPGRADE;
        assert decide(knife,stronger,false,10) == LEAVE : "Do not replace a better item";
        assert decide(knife,knife,false,10) == LEAVE : "Identical equipment is not an upgrade";
        assert decide(melee(1.05,10,5),knife,false,10) == LEAVE : "Respect the caller's improvement margin";
        assert decide(knife,stronger,true,10) == RESERVE : "A real free attachment may hold a reserve";
        assert decide(stronger,knife,true,.5) == LEAVE : "Free attachment never overrides capacity";
        assert decide(melee(100,0,10),knife,true,10) == LEAVE : "Broken high-damage item is unusable";
        assert decide(melee(1.4,1,5),knife,false,10) == LEAVE : "Condition matters, not raw damage alone";
        assert decide(melee(1.1,10,0),melee(1,10,10),false,10) == LEAVE : "Use the actual skill for each weapon";
        assert WeaponChoice.decide(knife,null,false,true,false,1,1,0) == EQUIP;
        assert WeaponChoice.decide(knife,null,false,false,false,1,10,0) == LEAVE : "No imaginary slot";
        var gun = new WeaponChoice.Profile(RANGED,10,10,2,10,10,2,20,5,true);
        var empty = new WeaponChoice.Profile(RANGED,10,10,2,10,10,2,20,5,false);
        assert decide(gun,knife,false,10) == LEAVE : "Melee and ranged scores are not comparable";
        assert decide(gun,knife,true,10) == RESERVE;
        assert decide(empty,gun,true,10) == LEAVE : "No compatible ammunition/reload source";
        assert WeaponChoice.utility(gun)>0 : "Ammo availability may be reserves, not a loaded chamber";
        assert WeaponChoice.utility(knife)>0 : "Melee does not need ammunition";
        assert WeaponChoice.decide(stronger,knife,true,false,false,1,10,Double.NaN) == LEAVE;
        assert WeaponChoice.decide(stronger,knife,true,false,false,Double.NaN,10,0) == LEAVE;
        assert WeaponChoice.utility(new WeaponChoice.Profile(MELEE,1,2,0,10,10,1,1,5,false))==0;
        assert WeaponChoice.utility(new WeaponChoice.Profile(MELEE,1,2,1,10,10,Double.NaN,1,5,false))==0;
        assert WeaponChoice.utility(new WeaponChoice.Profile(MELEE,1,Double.POSITIVE_INFINITY,1,10,10,1,1,5,false))==0;
        assert WeaponChoice.utility(new WeaponChoice.Profile(MELEE,1,2,1,10,10,0,1,5,false))>0 : "Weight mods may set zero weight";
        assert decide(knife,melee(1,0,5),false,10)==UPGRADE : "Replace a broken incumbent";
        var unknownOld = new WeaponChoice.Profile(MELEE,1,2,0,10,10,1,1,5,false);
        assert decide(stronger,unknownOld,false,10)==LEAVE : "Missing old stats must not count as a broken weapon";
        assert decide(stronger,unknownOld,true,10)==RESERVE : "A real spare slot does not replace the unknown incumbent";
        var overflowOld = new WeaponChoice.Profile(MELEE,Double.MAX_VALUE,Double.MAX_VALUE,1,10,10,1,1,5,false);
        assert decide(stronger,overflowOld,false,10)==LEAVE : "Out-of-range utility is not proof of a weak incumbent";
        System.out.println("Weapon policy tests passed: roles, condition, skills, ammo/reserves, real slots, capacity, equal items and invalid data.");
    }
}
