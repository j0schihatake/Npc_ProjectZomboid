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
            new Template("watch", "Сторожа", "guards", 6, 30),
            new Template("militia", "Ополчение", "guards", 12, 40),
            new Template("bikers", "Байкеры", "raiders", 6, 70),
            new Template("cult", "Культисты", "raiders", 8, 85),
            new Template("rangers", "Рейнджеры", "scouts", 5, 15),
            new Template("doctors", "Врачи", "support", 4, 0),
            new Template("mechanics", "Механики", "support", 5, 10),
            new Template("miners", "Шахтёры", "workers", 8, 15),
            new Template("loggers", "Лесорубы", "workers", 7, 15),
            new Template("fishermen", "Рыбаки", "settlers", 5, 5),
            new Template("hunters", "Охотники", "scouts", 4, 10),
            new Template("nomads", "Кочевники", "civilians", 6, 5),
            new Template("prisoners", "Заключённые", "raiders", 9, 75),
            new Template("soldiers", "Солдаты", "guards", 14, 45),
            new Template("scientists", "Учёные", "support", 4, 5),
            new Template("smugglers", "Контрабандисты", "traders", 6, 35),
            new Template("farmers2", "Хуторяне", "settlers", 7, 5),
            new Template("boatpeople", "Лодочники", "settlers", 5, 10),
            new Template("church", "Прихожане", "civilians", 10, 0),
            new Template("mounted", "Кавалеристы", "scouts", 4, 25),
            new Template("trappers", "Звероловы", "scouts", 4, 15));
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
