package pro.sketchware.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Sends SketchwarePro compile errors to Google Gemini API and returns fix suggestions.
 */
public class GeminiErrorAnalyzer {

    private static final String PREF_NAME = "ai_error_fix_prefs";
    private static final String PREF_API_KEY = "gemini_api_key";
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final Context context;

    public interface AnalysisCallback {
        void onSuccess(String analysis);
        void onError(String errorMessage);
    }

    public GeminiErrorAnalyzer(Context context) {
        this.context = context.getApplicationContext();
    }

    public String getSavedApiKey() {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(PREF_API_KEY, "");
    }

    public void saveApiKey(String apiKey) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(PREF_API_KEY, apiKey.trim()).apply();
    }

    public boolean hasApiKey() {
        return !getSavedApiKey().isEmpty();
    }

    public void analyzeError(String errorLog, String apiKey, AnalysisCallback callback) {
        try {
            String truncated = errorLog.length() > 8000
                    ? errorLog.substring(0, 8000) + "\n...(truncated)"
                    : errorLog;

            String prompt = "You are an expert in Android development and SketchwarePro " +
                    "(a visual block-based Android app builder).\n\n" +
                    "Analyze this compile error and respond EXACTLY in this format:\n\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "🔍 ERROR SUMMARY\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "[What went wrong in 1-2 simple sentences]\n\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "🎯 ROOT CAUSE\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "[Why this error happened]\n\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "🔧 HOW TO FIX\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "[Numbered fix steps specific to SketchwarePro]\n\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "💡 PREVENTION\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "[1-2 tips to avoid this error]\n\n" +
                    "COMPILE ERROR LOG:\n" + truncated;

            JSONObject part = new JSONObject().put("text", prompt);
            JSONArray parts = new JSONArray().put(part);
            JSONObject content = new JSONObject().put("parts", parts);
            JSONArray contents = new JSONArray().put(content);
            JSONObject config = new JSONObject()
                    .put("temperature", 0.3)
                    .put("maxOutputTokens", 2048);
            JSONObject body = new JSONObject()
                    .put("contents", contents)
                    .put("generationConfig", config);

            Request request = new Request.Builder()
                    .url(GEMINI_URL + apiKey)
                    .post(RequestBody.create(body.toString(), JSON_TYPE))
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        switch (response.code()) {
                            case 400: callback.onError("Invalid API key. Go to ⚙ Settings to update it."); break;
                            case 403: callback.onError("API key missing permission. Enable 'Generative Language API' in Google Cloud Console."); break;
                            case 429: callback.onError("Rate limit exceeded. Wait a moment and try again."); break;
                            default:  callback.onError("API Error " + response.code() + ": " + responseBody); break;
                        }
                        return;
                    }
                    try {
                        String text = new JSONObject(responseBody)
                                .getJSONArray("candidates").getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts").getJSONObject(0)
                                .getString("text");
                        callback.onSuccess(text);
                    } catch (Exception e) {
                        callback.onError("Failed to parse response: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Request error: " + e.getMessage());
        }
    }
}
