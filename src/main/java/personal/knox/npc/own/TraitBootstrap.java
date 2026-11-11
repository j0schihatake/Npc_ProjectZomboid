package personal.knox.npc.own;

/** Registers custom B42 CharacterTrait resources before scripts are loaded. */
public final class TraitBootstrap {
    private TraitBootstrap() {}

    public static int register() {
        int count = 0;
        try {
            Class<?> trait = Class.forName("zombie.scripting.objects.CharacterTrait");
            var register = trait.getMethod("register", String.class);
            for (int i = 1; i <= 4; i++) {
                register.invoke(null, "personalknox:companions" + i);
                register.invoke(null, "PersonalKnox:Companions" + i);
                count += 2;
            }
        } catch (Throwable ignored) { }
        return count;
    }
}
