package personal.knox.npc.own;

/** Small deterministic phrase table for squad alerts. */
public final class OwnNpcAlert {
    private static final String[] PHRASES = {
        "Оружие к бою!", "Вижу врага!", "Приготовиться!", "Занять позиции!", "Не зевать!"
    };
    private OwnNpcAlert() {}

    public static String phrase(int roll) {
        int index = Math.floorMod(roll, PHRASES.length);
        return PHRASES[index];
    }
}
