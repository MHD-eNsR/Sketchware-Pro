package pro.sketchware.supabase;

import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import mod.hey.studios.util.Helper;
import pro.sketchware.utility.FileUtil;

/**
 * Adds a "Supabase" palette with ready-made blocks to Sketchware Pro's custom Block Manager files
 * (<code>.sketchware/resources/block/My Block/palette.json</code> and <code>block.json</code>).
 * <p>
 * The blocks generate calls to the generated <code>SupabaseHelper</code> class. Network blocks are
 * C-shaped: the blocks placed inside run when the request finishes, and the reporter blocks
 * "request succeeded", "response code" and "response body" are valid only inside them.
 * <p>
 * Safe to call repeatedly: existing Supabase blocks are replaced, anything else is left untouched.
 * If one of the files contains invalid JSON, nothing is written so user data is never clobbered.
 */
public final class SupabaseBlocksInstaller {

    private static final String TAG = "SupabaseBlocksInstaller";
    private static final String PALETTE_NAME = "Supabase";
    private static final String PALETTE_COLOR = "#3ECF8E";
    private static final String BLOCK_PREFIX = "supabase";

    private SupabaseBlocksInstaller() {
    }

    private static String dir() {
        return FileUtil.getExternalStorageDir() + "/.sketchware/resources/block/My Block/";
    }

    public static void install() {
        try {
            String palettePath = dir() + "palette.json";
            String blocksPath = dir() + "block.json";

            ArrayList<HashMap<String, Object>> palettes = readList(palettePath);
            ArrayList<HashMap<String, Object>> blocks = readList(blocksPath);
            if (palettes == null || blocks == null) {
                Log.w(TAG, "Block Manager files are not valid JSON, skipping Supabase blocks");
                return;
            }

            int paletteIndex = -1;
            for (int i = 0; i < palettes.size(); i++) {
                if (PALETTE_NAME.equals(String.valueOf(palettes.get(i).get("name")))) {
                    paletteIndex = i;
                    break;
                }
            }
            if (paletteIndex == -1) {
                HashMap<String, Object> palette = new HashMap<>();
                palette.put("name", PALETTE_NAME);
                palette.put("color", PALETTE_COLOR);
                palettes.add(palette);
                paletteIndex = palettes.size() - 1;
            }
            // Custom palettes are numbered from 9 (see PaletteSelector).
            String paletteId = String.valueOf(9 + paletteIndex);

            for (Iterator<HashMap<String, Object>> it = blocks.iterator(); it.hasNext(); ) {
                Object name = it.next().get("name");
                if (name instanceof String s && s.startsWith(BLOCK_PREFIX)) {
                    it.remove();
                }
            }
            addBlocks(blocks, paletteId);

            FileUtil.makeDir(dir());
            Gson gson = new Gson();
            FileUtil.writeFile(palettePath, gson.toJson(palettes));
            FileUtil.writeFile(blocksPath, gson.toJson(blocks));
        } catch (Exception e) {
            Log.e(TAG, "Failed to install Supabase blocks", e);
        }
    }

    /** @return the parsed list, an empty list if the file is missing/empty, or null if invalid. */
    private static ArrayList<HashMap<String, Object>> readList(String path) {
        if (!FileUtil.isExistFile(path)) {
            return new ArrayList<>();
        }
        String content = FileUtil.readFile(path);
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ArrayList<HashMap<String, Object>> list = new Gson().fromJson(content, Helper.TYPE_MAP_LIST);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return null;
        }
    }

    private static void add(ArrayList<HashMap<String, Object>> blocks, String paletteId,
                            String name, String type, String spec, String code) {
        HashMap<String, Object> block = new HashMap<>();
        block.put("name", name);
        block.put("type", type);
        block.put("typeName", "");
        block.put("spec", spec);
        block.put("color", PALETTE_COLOR);
        block.put("palette", paletteId);
        block.put("code", code);
        blocks.add(block);
    }

    /** Callback wrapper: the last %s is the nested block stack. */
    private static String call(String invocation) {
        return "SupabaseHelper." + invocation + ", new SupabaseHelper.Callback() {\n"
                + "@Override\n"
                + "public void onComplete(boolean _ok, int _httpCode, String _body) {\n"
                + "%s\n"
                + "}\n"
                + "});";
    }

    /** Same as {@link #call(String)} for methods whose only parameter is the callback. */
    private static String callNoArgs(String invocation) {
        return call(invocation).replace("SupabaseHelper." + invocation + ", new", "SupabaseHelper." + invocation + "new");
    }

    private static void addBlocks(ArrayList<HashMap<String, Object>> b, String p) {
        add(b, p, "supabaseHeaderSetup", "h", "Supabase Setup", "");
        add(b, p, "supabaseInit", " ", "supabase restore saved session",
                "SupabaseHelper.init(getApplicationContext());");

        add(b, p, "supabaseHeaderDb", "h", "Supabase Database", "");
        add(b, p, "supabaseSelect", "c", "supabase select from table %s query %s",
                call("select(%s, %s"));
        add(b, p, "supabaseInsert", "c", "supabase insert into table %s json %s",
                call("insert(%s, %s"));
        add(b, p, "supabaseUpdate", "c", "supabase update table %s where %s json %s",
                call("update(%s, %s, %s"));
        add(b, p, "supabaseDelete", "c", "supabase delete from table %s where %s",
                call("delete(%s, %s"));
        add(b, p, "supabaseRpc", "c", "supabase call function %s args json %s",
                call("rpc(%s, %s"));

        add(b, p, "supabaseHeaderAuth", "h", "Supabase Auth", "");
        add(b, p, "supabaseSignUp", "c", "supabase sign up email %s password %s",
                call("signUp(%s, %s"));
        add(b, p, "supabaseSignIn", "c", "supabase sign in email %s password %s",
                call("signIn(%s, %s"));
        add(b, p, "supabaseGetUser", "c", "supabase get current user",
                callNoArgs("getUser("));
        add(b, p, "supabaseSignOut", "c", "supabase sign out",
                callNoArgs("signOut("));
        add(b, p, "supabaseIsSignedIn", "b", "supabase is signed in",
                "SupabaseHelper.isSignedIn()");
        add(b, p, "supabaseAccessToken", "s", "supabase access token",
                "SupabaseHelper.getAccessToken()");

        add(b, p, "supabaseHeaderResult", "h", "Inside a request (result)", "");
        add(b, p, "supabaseResultOk", "b", "supabase request succeeded", "_ok");
        add(b, p, "supabaseResultCode", "d", "supabase response code", "_httpCode");
        add(b, p, "supabaseResultBody", "s", "supabase response body", "_body");

        add(b, p, "supabaseHeaderTools", "h", "Supabase Tools", "");
        add(b, p, "supabaseEncode", "s", "supabase url encode %s",
                "SupabaseHelper.encode(%s)");
        add(b, p, "supabasePublicUrl", "s", "supabase public url bucket %s path %s",
                "SupabaseHelper.publicUrl(%s, %s)");
    }
}
