package personal.knox.npc.own;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/** Deterministic name/profession/skill database for own NPCs. */
public final class NpcIdentityDatabase {
    private record Pool(String culture, String[] male, String[] female,
                        String[] maleSurnames, String[] femaleSurnames) {}
    private record Profession(String key, String label, String[] perks, int[] levels) {}

    private static final List<Pool> POOLS = List.of(
            new Pool("ru",
                    new String[]{"Александр","Иван","Дмитрий","Сергей","Андрей","Михаил","Николай","Павел","Роман","Егор","Кирилл","Артём","Максим","Виктор","Олег","Глеб",
                            "Владимир","Алексей","Аркадий","Борис","Вадим","Вячеслав","Григорий","Даниил","Илья","Лев","Леонид","Матвей","Никита","Пётр","Степан","Фёдор",
                            "Арсений","Валерий","Василий","Всеволод","Герман","Денис","Захар","Игорь","Константин","Марк","Остап","Платон","Савелий","Тимофей","Эдуард","Яков"},
                    new String[]{"Анна","Мария","Елена","Ольга","Наталья","Ирина","Светлана","Татьяна","Дарья","Ксения","Вера","Полина","Алиса","Юлия","Софья","Екатерина",
                            "Анастасия","Валентина","Варвара","Галина","Евгения","Зоя","Инна","Лариса","Лидия","Любовь","Маргарита","Надежда","Нина","Раиса","Тамара","Элина",
                            "Алина","Алла","Василиса","Диана","Ева","Жанна","Злата","Карина","Кристина","Милана","Олеся","Регина","Снежана","Ульяна","Эвелина","Яна"},
                    new String[]{"Смирнов","Иванов","Кузнецов","Попов","Соколов","Лебедев","Козлов","Новиков","Морозов","Петров","Волков","Соловьёв","Васильев","Зайцев","Павлов","Семёнов",
                            "Алексеев","Андреев","Антонов","Афанасьев","Баранов","Белов","Борисов","Виноградов","Воробьёв","Григорьев","Данилов","Егоров","Ершов","Жуков","Киселёв","Орлов",
                            "Агеев","Аксёнов","Артемьев","Балашов","Беляев","Богданов","Большаков","Быков","Ветров","Гаврилов","Гусев","Дроздов","Ефимов","Захаров","Ильин","Карпов"},
                    new String[]{"Смирнова","Иванова","Кузнецова","Попова","Соколова","Лебедева","Козлова","Новикова","Морозова","Петрова","Волкова","Соловьёва","Васильева","Зайцева","Павлова","Семёнова",
                            "Алексеева","Андреева","Антонова","Афанасьева","Баранова","Белова","Борисова","Виноградова","Воробьёва","Григорьева","Данилова","Егорова","Ершова","Жукова","Киселёва","Орлова",
                            "Агеева","Аксёнова","Артемьева","Балашова","Беляева","Богданова","Большакова","Быкова","Ветрова","Гаврилова","Гусева","Дроздова","Ефимова","Захарова","Ильина","Карпова"}),
            new Pool("pl",
                    new String[]{"Jakub","Piotr","Krzysztof","Andrzej","Tomasz","Jan","Michał","Marcin","Paweł","Grzegorz","Adam","Łukasz","Mateusz","Rafał","Bartosz","Kamil"},
                    new String[]{"Anna","Maria","Katarzyna","Małgorzata","Agnieszka","Barbara","Ewa","Krystyna","Elżbieta","Zofia","Magdalena","Joanna","Aleksandra","Natalia","Monika","Julia"},
                    new String[]{"Nowak","Kowalski","Wiśniewski","Wójcik","Kowalczyk","Kamiński","Lewandowski","Zieliński","Szymański","Woźniak","Dąbrowski","Kozłowski","Jankowski","Mazur","Wojciechowski","Krawczyk"},
                    new String[]{"Nowak","Kowalska","Wiśniewska","Wójcik","Kowalczyk","Kamińska","Lewandowska","Zielińska","Szymańska","Woźniak","Dąbrowska","Kozłowska","Jankowska","Mazur","Wojciechowska","Krawczyk"}),
            new Pool("it",
                    new String[]{"Luca","Marco","Andrea","Giuseppe","Giovanni","Antonio","Matteo","Francesco","Alessandro","Davide","Simone","Stefano","Paolo","Riccardo","Federico","Lorenzo"},
                    new String[]{"Giulia","Sofia","Martina","Chiara","Francesca","Alessia","Elena","Sara","Laura","Valentina","Anna","Beatrice","Camilla","Elisa","Ilaria","Silvia"},
                    new String[]{"Rossi","Russo","Ferrari","Esposito","Bianchi","Romano","Colombo","Ricci","Marino","Greco","Bruno","Gallo","Conti","De Luca","Costa","Giordano"},
                    new String[]{"Rossi","Russo","Ferrari","Esposito","Bianchi","Romano","Colombo","Ricci","Marino","Greco","Bruno","Gallo","Conti","De Luca","Costa","Giordano"}),
            new Pool("en",
                    new String[]{"James","John","Robert","Michael","William","David","Richard","Joseph","Thomas","Charles","Daniel","Matthew","Anthony","Mark","Steven","Paul"},
                    new String[]{"Mary","Patricia","Jennifer","Linda","Elizabeth","Barbara","Susan","Jessica","Sarah","Karen","Nancy","Lisa","Betty","Margaret","Sandra","Ashley"},
                    new String[]{"Smith","Johnson","Williams","Brown","Jones","Garcia","Miller","Davis","Rodriguez","Martinez","Hernandez","Lopez","Gonzalez","Wilson","Anderson","Thomas"},
                    new String[]{"Smith","Johnson","Williams","Brown","Jones","Garcia","Miller","Davis","Rodriguez","Martinez","Hernandez","Lopez","Gonzalez","Wilson","Anderson","Thomas"}),
            new Pool("es",
                    new String[]{"José","Juan","Carlos","Luis","Miguel","Javier","Pedro","Antonio","Manuel","Francisco","Diego","Alejandro","Pablo","Sergio","Rafael","Fernando"},
                    new String[]{"María","Carmen","Lucía","Ana","Isabel","Laura","Marta","Elena","Sofía","Paula","Cristina","Rosa","Pilar","Teresa","Julia","Valeria"},
                    new String[]{"García","Rodríguez","González","Fernández","López","Martínez","Sánchez","Pérez","Gómez","Martín","Jiménez","Ruiz","Hernández","Díaz","Moreno","Álvarez"},
                    new String[]{"García","Rodríguez","González","Fernández","López","Martínez","Sánchez","Pérez","Gómez","Martín","Jiménez","Ruiz","Hernández","Díaz","Moreno","Álvarez"}),
            new Pool("fr",
                    new String[]{"Jean","Pierre","Michel","André","Philippe","Louis","Nicolas","François","Alain","Julien","Bernard","Marcel","Étienne","Henri","Paul","Luc"},
                    new String[]{"Marie","Jeanne","Françoise","Monique","Catherine","Nathalie","Isabelle","Sylvie","Anne","Valérie","Chantal","Sophie","Claire","Élise","Camille","Léa"},
                    new String[]{"Martin","Bernard","Dubois","Thomas","Robert","Richard","Petit","Durand","Leroy","Moreau","Simon","Laurent","Lefebvre","Michel","Garcia","David"},
                    new String[]{"Martin","Bernard","Dubois","Thomas","Robert","Richard","Petit","Durand","Leroy","Moreau","Simon","Laurent","Lefebvre","Michel","Garcia","David"}),
            new Pool("de",
                    new String[]{"Hans","Peter","Klaus","Wolfgang","Jürgen","Dieter","Manfred","Uwe","Thomas","Michael","Andreas","Stefan","Frank","Bernd","Karl","Otto"},
                    new String[]{"Anna","Ursula","Monika","Petra","Sabine","Helga","Ingrid","Renate","Gisela","Karin","Elke","Heike","Birgit","Christa","Julia","Lena"},
                    new String[]{"Müller","Schmidt","Schneider","Fischer","Weber","Meyer","Wagner","Becker","Schulz","Hoffmann","Koch","Bauer","Richter","Klein","Wolf","Schröder"},
                    new String[]{"Müller","Schmidt","Schneider","Fischer","Weber","Meyer","Wagner","Becker","Schulz","Hoffmann","Koch","Bauer","Richter","Klein","Wolf","Schröder"})
    );

    private static final List<Profession> PROFESSIONS = List.of(
            new Profession("doctor", "Doctor", new String[]{"Doctor","FirstAid"}, new int[]{4,2}),
            new Profession("carpenter", "Carpenter", new String[]{"Carpentry","Maintenance"}, new int[]{4,2}),
            new Profession("farmer", "Farmer", new String[]{"Farming","Cooking"}, new int[]{4,2}),
            new Profession("mechanic", "Mechanic", new String[]{"Mechanics","Metalworking"}, new int[]{4,2}),
            new Profession("police", "Police Officer", new String[]{"Aiming","Reloading"}, new int[]{3,3}),
            new Profession("veteran", "Veteran", new String[]{"Axe","Blunt"}, new int[]{3,3}),
            new Profession("chef", "Chef", new String[]{"Cooking","Maintenance"}, new int[]{4,1}),
            new Profession("fisher", "Fisherman", new String[]{"Fishing","Cooking"}, new int[]{4,2}),
            new Profession("guard", "Security Guard", new String[]{"Sprinting","Fitness"}, new int[]{4,2}),
            new Profession("unemployed", "Unemployed", new String[]{"Strength","Fitness"}, new int[]{1,1})
    );

    private static volatile java.util.List<Pool> DB_POOLS;

    private static volatile java.util.List<Profession> DB_PROFESSIONS;

    private static java.util.List<Profession> professions() {
        var cached = DB_PROFESSIONS;
        if (cached != null) return cached;
        synchronized (NpcIdentityDatabase.class) {
            if (DB_PROFESSIONS != null) return DB_PROFESSIONS;
            var loaded = new java.util.ArrayList<Profession>();
            try {
                for (var row : personal.knox.db.ModDatabase.getInstance().professionRows()) {
                    var perks = row[2].split(",");
                    var levels = row[3].split(",");
                    if (perks.length != levels.length || perks.length == 0) continue;
                    var values = new int[levels.length];
                    for (int i = 0; i < levels.length; i++) values[i] = Integer.parseInt(levels[i]);
                    loaded.add(new Profession(row[0], row[1], perks, values));
                }
            } catch (Throwable ignored) { }
            DB_PROFESSIONS = loaded.isEmpty() ? PROFESSIONS : java.util.List.copyOf(loaded);
            return DB_PROFESSIONS;
        }
    }

    private static java.util.List<Pool> pools() {
        var cached = DB_POOLS;
        if (cached != null) return cached;
        synchronized (NpcIdentityDatabase.class) {
            if (DB_POOLS != null) return DB_POOLS;
            var loaded = new java.util.ArrayList<Pool>();
            try {
                var db = personal.knox.db.ModDatabase.getInstance();
                for (var culture : db.cultures()) {
                    var male = db.names(culture, "male", "forename");
                    var female = db.names(culture, "female", "forename");
                    var maleSurnames = db.names(culture, "male", "surname");
                    var femaleSurnames = db.names(culture, "female", "surname");
                    if (male.size() >= 4 && female.size() >= 4 && maleSurnames.size() >= 4 && femaleSurnames.size() >= 4) {
                        loaded.add(new Pool(culture, male.toArray(String[]::new), female.toArray(String[]::new),
                                maleSurnames.toArray(String[]::new), femaleSurnames.toArray(String[]::new)));
                    }
                }
            } catch (Throwable ignored) { }
            DB_POOLS = loaded.isEmpty() ? POOLS : java.util.List.copyOf(loaded);
            return DB_POOLS;
        }
    }

    private NpcIdentityDatabase() {}

    public static NpcIdentity generate(String id) { return generate(id, false); }

    public static NpcIdentity generate(String id, boolean preferRussian) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Missing id");
        long seed = id.hashCode() * 0x9E3779B97F4A7C15L + id.length() * 0x100000001B3L;
        return generate(seed, preferRussian);
    }

    public static NpcIdentity generate(long seed) { return generate(seed, false); }

    public static NpcIdentity generate(long seed, boolean preferRussian) {
        var random = new SplittableRandom(seed);
        var pools = pools();
        Pool russian = null;
        for (var candidate : pools) if ("ru".equals(candidate.culture())) { russian = candidate; break; }
        var pool = preferRussian && russian != null && random.nextInt(100) < 60
                ? russian : pools.get(random.nextInt(pools.size()));
        boolean female = random.nextBoolean();
        var names = female ? pool.female() : pool.male();
        var surnames = female ? pool.femaleSurnames() : pool.maleSurnames();
        var professionList = professions();
        var profession = professionList.get(random.nextInt(professionList.size()));
        var skills = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < profession.perks().length; i++) skills.put(profession.perks()[i], profession.levels()[i]);
        return new NpcIdentity(pool.culture(), female ? "female" : "male",
                names[random.nextInt(names.length)], surnames[random.nextInt(surnames.length)],
                profession.key(), profession.label(), skills);
    }

    public static List<String> cultures() {
        var result = new ArrayList<String>();
        for (var pool : pools()) result.add(pool.culture());
        return List.copyOf(result);
    }

    public static Map<String, List<String>> namesForTest() {
        var result = new LinkedHashMap<String, List<String>>();
        for (var pool : pools()) {
            var names = new ArrayList<String>();
            names.addAll(List.of(pool.male()));
            names.addAll(List.of(pool.female()));
            result.put(pool.culture(), List.copyOf(names));
        }
        return Map.copyOf(result);
    }
}
