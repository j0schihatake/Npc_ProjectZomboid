package personal.knox.alife;

/** Chooses an incoming contact by relation: enemy warns, neutral asks, ally chats. */
public final class RadioDirector {
    private static final String[] ALLY_CHAT = {
            "Слышал, у вас там спокойно? Мы рядом.",
            "Держитесь. Если что — мы на частоте.",
            "Анекдот: зомби заходит в бар, а бармен ему — вы заняты?"
    };

    private RadioDirector() {}

    public static RadioContact contact(String groupId, int relation, int roll, long atMillis) {
        if (relation < -100 || relation > 100) throw new IllegalArgumentException("Invalid relation");
        if (roll < 0 || roll > 99 || atMillis < 0) throw new IllegalArgumentException("Invalid contact roll");
        if (relation < -20) {
            return new RadioContact(groupId, RadioContact.Stance.ENEMY, RadioContact.Kind.WARNING,
                    "Отойдите от нашей территории. Один раз предупредили.", atMillis);
        }
        if (relation < 20) {
            return new RadioContact(groupId, RadioContact.Stance.NEUTRAL, RadioContact.Kind.QUESTION,
                    "Вы кто такие и что вам здесь нужно?", atMillis);
        }
        var kind = roll < 20 ? RadioContact.Kind.STORY : roll < 60 ? RadioContact.Kind.JOKE : RadioContact.Kind.CHAT;
        return new RadioContact(groupId, RadioContact.Stance.ALLY, kind, ALLY_CHAT[roll % ALLY_CHAT.length], atMillis);
    }
}
