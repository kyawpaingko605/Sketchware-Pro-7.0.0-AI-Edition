package com.besome.sketch.export;

import android.content.Context;
import android.os.Bundle;

import com.sketchware.remod.R;

import java.io.File;

import a.a.a.Jp;

public class AppBundleCompiler {

    private String projectDir;
    private String outputDir;
    private Context context;
    private Jp builder;

    public AppBundleCompiler(Jp builder) {
        this.builder = builder;
        this.context = builder.getContext();
        this.projectDir = builder.getProjectDir();
        this.outputDir = builder.getOutputDir();
    }

    public AppBundleCompiler(Context context, String projectDir, String outputDir) {
        this.context = context;
        this.projectDir = projectDir;
        this.outputDir = outputDir;
    }

    public boolean compile() {
        try {
            // AAB (Android App Bundle) ဖိုင်ကို compile လုပ်ခြင်း
            File outputFile = getOutputFile();
            if (outputFile.exists()) {
                outputFile.delete();
            }
            
            // Bundle ဖိုင်တည်ဆောက်ခြင်း
            Bundle bundle = new Bundle();
            bundle.putString("project_dir", projectDir);
            bundle.putString("output_dir", outputDir);
            
            // ဒီနေရာမှာ AAB compile logic ထည့်ပါ
            // ဥပမာ - aapt2, bundletool သုံးပြီး compile လုပ်ခြင်း
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public File getOutputFile() {
        return new File(outputDir, "app.aab");
    }

    public static File getDefaultAppBundleOutputFile(String projectMetadata) {
        // Project metadata ကို parse လုပ်ပြီး output path ပြန်ပေးခြင်း
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        return new File(baseDir + "/Sketchware/project_output/" + projectMetadata + "/app.aab");
    }

    public static File getDefaultAppBundleOutputFile(Context context, String projectName) {
        File outputDir = new File(context.getExternalFilesDir(null), "aab_output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        return new File(outputDir, projectName + ".aab");
    }

    public String getOutputPath() {
        return getOutputFile().getAbsolutePath();
    }

    public void setBuilder(Jp builder) {
        this.builder = builder;
    }

    public Jp getBuilder() {
        return builder;
    }
}
