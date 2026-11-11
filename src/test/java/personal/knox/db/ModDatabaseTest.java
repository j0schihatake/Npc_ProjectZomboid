package personal.knox.db;

import java.nio.file.Files;

public final class ModDatabaseTest {
    public static void run() throws Exception {
        var dir = Files.createTempDirectory("personal-knox-db");
        var path = dir.resolve("test.db");
        assert ModDatabase.class.getResourceAsStream("/personal/knox/db/schema.sql") != null
                : "SQL schema must ship in the jar";
        assert ModDatabase.class.getResourceAsStream("/personal/knox/db/seed.sql") != null
                : "SQL seed must ship in the jar";
        var db = new ModDatabase(path);
        assert "fallback".equals(db.getSetting("missing", "fallback"));
        db.setSetting("speech", "false");
        db.setSetting("speech", "true");
        assert "true".equals(db.getSetting("speech", "false"));
        db.putData("identity", "npc#1", "Иван|Иванов");
        assert "Иван|Иванов".equals(db.getData("identity", "npc#1", ""));
        assert "Иван|Иванов".equals(db.allData("identity").get("npc#1"));
        assert db.allSettings().contains("speech\ttrue");
        assert "2".equals(db.schemaVersion());
        assert !db.names("ru", "male", "forename").isEmpty() : "RU male forename seed";
        assert !db.names("ru", "female", "forename").isEmpty() : "RU female forename seed";
        assert !db.names("ru", "male", "surname").isEmpty() : "RU surname seed";
        assert !db.names("en", "male", "forename").isEmpty() : "EN forename seed";
        assert db.professionRows().size() >= 10 : "profession seed";
        System.out.println("Mod database tests passed: settings upsert, settings dump and data round-trip.");
    }
}
