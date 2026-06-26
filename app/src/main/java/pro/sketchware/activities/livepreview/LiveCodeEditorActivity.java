package pro.sketchware.activities.livepreview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.tabs.TabLayout;

import io.github.rosemoe.sora.langs.java.JavaLanguage;

import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.ActivityLiveCodeEditorBinding;
import pro.sketchware.utility.SketchwareUtil;

/**
 * Live Code Editor — write XML layouts + Java logic, preview in real time.
 *
 * Tab 0: XML Layout   — sora code editor for writing Android XML
 * Tab 1: Java Logic   — sora code editor for Java code reference
 * Tab 2: Preview      — live rendered view from the XML
 * Tab 3: Log          — render messages & interaction log
 */
public class LiveCodeEditorActivity extends BaseAppCompatActivity {

    public static final String EXTRA_XML   = "initial_xml";
    public static final String EXTRA_SC_ID = "sc_id";

    private static final String PREF_XML_KEY  = "live_editor_xml";
    private static final String PREF_JAVA_KEY = "live_editor_java";

    private ActivityLiveCodeEditorBinding binding;
    private XmlLayoutRenderer renderer;
    private SharedPreferences prefs;

    // ── Default starter code ─────────────────────────────────────────────────
    private static final String DEFAULT_XML =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
        "<LinearLayout\n" +
        "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
        "    android:layout_width=\"match_parent\"\n" +
        "    android:layout_height=\"match_parent\"\n" +
        "    android:orientation=\"vertical\"\n" +
        "    android:padding=\"16dp\"\n" +
        "    android:background=\"#FFFFFF\">\n\n" +
        "    <TextView\n" +
        "        android:layout_width=\"match_parent\"\n" +
        "        android:layout_height=\"wrap_content\"\n" +
        "        android:text=\"Hello, SketchwarePro!\"\n" +
        "        android:textSize=\"24sp\"\n" +
        "        android:textColor=\"#1976D2\"\n" +
        "        android:textStyle=\"bold\"\n" +
        "        android:layout_marginBottom=\"12dp\" />\n\n" +
        "    <EditText\n" +
        "        android:layout_width=\"match_parent\"\n" +
        "        android:layout_height=\"wrap_content\"\n" +
        "        android:hint=\"Type something here...\"\n" +
        "        android:layout_marginBottom=\"12dp\" />\n\n" +
        "    <Button\n" +
        "        android:layout_width=\"wrap_content\"\n" +
        "        android:layout_height=\"wrap_content\"\n" +
        "        android:text=\"Click Me\" />\n\n" +
        "    <LinearLayout\n" +
        "        android:layout_width=\"match_parent\"\n" +
        "        android:layout_height=\"wrap_content\"\n" +
        "        android:orientation=\"horizontal\"\n" +
        "        android:layout_marginTop=\"16dp\">\n\n" +
        "        <CheckBox\n" +
        "            android:layout_width=\"wrap_content\"\n" +
        "            android:layout_height=\"wrap_content\"\n" +
        "            android:text=\"Option 1\"\n" +
        "            android:layout_marginEnd=\"16dp\" />\n\n" +
        "        <CheckBox\n" +
        "            android:layout_width=\"wrap_content\"\n" +
        "            android:layout_height=\"wrap_content\"\n" +
        "            android:text=\"Option 2\" />\n" +
        "    </LinearLayout>\n\n" +
        "    <ProgressBar\n" +
        "        android:layout_width=\"match_parent\"\n" +
        "        android:layout_height=\"wrap_content\"\n" +
        "        android:layout_marginTop=\"16dp\" />\n\n" +
        "</LinearLayout>";

    private static final String DEFAULT_JAVA =
        "// SketchwarePro — Java Logic Editor\n" +
        "// Write your Java code here.\n" +
        "// Copy-paste snippets into Sketchware custom code blocks.\n\n" +
        "public class MainActivity extends AppCompatActivity {\n\n" +
        "    TextView  tvTitle;\n" +
        "    EditText  etInput;\n" +
        "    Button    btnClick;\n\n" +
        "    @Override\n" +
        "    protected void onCreate(Bundle savedInstanceState) {\n" +
        "        super.onCreate(savedInstanceState);\n" +
        "        setContentView(R.layout.activity_main);\n\n" +
        "        tvTitle  = findViewById(R.id.tv_title);\n" +
        "        etInput  = findViewById(R.id.et_input);\n" +
        "        btnClick = findViewById(R.id.btn_click);\n\n" +
        "        btnClick.setOnClickListener(v -> {\n" +
        "            String input = etInput.getText().toString().trim();\n" +
        "            if (!input.isEmpty()) {\n" +
        "                tvTitle.setText(\"Hello, \" + input + \"!\");\n" +
        "            } else {\n" +
        "                Toast.makeText(this, \"Please enter something\",\n" +
        "                    Toast.LENGTH_SHORT).show();\n" +
        "            }\n" +
        "        });\n" +
        "    }\n" +
        "}\n";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ActivityLiveCodeEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs    = getPreferences(Context.MODE_PRIVATE);
        renderer = new XmlLayoutRenderer(this);

        setupToolbar();
        setupEditors();
        setupTabs();

        binding.fabRun.setOnClickListener(v -> runPreview());

        // Load saved or initial XML
        String initialXml = getIntent().getStringExtra(EXTRA_XML);
        String xml  = prefs.getString(PREF_XML_KEY,  initialXml != null ? initialXml : DEFAULT_XML);
        String java = prefs.getString(PREF_JAVA_KEY, DEFAULT_JAVA);
        binding.editorXml.setText(xml);
        binding.editorJava.setText(java);

        appendLog("Ready. Tap ▶ Run to preview your XML layout.");
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
        binding.topAppBar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_run) {
                runPreview(); return true;
            }
            if (id == R.id.menu_save_code) {
                saveCode(); SketchwareUtil.toast("Code saved."); return true;
            }
            if (id == R.id.menu_clear_log) {
                binding.tvLog.setText(""); appendLog("Log cleared."); return true;
            }
            return false;
        });
    }

    private void setupEditors() {
        // Use built-in Java language (closest available; sora handles XML too via textmate)
        binding.editorXml.setEditorLanguage(new JavaLanguage());
        binding.editorJava.setEditorLanguage(new JavaLanguage());
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("XML Layout"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Java Logic"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Preview"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Log"));

        showPage(0); // start on XML tab

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { showPage(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showPage(int position) {
        binding.pageXml.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        binding.pageJava.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        binding.pagePreview.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
        binding.pageLog.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
        // Show FAB on XML and Preview tabs only
        binding.fabRun.setVisibility((position == 0 || position == 2) ? View.VISIBLE : View.GONE);
    }

    // ── Preview logic ────────────────────────────────────────────────────────

    private void runPreview() {
        hideKeyboard();
        String xml = binding.editorXml.getText().toString().trim();
        if (xml.isEmpty()) { SketchwareUtil.toast("XML is empty."); return; }

        saveCode();
        binding.previewContainer.removeAllViews();

        StringBuilder errBuf = new StringBuilder();
        View rendered = renderer.render(xml, errBuf);

        if (rendered != null) {
            binding.previewContainer.addView(rendered);
            appendLog("[OK] Rendered successfully.");
            // Switch to Preview tab
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(2));
            showPage(2);
        } else {
            String err = errBuf.length() > 0 ? errBuf.toString() : "Unknown render error.";
            appendLog("[ERROR] Render failed:\n   " + err);
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(3));
            showPage(3);
            SketchwareUtil.toast("Render error — see Log tab.");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void saveCode() {
        prefs.edit()
             .putString(PREF_XML_KEY,  binding.editorXml.getText().toString())
             .putString(PREF_JAVA_KEY, binding.editorJava.getText().toString())
             .apply();
    }

    private void appendLog(String msg) {
        String cur = binding.tvLog.getText().toString();
        binding.tvLog.setText(cur.isEmpty() ? msg : cur + "\n" + msg);
        // Scroll to bottom
        binding.pageLog.post(() -> binding.pageLog.fullScroll(View.FOCUS_DOWN));
    }

    private void hideKeyboard() {
        View f = getCurrentFocus();
        if (f == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(f.getWindowToken(), 0);
    }

    /** Convenience launcher. Pass nulls to use defaults. */
    public static void launch(Context context, String initialXml, String scId) {
        Intent intent = new Intent(context, LiveCodeEditorActivity.class);
        if (initialXml != null) intent.putExtra(EXTRA_XML, initialXml);
        if (scId      != null) intent.putExtra(EXTRA_SC_ID, scId);
        context.startActivity(intent);
    }
}
