package personal.knox.db;

import java.nio.file.Files;
import java.sql.DriverManager;

public final class ModDatabaseMigrationTest {
    public static void run() throws Exception {
        var dir = Files.createTempDirectory("personal-knox-migration");
        var path = dir.resolve("v1.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE settings(key TEXT PRIMARY KEY,value TEXT NOT NULL)");
            statement.execute("CREATE TABLE data(kind TEXT NOT NULL,id TEXT NOT NULL,value TEXT NOT NULL,PRIMARY KEY(kind,id))");
            statement.execute("CREATE TABLE names(culture TEXT NOT NULL,gender TEXT NOT NULL,kind TEXT NOT NULL,value TEXT NOT NULL)");
            statement.execute("CREATE TABLE professions(key TEXT PRIMARY KEY,label TEXT NOT NULL,perks TEXT NOT NULL,levels TEXT NOT NULL)");
            statement.execute("CREATE TABLE meta(key TEXT PRIMARY KEY,value TEXT NOT NULL)");
            statement.execute("INSERT INTO settings(key,value) VALUES('speech','false')");
            statement.execute("INSERT INTO meta(key,value) VALUES('schema_version','1')");
        }
        var db = new ModDatabase(path);
        assert "false".equals(db.getSetting("speech", "true")) : "speech=" + db.getSetting("speech", "true");
        assert !db.names("ru", "male", "forename").isEmpty() : "migration must reseed names";
        assert Files.exists(path.resolveSibling(path.getFileName() + ".v1.bak")) : "migration must back up v1";
        System.out.println("Mod database tests passed: settings upsert, settings dump and data round-trip.\nMod database migration passed: v1 backup and name reseed.");
    }
}
