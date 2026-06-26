package pro.sketchware.activities.livepreview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Parses an Android XML layout string and builds a native View hierarchy at runtime.
 * Supports common layout containers and widgets with their basic attributes.
 */
public class XmlLayoutRenderer {

    private final Context context;

    public XmlLayoutRenderer(Context context) {
        this.context = context;
    }

    /**
     * Parse the given XML string and return a View hierarchy.
     *
     * @param xml      Android layout XML string
     * @param errorOut If non-null, render errors are appended here
     * @return Root view, or null on failure
     */
    public View render(String xml, StringBuilder errorOut) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xml));

            Deque<ViewGroup> stack = new ArrayDeque<>();
            View root = null;
            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tag = parser.getName();
                    View view = createView(tag, parser);
                    if (view == null) {
                        view = new FrameLayout(context); // unknown tag placeholder
                    }
                    applyLayoutParams(view, parser, stack.isEmpty() ? null : stack.peek());

                    if (root == null) root = view;

                    if (!stack.isEmpty()) {
                        stack.peek().addView(view);
                    }

                    if (view instanceof ViewGroup) {
                        stack.push((ViewGroup) view);
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    if (!stack.isEmpty()) {
                        String tag = parser.getName();
                        View top = stack.peek();
                        if (isViewGroup(tag)) {
                            stack.pop();
                        }
                    }
                }
                eventType = parser.next();
            }
            return root;
        } catch (Exception e) {
            if (errorOut != null) errorOut.append("Render error: ").append(e.getMessage()).append("\n");
            return null;
        }
    }

    private View createView(String tag, XmlPullParser p) {
        switch (tag) {
            // ── Layouts ──────────────────────────────────────
            case "LinearLayout":
            case "androidx.constraintlayout.widget.ConstraintLayout": {
                LinearLayout ll = new LinearLayout(context);
                String orientation = attr(p, "android:orientation");
                ll.setOrientation("horizontal".equals(orientation)
                        ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
                applyCommonAttributes(ll, p);
                return ll;
            }
            case "RelativeLayout": {
                RelativeLayout rl = new RelativeLayout(context);
                applyCommonAttributes(rl, p);
                return rl;
            }
            case "FrameLayout":
            case "androidx.cardview.widget.CardView":
            case "com.google.android.material.card.MaterialCardView": {
                FrameLayout fl = new FrameLayout(context);
                applyCommonAttributes(fl, p);
                return fl;
            }
            case "ScrollView": {
                ScrollView sv = new ScrollView(context);
                applyCommonAttributes(sv, p);
                return sv;
            }
            case "HorizontalScrollView": {
                HorizontalScrollView hsv = new HorizontalScrollView(context);
                applyCommonAttributes(hsv, p);
                return hsv;
            }
            // ── Widgets ──────────────────────────────────────
            case "TextView": {
                TextView tv = new TextView(context);
                applyTextViewAttributes(tv, p);
                return tv;
            }
            case "Button":
            case "com.google.android.material.button.MaterialButton": {
                Button btn = new Button(context);
                applyTextViewAttributes(btn, p);
                return btn;
            }
            case "EditText":
            case "com.google.android.material.textfield.TextInputEditText": {
                EditText et = new EditText(context);
                applyTextViewAttributes(et, p);
                String hint = attr(p, "android:hint");
                if (!hint.isEmpty()) et.setHint(hint);
                return et;
            }
            case "ImageView": {
                ImageView iv = new ImageView(context);
                applyCommonAttributes(iv, p);
                iv.setBackgroundColor(Color.parseColor("#E0E0E0")); // placeholder grey
                return iv;
            }
            case "CheckBox": {
                CheckBox cb = new CheckBox(context);
                applyTextViewAttributes(cb, p);
                return cb;
            }
            case "RadioButton": {
                RadioButton rb = new RadioButton(context);
                applyTextViewAttributes(rb, p);
                return rb;
            }
            case "Switch":
            case "androidx.appcompat.widget.SwitchCompat": {
                Switch sw = new Switch(context);
                applyTextViewAttributes(sw, p);
                return sw;
            }
            case "ProgressBar": {
                ProgressBar pb = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
                pb.setMax(100);
                pb.setProgress(50);
                applyCommonAttributes(pb, p);
                return pb;
            }
            case "View": {
                View v = new View(context);
                applyCommonAttributes(v, p);
                return v;
            }
            default:
                return null;
        }
    }

    // ── Attribute helpers ────────────────────────────────────────────────────

    private void applyCommonAttributes(View v, XmlPullParser p) {
        String bg = attr(p, "android:background");
        if (!bg.isEmpty()) {
            try {
                v.setBackgroundColor(Color.parseColor(bg));
            } catch (Exception ignored) {}
        }
        applyPadding(v, p);
        applyGravity(v, p);
    }

    private void applyTextViewAttributes(TextView tv, XmlPullParser p) {
        applyCommonAttributes(tv, p);

        String text = attr(p, "android:text");
        if (!text.isEmpty()) tv.setText(text);

        String textSize = attr(p, "android:textSize");
        if (!textSize.isEmpty()) {
            float sp = parseDimension(textSize);
            if (sp > 0) tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        }

        String textColor = attr(p, "android:textColor");
        if (!textColor.isEmpty()) {
            try { tv.setTextColor(Color.parseColor(textColor)); } catch (Exception ignored) {}
        }

        String textStyle = attr(p, "android:textStyle");
        if ("bold".equals(textStyle)) tv.setTypeface(null, Typeface.BOLD);
        else if ("italic".equals(textStyle)) tv.setTypeface(null, Typeface.ITALIC);
        else if ("bold|italic".equals(textStyle)) tv.setTypeface(null, Typeface.BOLD_ITALIC);

        String gravity = attr(p, "android:gravity");
        if (!gravity.isEmpty()) tv.setGravity(parseGravity(gravity));

        String lines = attr(p, "android:maxLines");
        if (!lines.isEmpty()) { try { tv.setMaxLines(Integer.parseInt(lines)); } catch (Exception ignored) {} }

        String singleLine = attr(p, "android:singleLine");
        if ("true".equals(singleLine)) tv.setSingleLine(true);
    }

    private void applyGravity(View v, XmlPullParser p) {
        if (!(v instanceof LinearLayout)) return;
        String gravity = attr(p, "android:gravity");
        if (!gravity.isEmpty()) ((LinearLayout) v).setGravity(parseGravity(gravity));
    }

    private void applyPadding(View v, XmlPullParser p) {
        int pad = parseDpToPixels(attr(p, "android:padding"));
        int padL = parseDpToPixels(attr(p, "android:paddingLeft"));
        int padR = parseDpToPixels(attr(p, "android:paddingRight"));
        int padT = parseDpToPixels(attr(p, "android:paddingTop"));
        int padB = parseDpToPixels(attr(p, "android:paddingBottom"));
        int padH = parseDpToPixels(attr(p, "android:paddingHorizontal"));
        int padV = parseDpToPixels(attr(p, "android:paddingVertical"));

        int l = padL > 0 ? padL : (padH > 0 ? padH : pad);
        int r = padR > 0 ? padR : (padH > 0 ? padH : pad);
        int t = padT > 0 ? padT : (padV > 0 ? padV : pad);
        int b = padB > 0 ? padB : (padV > 0 ? padV : pad);
        if (l != 0 || r != 0 || t != 0 || b != 0) v.setPadding(l, t, r, b);
    }

    private void applyLayoutParams(View v, XmlPullParser p, ViewGroup parent) {
        int w = parseDimension2(attr(p, "android:layout_width"), parent);
        int h = parseDimension2(attr(p, "android:layout_height"), parent);
        if (w == 0) w = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (h == 0) h = ViewGroup.LayoutParams.WRAP_CONTENT;

        int ml = parseDpToPixels(attr(p, "android:layout_marginLeft"));
        int mr = parseDpToPixels(attr(p, "android:layout_marginRight"));
        int mt = parseDpToPixels(attr(p, "android:layout_marginTop"));
        int mb = parseDpToPixels(attr(p, "android:layout_marginBottom"));
        int mAll = parseDpToPixels(attr(p, "android:layout_margin"));
        int mH = parseDpToPixels(attr(p, "android:layout_marginHorizontal"));
        int mV = parseDpToPixels(attr(p, "android:layout_marginVertical"));

        ml = ml > 0 ? ml : (mH > 0 ? mH : mAll);
        mr = mr > 0 ? mr : (mH > 0 ? mH : mAll);
        mt = mt > 0 ? mt : (mV > 0 ? mV : mAll);
        mb = mb > 0 ? mb : (mV > 0 ? mV : mAll);

        String weightStr = attr(p, "android:layout_weight");

        if (parent instanceof LinearLayout) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
            lp.setMargins(ml, mt, mr, mb);
            if (!weightStr.isEmpty()) {
                try { lp.weight = Float.parseFloat(weightStr); } catch (Exception ignored) {}
            }
            v.setLayoutParams(lp);
        } else if (parent instanceof RelativeLayout) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(w, h);
            lp.setMargins(ml, mt, mr, mb);
            v.setLayoutParams(lp);
        } else if (parent instanceof FrameLayout) {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
            lp.setMargins(ml, mt, mr, mb);
            String gravity = attr(p, "android:layout_gravity");
            if (!gravity.isEmpty()) lp.gravity = parseGravity(gravity);
            v.setLayoutParams(lp);
        } else {
            v.setLayoutParams(new ViewGroup.LayoutParams(w, h));
        }
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private String attr(XmlPullParser p, String name) {
        // Try namespaced first
        String val = p.getAttributeValue("http://schemas.android.com/apk/res/android",
                name.replace("android:", ""));
        if (val == null) val = p.getAttributeValue(null, name.replace("android:", ""));
        return val != null ? val : "";
    }

    private int parseDpToPixels(String value) {
        float dp = parseDimension(value);
        if (dp <= 0) return 0;
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    private float parseDimension(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            if (value.endsWith("dp") || value.endsWith("dip"))
                return Float.parseFloat(value.replace("dp", "").replace("dip", "").trim());
            if (value.endsWith("sp"))
                return Float.parseFloat(value.replace("sp", "").trim());
            if (value.endsWith("px"))
                return Float.parseFloat(value.replace("px", "").trim());
            return Float.parseFloat(value);
        } catch (Exception e) { return 0; }
    }

    private int parseDimension2(String value, ViewGroup parent) {
        if ("match_parent".equals(value) || "fill_parent".equals(value))
            return ViewGroup.LayoutParams.MATCH_PARENT;
        if ("wrap_content".equals(value)) return ViewGroup.LayoutParams.WRAP_CONTENT;
        return parseDpToPixels(value);
    }

    private int parseGravity(String gravity) {
        int g = Gravity.NO_GRAVITY;
        if (gravity.contains("center")) g |= Gravity.CENTER;
        if (gravity.contains("center_horizontal")) g |= Gravity.CENTER_HORIZONTAL;
        if (gravity.contains("center_vertical")) g |= Gravity.CENTER_VERTICAL;
        if (gravity.contains("start") || gravity.contains("left")) g |= Gravity.START;
        if (gravity.contains("end") || gravity.contains("right")) g |= Gravity.END;
        if (gravity.contains("top")) g |= Gravity.TOP;
        if (gravity.contains("bottom")) g |= Gravity.BOTTOM;
        return g;
    }

    private boolean isViewGroup(String tag) {
        switch (tag) {
            case "LinearLayout": case "RelativeLayout": case "FrameLayout":
            case "ScrollView": case "HorizontalScrollView":
            case "androidx.constraintlayout.widget.ConstraintLayout":
            case "androidx.cardview.widget.CardView":
            case "com.google.android.material.card.MaterialCardView":
                return true;
            default: return false;
        }
    }
}
