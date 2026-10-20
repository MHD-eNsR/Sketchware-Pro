package pro.sketchware.supabase;

import org.json.JSONObject;

import pro.sketchware.utility.FileUtil;

/**
 * Per-project Supabase settings.
 * <p>
 * Stored as JSON in {@code .sketchware/data/<sc_id>/supabase.json}, so it is included in
 * project backups and is independent of the (closed) Firebase/AdMob library storage.
 */
public class SupabaseConfig {
    public static final String FILE_NAME = "supabase.json";

    public boolean enabled;
    public String url = "";
    public String anonKey = "";

    public static String getFilePath(String scId) {
        return FileUtil.getExternalStorageDir() + "/.sketchware/data/" + scId + "/" + FILE_NAME;
    }

    public static SupabaseConfig load(String scId) {
        SupabaseConfig config = new SupabaseConfig();
        try {
            String path = getFilePath(scId);
            if (FileUtil.isExistFile(path)) {
                JSONObject json = new JSONObject(FileUtil.readFile(path));
                config.enabled = json.optBoolean("enabled", false);
                config.url = json.optString("url", "");
                config.anonKey = json.optString("anonKey", "");
            }
        } catch (Exception ignored) {
            // Corrupt file: behave as if Supabase isn't configured.
        }
        return config;
    }

    public void save(String scId) {
        try {
            JSONObject json = new JSONObject();
            json.put("enabled", enabled);
            json.put("url", normalizeUrl(url));
            json.put("anonKey", anonKey == null ? "" : anonKey.trim());
            FileUtil.writeFile(getFilePath(scId), json.toString(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Usable only if enabled and both required fields are filled in. */
    public boolean isUsable() {
        return enabled && !normalizeUrl(url).isEmpty() && anonKey != null && !anonKey.trim().isEmpty();
    }

    public static String normalizeUrl(String url) {
        if (url == null) return "";
        String result = url.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        if (!result.isEmpty() && !result.startsWith("http://") && !result.startsWith("https://")) {
            result = "https://" + result;
        }
        return result;
    }

    /** Escapes a value so it can be embedded in a Java string literal. */
    public static String escapeJava(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "").replace("\r", "");
    }

    /**
     * Returns the source of the generated {@code SupabaseHelper} class, or null when
     * the project doesn't use Supabase.
     */
    public static String generateHelperSource(String scId, String packageName, String template) {
        SupabaseConfig config = load(scId);
        if (!config.isUsable()) return null;
        return template
                .replace("<?package_name?>", packageName)
                .replace("<?supabase_url?>", escapeJava(normalizeUrl(config.url)))
                .replace("<?supabase_anon_key?>", escapeJava(config.anonKey.trim()));
    }
}
