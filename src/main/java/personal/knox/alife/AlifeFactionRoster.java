package personal.knox.alife;

import java.util.List;

/** Twelve own faction templates; no A-Life content is copied. */
public final class AlifeFactionRoster {
    public record Template(String id, String name, String role, int members, int hostility) {}
    public static final List<Template> TEMPLATES = List.of(
            new Template("family", "Семья", "settlers", 5, 10),
            new Template("patrol", "Патруль", "guards", 8, 35),
            new Template("marauders", "Мародёры", "raiders", 7, 80),
            new Template("medics", "Медики", "support", 4, 5),
            new Template("guards", "Охранники", "guards", 10, 25),
            new Template("refugees", "Беженцы", "civilians", 12, 0),
            new Template("farmers", "Фермеры", "settlers", 6, 5),
            new Template("traders", "Торговцы", "traders", 5, 15),
            new Template("scouts", "Разведчики", "scouts", 4, 20),
            new Template("engineers", "Инженеры", "support", 5, 10),
            new Template("raiders", "Рейдеры", "raiders", 9, 90),
            new Template("watch", "Сторожа", "guards", 6, 30));
    private AlifeFactionRoster() {}

    public static Template find(String id) {
        for (var t : TEMPLATES) if (t.id().equals(id)) return t;
        return null;
    }

    public static int count() { return TEMPLATES.size(); }
    public static String roles() { return "settlers,guards,raiders,support,civilians,traders,scouts"; }
    public static String names() {
        var out = new StringBuilder();
        for (var t : TEMPLATES) out.append(t.name()).append(';');
        return out.toString();
    }
}
