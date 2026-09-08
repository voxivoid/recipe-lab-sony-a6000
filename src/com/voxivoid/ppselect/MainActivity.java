package com.voxivoid.ppselect;

import android.app.Activity;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;

/**
 * PP Select 0.8 — film recipes with LIVE PREVIEW, then persistent write (photo + video, survives power-cycle).
 *
 * Preview = runtime camera parameters (exact for colour mode / sat / con / sharp / WB / matrix).
 * ENTER  = write the recipe's stored bytes (Creative Style + WB slots) + sync → power-cycle applies it everywhere.
 *
 * Keys: control wheel / LEFT / RIGHT = recipe (previewed instantly) · UP / DOWN = select parameter · top dial = adjust it
 *       TRASH (also C1 / DISP / Fn) = overlay: full → pill → hidden · ENTER write+sync · AEL stage factory (PLAY = firmware playback, unusable)
 *       SHUTTER photo (preview look) · MENU exit (runtime look reverts, stored values stay)
 */
public class MainActivity extends Activity implements SurfaceHolder.Callback {
    // ScalarInput scan codes
    private static final int K_UP = 103, K_DOWN = 108, K_LEFT = 105, K_RIGHT = 106, K_ENTER = 232, K_MENU = 514, K_SK1 = 229,
            K_DELETE = 595, K_SK2 = 513, K_PLAY = 207, K_DISP = 608, K_FN = 520, K_AEL = 532, K_C1 = 622, K_S1 = 516, K_S2 = 518,
            K_WHEEL_CW = 522, K_WHEEL_CCW = 523, K_DIAL_CW = 525, K_DIAL_CCW = 526;

    private static final int ID_STYLE = 0x01070175, ID_CON = 0x01070178, ID_SAT = 0x01070187, ID_SHARP = 0x0107018a, ID_PP_NO = 0x0107031c,
            ID_WB_MODE = 0x01070019, ID_WB_TEMP = 0x01070018, ID_WB_AB = 0x01070017, ID_WB_GM = 0x01070016;

    private static final String[] ROW_NAME = { "RECIPE", "STYLE", "SAT", "CON", "SHARP", "MATRIX", "WB", "KELVIN", "A-B", "G-M" };
    private static final int[] ROW_ID = { 0, ID_STYLE, ID_SAT, ID_CON, ID_SHARP, ID_PP_NO, ID_WB_MODE, ID_WB_TEMP, ID_WB_AB, ID_WB_GM };
    private static final int[] ROW_MIN = { 0, 1, -16, -8, -8, 0, 0, 25, -7, -7 };
    private static final int[] ROW_MAX = { 0, 13, 16, 8, 8, 1, 20, 99, 7, 7 };
    private static final int N = ROW_ID.length;
    // PP3 colour matrix measured on this body, Q10 fixed point (1.0 = 1024)
    private static final String PP3_MATRIX = "1331,-307,-51,-205,1331,-123,-20,-461,1485";

    private static final int ACCENT = 0xFFF2B85C, INK = 0xFF1A1208, WHITE = 0xFFFFFFFF, DIM = 0x99FFFFFF;

    private View panel;
    private TextView name, badge, count, meta, mini, toast;
    private HintBar hints;
    private LinearLayout chips;
    private final TextView[] chipLabel = new TextView[N], chipValue = new TextView[N];
    private final View[] chip = new View[N];
    private final Handler handler = new Handler();
    private final Runnable hideToast = new Runnable() { public void run() { toast.setVisibility(View.GONE); } };

    private SurfaceHolder holder;
    private Object cameraEx; private Camera camera; private String origFlat;
    private int row = 0, recipe = 0, overlay = 0;     // overlay: 0 full, 1 pill, 2 hidden
    private final int[] cur = new int[N], edit = new int[N];
    private boolean protectedStore = false, previewOk = false;
    private String previewErr = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.main);
        panel = findViewById(R.id.panel);
        name = (TextView) findViewById(R.id.name);
        badge = (TextView) findViewById(R.id.badge);
        count = (TextView) findViewById(R.id.count);
        meta = (TextView) findViewById(R.id.meta);
        hints = (HintBar) findViewById(R.id.hints);
        mini = (TextView) findViewById(R.id.mini);
        toast = (TextView) findViewById(R.id.toast);
        chips = (LinearLayout) findViewById(R.id.chips);
        buildChips();
        SurfaceView sv = (SurfaceView) findViewById(R.id.surface);
        holder = sv.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private void buildChips() {
        for (int i = 1; i < N; i++) {
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setGravity(Gravity.CENTER_HORIZONTAL);
            c.setPadding(dp(8), dp(3), dp(8), dp(4));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(5);
            c.setLayoutParams(lp);
            TextView l = new TextView(this); l.setTextSize(9); l.setText(ROW_NAME[i]);
            TextView v = new TextView(this); v.setTextSize(13); v.setTypeface(Typeface.DEFAULT_BOLD); v.setSingleLine(true);
            c.addView(l); c.addView(v);
            chips.addView(c);
            chip[i] = c; chipLabel[i] = l; chipValue[i] = v;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
        try {
            Class<?> cx = Class.forName("com.sony.scalar.hardware.CameraEx");
            Method open = cx.getMethod("open", int.class, Class.forName("com.sony.scalar.hardware.CameraEx$OpenOptions"));
            cameraEx = open.invoke(null, 0, null);
            camera = (Camera) cx.getMethod("getNormalCamera").invoke(cameraEx);
            origFlat = camera.getParameters().flatten();
            holder.addCallback(this);
            previewOk = true;
        } catch (Throwable t) { previewOk = false; previewErr = String.valueOf(t); }
        stageRecipe(); applyPreview(); render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(hideToast);
        holder.removeCallback(this);
        try { if (camera != null && origFlat != null) { Camera.Parameters p = camera.getParameters(); p.unflatten(origFlat); camera.setParameters(p); } } catch (Throwable t) {}
        try { if (camera != null) camera.stopPreview(); } catch (Throwable t) {}
        try { if (cameraEx != null) cameraEx.getClass().getMethod("release").invoke(cameraEx); } catch (Throwable t) {}
        cameraEx = null; camera = null;
    }

    public void surfaceCreated(SurfaceHolder h) {
        try { camera.setPreviewDisplay(h); camera.startPreview(); }
        catch (Throwable t) { previewOk = false; previewErr = String.valueOf(t); render(); }
    }
    public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) {}
    public void surfaceDestroyed(SurfaceHolder h) {}

    // ------------------------------------------------------------ stored settings
    private int rd(int id) throws NativeException { return (byte) NativeBackup.readByte(id); }
    private int rdu(int id) throws NativeException { return NativeBackup.readByte(id) & 0xff; }

    private void load() {
        try {
            for (int i = 1; i < N; i++) {
                int id = ROW_ID[i];
                int v = (id == ID_WB_TEMP || id == ID_WB_MODE || id == ID_STYLE) ? rdu(id) : rd(id);
                if (id == ID_PP_NO) v = (v == 0) ? 0 : 1;
                cur[i] = v; edit[i] = v;
            }
            protectedStore = NativeBackup.isProtected();
        } catch (Throwable t) { showToast("Read failed: " + t.getMessage(), 0); }
    }

    private void stageRecipe() {
        Recipes.Recipe r = Recipes.ALL[recipe];
        edit[1] = r.style; edit[2] = r.sat; edit[3] = r.con; edit[4] = r.sharp; edit[5] = r.matrix;
        if (r.wbMode != 0) { edit[6] = r.wbMode; if (r.wbMode == 14) edit[7] = r.kelvin / 100; }
        edit[8] = r.ab; edit[9] = r.gm;
    }

    private boolean dirty() { for (int i = 1; i < N; i++) if (edit[i] != cur[i]) return true; return false; }

    private void writeAll() {
        if (!dirty()) { showToast("Already stored — nothing to write", 2500); return; }
        StringBuilder s = new StringBuilder();
        String msg;
        try {
            int n = 0;
            for (int i = 1; i < N; i++) {
                if (edit[i] == cur[i]) continue;
                int v = edit[i];
                if (ROW_ID[i] == ID_PP_NO) v = (v == 0) ? 0 : 3;
                NativeBackup.writeByte(ROW_ID[i], v);
                n++;
            }
            NativeBackup.sync();
            msg = "Stored " + n + " value" + (n == 1 ? "" : "s") + " — power-cycle the camera to apply everywhere";
        } catch (Throwable t) { msg = "WRITE FAILED: " + t.getMessage(); }
        load(); stageRecipe();
        showToast(msg, 5000); render();
    }

    // ------------------------------------------------------------ live preview (runtime params)
    private void applyPreview() {
        if (camera == null) return;
        try {
            Camera.Parameters p = camera.getParameters();
            int st = edit[1];
            p.set("color-mode", st >= 1 && st < Recipes.STYLE_NAMES.length ? Recipes.STYLE_NAMES[st] : "standard");
            p.set("saturation", String.valueOf(edit[2]));
            p.set("contrast", String.valueOf(Math.max(-3, Math.min(3, edit[3]))));
            p.set("sharpness", String.valueOf(Math.max(-3, Math.min(3, edit[4]))));
            if (edit[5] == 1) { p.set("rgb-matrix", PP3_MATRIX); p.set("rgb-matrix-mode", "true"); } else p.set("rgb-matrix-mode", "false");
            if (edit[6] == 14) { p.set("whitebalance", "color-temp"); p.set("color-temperture-white-balance", String.valueOf(edit[7] * 100)); }
            else if (edit[6] == 1) p.set("whitebalance", "auto");
            p.set("light-balance-for-white-balance", String.valueOf(edit[8]));
            p.set("color-compensation-for-white-balance", String.valueOf(edit[9]));
            camera.setParameters(p);
            previewOk = true;
        } catch (Throwable t) { previewOk = false; previewErr = String.valueOf(t.getMessage()); }
    }

    // ------------------------------------------------------------ UI
    private static String cap(String s) { return s.length() == 0 ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    private String styleName(int v) { return v >= 1 && v < Recipes.STYLE_NAMES.length ? cap(Recipes.STYLE_NAMES[v]) : "?" + v; }

    private String fmt(int i, int v) {
        switch (i) {
            case 1: return styleName(v);
            case 5: return v == 0 ? "off" : "PP3";
            case 6: return v == 1 ? "auto" : v == 14 ? "kelvin" : String.valueOf(v);
            case 7: return edit[6] == 14 ? (v * 100) + "K" : "-";
            case 8: return v == 0 ? "0" : (v > 0 ? "A" + v : "B" + (-v));
            case 9: return v == 0 ? "0" : (v > 0 ? "G" + v : "M" + (-v));
            default: return (v > 0 ? "+" : "") + v;
        }
    }

    private void showToast(String msg, int ms) {
        toast.setText(msg); toast.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideToast);
        if (ms > 0) handler.postDelayed(hideToast, ms);
    }

    private void render() {
        Recipes.Recipe r = Recipes.ALL[recipe];
        boolean dirty = dirty();
        String pos = (recipe + 1) + " / " + Recipes.ALL.length;
        if (overlay == 0) {
            panel.setVisibility(View.VISIBLE); mini.setVisibility(View.GONE);
            name.setText(r.name);
            name.setTextColor(row == 0 ? ACCENT : WHITE);
            count.setText(pos);
            if (protectedStore) { badge.setText("PROTECTED"); badge.setBackgroundResource(R.drawable.badge_err); }
            else if (dirty) { badge.setText("PREVIEW"); badge.setBackgroundResource(R.drawable.badge_warn); }
            else { badge.setText("STORED"); badge.setBackgroundResource(R.drawable.badge_ok); }
            StringBuilder m = new StringBuilder(styleName(edit[1]));
            m.append("  ·  WB ").append(edit[6] == 14 ? (edit[7] * 100) + "K" : edit[6] == 1 ? "auto" : "mode " + edit[6]);
            if (edit[5] == 1) m.append("  ·  PP3 colour matrix");
            if (!previewOk) m.append("  ·  no live preview: ").append(previewErr);
            meta.setText(m);
            for (int i = 1; i < N; i++) {
                boolean sel = i == row, ch = edit[i] != cur[i];
                chip[i].setBackgroundResource(sel ? R.drawable.chip_sel : R.drawable.chip);
                chipLabel[i].setTextColor(sel ? INK : DIM);
                chipValue[i].setTextColor(sel ? INK : ch ? ACCENT : WHITE);
                chipValue[i].setText(fmt(i, edit[i]));
            }
            hints.setEditMode(row != 0);
        } else if (overlay == 1) {
            panel.setVisibility(View.GONE); mini.setVisibility(View.VISIBLE);
            mini.setText(r.name + "   " + pos + (dirty ? "   · preview" : "   · stored"));
        } else {
            panel.setVisibility(View.GONE); mini.setVisibility(View.GONE);
        }
    }

    // ------------------------------------------------------------ input
    private void step(int dir) {              // LEFT/RIGHT or dial on current row
        if (row == 0 || overlay != 0) { recipe = (recipe + Recipes.ALL.length + dir) % Recipes.ALL.length; stageRecipe(); }
        else if (row == 6) edit[6] = edit[6] == 14 ? 1 : 14;
        else edit[row] = Math.max(ROW_MIN[row], Math.min(ROW_MAX[row], edit[row] + dir));
        applyPreview(); render();
    }

    private void nextRecipe(int dir) { recipe = (recipe + Recipes.ALL.length + dir) % Recipes.ALL.length; stageRecipe(); applyPreview(); render(); }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        switch (e.getScanCode()) {
            case K_LEFT: step(-1); return true;
            case K_RIGHT: step(+1); return true;
            case K_WHEEL_CW: nextRecipe(+1); return true;
            case K_WHEEL_CCW: nextRecipe(-1); return true;
            case K_DIAL_CW: step(+1); return true;
            case K_DIAL_CCW: step(-1); return true;
            case K_UP: if (overlay == 0) { row = (row + N - 1) % N; render(); } return true;
            case K_DOWN: if (overlay == 0) { row = (row + 1) % N; render(); } return true;
            case K_DELETE: case K_SK2: case K_C1: case K_DISP: case K_FN:
                overlay = (overlay + 1) % 3; render(); return true;
            case K_ENTER: writeAll(); return true;
            case K_AEL: recipe = 0; stageRecipe(); applyPreview(); showToast("Factory values staged — ENTER to store", 3000); render(); return true;
            case K_PLAY: return true;   // firmware opens playback anyway; nothing bound
            case K_S1: try { camera.autoFocus(null); } catch (Throwable t) {} return true;
            case K_S2: try { camera.takePicture(null, null, null); } catch (Throwable t) {} return true;
            case K_MENU: case K_SK1: return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true; }
        return super.onKeyDown(keyCode, e);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent e) {
        switch (e.getScanCode()) {
            case K_MENU: case K_SK1: finish(); return true;
            case K_S1: try { camera.cancelAutoFocus(); } catch (Throwable t) {} return true;
            case K_S2: try { cameraEx.getClass().getMethod("cancelTakePicture").invoke(cameraEx); } catch (Throwable t) {} return true;
            case K_UP: case K_DOWN: case K_LEFT: case K_RIGHT: case K_ENTER: case K_PLAY: case K_DISP: case K_FN:
            case K_DELETE: case K_SK2: case K_C1: case K_AEL: case K_WHEEL_CW: case K_WHEEL_CCW: case K_DIAL_CW: case K_DIAL_CCW: return true;
        }
        return super.onKeyUp(keyCode, e);
    }
}
