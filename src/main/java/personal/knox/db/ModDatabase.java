package personal.knox.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/** SQLite store for settings and mod data; schema/seed live next to the code. */
public final class ModDatabase {
    private static volatile ModDatabase instance;
    private static volatile String activeSave = "";
    private final Path path;
    private Connection connection;

    public ModDatabase(Path path) { this.path = path; }

    public static ModDatabase getInstance() {
        var save = currentSave();
        var local = instance;
        if (local == null || !save.equals(activeSave)) synchronized (ModDatabase.class) {
            if (instance == null || !save.equals(activeSave)) {
                if (instance != null) instance.close();
                instance = new ModDatabase(pathFor(save));
                activeSave = save;
            }
            local = instance;
        }
        return local;
    }

    private static String currentSave() {
        try {
            var core = Class.forName("zombie.core.Core").getMethod("getInstance").invoke(null);
            var folder = String.valueOf(core.getClass().getMethod("getSaveFolder").invoke(core));
            return folder == null || folder.isBlank() || "null".equals(folder) ? "" : folder;
        } catch (Throwable ignored) { return ""; }
    }

    private static Path pathFor(String save) {
        if (save != null && !save.isBlank()) return Path.of(save, "PersonalKnox.db");
        try {
            var core = Class.forName("zombie.core.Core");
            var dir = String.valueOf(core.getMethod("getLuaCacheDir").invoke(null));
            return Path.of(dir, "PersonalKnox.db");
        } catch (Throwable ignored) {
            return Path.of(System.getProperty("user.home"), "Zomboid", "Lua", "PersonalKnox.db");
        }
    }

    public synchronized void close() {
        try { if (connection != null) connection.close(); } catch (Exception ignored) { }
        connection = null;
    }

    private synchronized Connection connection() throws Exception {
        if (connection != null && !connection.isClosed()) return connection;
        Files.createDirectories(path.getParent());
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
        }
        runResource("schema.sql");
        seedOnce();
        return connection;
    }

    private void runResource(String name) throws Exception {
        try (InputStream input = ModDatabase.class.getResourceAsStream("/personal/knox/db/" + name)) {
            if (input == null) return;
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement statement = connection.createStatement()) {
                for (String part : sql.split(";")) {
                    if (!part.isBlank()) statement.execute(part);
                }
            }
        }
    }

    private void seedOnce() throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM settings")) {
            if (rows.next() && rows.getLong(1) > 0) return;
        }
        runResource("seed.sql");
    }

    public synchronized String getSetting(String key, String fallback) {
        if (key == null || key.isBlank()) return fallback;
        try (PreparedStatement statement = connection().prepareStatement("SELECT value FROM settings WHERE key=?")) {
            statement.setString(1, key);
            try (ResultSet rows = statement.executeQuery()) { return rows.next() ? rows.getString(1) : fallback; }
        } catch (Exception ignored) { return fallback; }
    }

    public synchronized void setSetting(String key, String value) {
        if (key == null || key.isBlank() || value == null) return;
        try (PreparedStatement statement = connection().prepareStatement(
                "INSERT INTO settings(key,value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        } catch (Exception ignored) { }
    }

    public synchronized String schemaVersion() {
        try (PreparedStatement s = connection().prepareStatement("SELECT value FROM meta WHERE key='schema_version'")) {
            try (ResultSet rows = s.executeQuery()) { return rows.next() ? rows.getString(1) : "0"; }
        } catch (Exception ignored) { return "0"; }
    }

    public synchronized java.util.List<String> names(String culture, String gender, String kind) {
        var result = new java.util.ArrayList<String>();
        if (culture == null || gender == null || kind == null) return result;
        try (PreparedStatement s = connection().prepareStatement(
                "SELECT value FROM names WHERE culture=? AND gender=? AND kind=? ORDER BY rowid")) {
            s.setString(1, culture); s.setString(2, gender); s.setString(3, kind);
            try (ResultSet rows = s.executeQuery()) { while (rows.next()) result.add(rows.getString(1)); }
        } catch (Exception ignored) { }
        return result;
    }

    public synchronized java.util.List<String> cultures() {
        var result = new java.util.ArrayList<String>();
        try (Statement s = connection().createStatement();
             ResultSet rows = s.executeQuery("SELECT DISTINCT culture FROM names ORDER BY culture")) {
            while (rows.next()) result.add(rows.getString(1));
        } catch (Exception ignored) { }
        return result;
    }

    public synchronized String allSettings() {
        var text = new StringBuilder();
        try (Statement statement = connection().createStatement();
             ResultSet rows = statement.executeQuery("SELECT key,value FROM settings ORDER BY key")) {
            while (rows.next()) text.append(rows.getString(1)).append('\t').append(rows.getString(2)).append('\n');
        } catch (Exception ignored) { }
        return text.toString();
    }

    public synchronized String getData(String kind, String id, String fallback) {
        if (kind == null || id == null) return fallback;
        try (PreparedStatement statement = connection().prepareStatement(
                "SELECT value FROM data WHERE kind=? AND id=?")) {
            statement.setString(1, kind);
            statement.setString(2, id);
            try (ResultSet rows = statement.executeQuery()) { return rows.next() ? rows.getString(1) : fallback; }
        } catch (Exception ignored) { return fallback; }
    }

    public synchronized void putData(String kind, String id, String value) {
        if (kind == null || id == null || value == null) return;
        try (PreparedStatement statement = connection().prepareStatement(
                "INSERT INTO data(kind,id,value) VALUES(?,?,?) ON CONFLICT(kind,id) DO UPDATE SET value=excluded.value")) {
            statement.setString(1, kind);
            statement.setString(2, id);
            statement.setString(3, value);
            statement.executeUpdate();
        } catch (Exception ignored) { }
    }

    public Path path() { return path; }
}
