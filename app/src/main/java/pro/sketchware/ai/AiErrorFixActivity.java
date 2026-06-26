package pro.sketchware.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import mod.hey.studios.util.Helper;
import mod.jbk.util.AddMarginOnApplyWindowInsetsListener;
import pro.sketchware.R;
import pro.sketchware.databinding.ActivityAiErrorFixBinding;
import pro.sketchware.utility.SketchwareUtil;

/**
 * Shows AI-powered compile error analysis using Google Gemini.
 * Launched from CompileLogActivity when user taps the AI Fix button.
 */
public class AiErrorFixActivity extends BaseAppCompatActivity {

    public static final String EXTRA_ERROR_LOG = "error_log";

    private ActivityAiErrorFixBinding binding;
    private GeminiErrorAnalyzer analyzer;
    private String errorLog;
    private String currentAnalysis = "";

    private enum State { LOADING, SUCCESS, ERROR }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeNoContrast();
        super.onCreate(savedInstanceState);
        binding = ActivityAiErrorFixBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.fabCopy,
                new AddMarginOnApplyWindowInsetsListener(
                        WindowInsetsCompat.Type.navigationBars(), WindowInsetsCompat.CONSUMED));

        analyzer = new GeminiErrorAnalyzer(this);
        binding.topAppBar.setNavigationOnClickListener(Helper.getBackPressedClickListener(this));
        binding.topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_ai_settings) { showApiKeyDialog(false); return true; }
            return false;
        });

        errorLog = getIntent().getStringExtra(EXTRA_ERROR_LOG);
        if (TextUtils.isEmpty(errorLog)) {
            setState(State.ERROR);
            binding.tvErrorState.setText("No error log provided.\n\nOpen the Compile Log screen first.");
            return;
        }

        // Show preview (truncated)
        String preview = errorLog.length() > 2000
                ? errorLog.substring(0, 2000) + "\n...(truncated)"
                : errorLog;
        binding.tvErrorPreview.setText(preview);

        binding.fabCopy.setOnClickListener(v -> copyToClipboard());
        binding.btnRetry.setOnClickListener(v -> startAnalysis());

        if (!analyzer.hasApiKey()) showApiKeyDialog(true);
        else startAnalysis();
    }

    private void startAnalysis() {
        setState(State.LOADING);
        analyzer.analyzeError(errorLog, analyzer.getSavedApiKey(), new GeminiErrorAnalyzer.AnalysisCallback() {
            @Override
            public void onSuccess(String analysis) {
                runOnUiThread(() -> {
                    currentAnalysis = analysis;
                    binding.tvAnalysisText.setText(analysis);
                    setState(State.SUCCESS);
                });
            }
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    binding.tvErrorState.setText(errorMessage);
                    setState(State.ERROR);
                });
            }
        });
    }

    private void setState(State s) {
        binding.progressBar.setVisibility(s == State.LOADING ? View.VISIBLE : View.GONE);
        binding.tvLoadingMessage.setVisibility(s == State.LOADING ? View.VISIBLE : View.GONE);
        binding.tvErrorState.setVisibility(s == State.ERROR ? View.VISIBLE : View.GONE);
        binding.btnRetry.setVisibility(s != State.LOADING ? View.VISIBLE : View.GONE);
        binding.cardAnalysis.setVisibility(s == State.SUCCESS ? View.VISIBLE : View.GONE);
        binding.fabCopy.setVisibility(s == State.SUCCESS ? View.VISIBLE : View.GONE);
    }

    private void copyToClipboard() {
        if (TextUtils.isEmpty(currentAnalysis)) { SketchwareUtil.toast("No analysis yet."); return; }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("AI Error Fix", currentAnalysis));
        SketchwareUtil.toast("Copied to clipboard.");
    }

    private void showApiKeyDialog(boolean startAfter) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = (int)(16 * getResources().getDisplayMetrics().density);
        layout.setPadding(p * 2, p, p * 2, p);

        EditText et = new EditText(this);
        et.setHint("AIza...");
        et.setText(analyzer.getSavedApiKey());
        et.setSingleLine(true);
        layout.addView(et);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Gemini API Key")
                .setMessage("Get a free key at:\naistudio.google.com/app/apikey\n\nStored locally on your device only.")
                .setView(layout)
                .setPositiveButton("Save & Analyze", (d, w) -> {
                    String key = et.getText().toString().trim();
                    if (key.isEmpty()) { SketchwareUtil.toast("Key cannot be empty."); return; }
                    analyzer.saveApiKey(key);
                    if (startAfter) startAnalysis();
                })
                .setNegativeButton("Cancel", (d, w) -> { if (startAfter && !analyzer.hasApiKey()) finish(); })
                .show();
    }

    public static void launch(Context context, String errorLog) {
        Intent intent = new Intent(context, AiErrorFixActivity.class);
        intent.putExtra(EXTRA_ERROR_LOG, errorLog);
        context.startActivity(intent);
    }
}
