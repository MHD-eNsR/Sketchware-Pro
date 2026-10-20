package com.besome.sketch.editor.manage.library.supabase;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import a.a.a.bB;
import pro.sketchware.R;
import pro.sketchware.supabase.SupabaseConfig;

/**
 * Settings screen for the Supabase library. Saves to {@link SupabaseConfig} when leaving.
 */
public class SupabaseActivity extends BaseAppCompatActivity {

    private String scId;
    private MaterialSwitch enableSwitch;
    private TextInputLayout urlLayout;
    private TextInputLayout keyLayout;
    private TextInputEditText urlInput;
    private TextInputEditText keyInput;
    private Button testButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_library_supabase);

        scId = savedInstanceState == null
                ? getIntent().getStringExtra("sc_id")
                : savedInstanceState.getString("sc_id");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        enableSwitch = findViewById(R.id.switch_enable);
        urlLayout = findViewById(R.id.til_url);
        keyLayout = findViewById(R.id.til_key);
        urlInput = findViewById(R.id.et_url);
        keyInput = findViewById(R.id.et_key);
        testButton = findViewById(R.id.btn_test);

        urlLayout.setPlaceholderText(getString(R.string.supabase_project_url_hint));

        SupabaseConfig config = SupabaseConfig.load(scId);
        enableSwitch.setChecked(config.enabled);
        if (savedInstanceState == null) {
            urlInput.setText(config.url);
            keyInput.setText(config.anonKey);
        }

        testButton.setOnClickListener(v -> testConnection());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (saveIfValid()) {
                    setResult(RESULT_OK);
                    finish();
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("sc_id", scId);
        super.onSaveInstanceState(outState);
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    /** Validates (only when enabled) and persists. Returns false to stay on the screen. */
    private boolean saveIfValid() {
        urlLayout.setError(null);
        keyLayout.setError(null);

        boolean enabled = enableSwitch.isChecked();
        String url = SupabaseConfig.normalizeUrl(text(urlInput));
        String key = text(keyInput);

        if (enabled) {
            if (url.isEmpty()) {
                urlLayout.setError(getString(R.string.supabase_fill_required));
                return false;
            }
            if (!android.util.Patterns.WEB_URL.matcher(url).matches()) {
                urlLayout.setError(getString(R.string.supabase_invalid_url));
                return false;
            }
            if (key.isEmpty()) {
                keyLayout.setError(getString(R.string.supabase_fill_required));
                return false;
            }
        }

        SupabaseConfig config = new SupabaseConfig();
        config.enabled = enabled;
        config.url = url;
        config.anonKey = key;
        config.save(scId);
        if (enabled) {
            // Adds the "Supabase" palette with ready-made blocks to the Block Manager.
            pro.sketchware.supabase.SupabaseBlocksInstaller.install();
        }
        return true;
    }

    private void testConnection() {
        String url = SupabaseConfig.normalizeUrl(text(urlInput));
        String key = text(keyInput);
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(key)) {
            bB.b(this, getString(R.string.supabase_fill_required), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        testButton.setEnabled(false);
        testButton.setText(R.string.supabase_testing);

        new Thread(() -> {
            String error = null;
            HttpURLConnection connection = null;
            try {
                // GET /rest/v1/ returns the OpenAPI spec when the URL and key are valid.
                connection = (HttpURLConnection) new URL(url + "/rest/v1/").openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("apikey", key);
                connection.setRequestProperty("Authorization", "Bearer " + key);
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) {
                    error = "HTTP " + code;
                } else {
                    InputStream stream = connection.getInputStream();
                    if (stream != null) stream.close();
                }
            } catch (Exception e) {
                error = e.getClass().getSimpleName();
            } finally {
                if (connection != null) connection.disconnect();
            }

            String finalError = error;
            runOnUiThread(() -> {
                testButton.setEnabled(true);
                testButton.setText(R.string.supabase_test_connection);
                if (finalError == null) {
                    bB.a(this, getString(R.string.supabase_test_ok), android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    bB.b(this, getString(R.string.supabase_test_failed, finalError), android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}
