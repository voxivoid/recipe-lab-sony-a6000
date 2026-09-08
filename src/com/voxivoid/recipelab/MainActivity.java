package com.voxivoid.recipelab;

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
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Recipe Lab 0.18 — film recipes with LIVE PREVIEW, then persistent write (photo + video, survives power-cycle).
 *
 * Preview = runtime camera parameters. ENTER = write the recipe's stored bytes + sync → power-cycle applies it everywhere.
 *
 * Keys: wheel / LEFT / RIGHT recipe · UP / DOWN parameter · top dial adjust · C1 brand browser · ENTER store
 *       AEL / DISP overlay: full → pill → hidden · TRASH stage factory · Fn settings snapshot / diff (finds storage slots)
 *       SHUTTER photo · MENU exit
 */
public class MainActivity extends Activity implements SurfaceHolder.Callback {
    // ScalarInput scan codes
    private static final int K_UP = 103, K_DOWN = 108, K_LEFT = 105, K_RIGHT = 106, K_ENTER = 232, K_MENU = 514, K_SK1 = 229,
            K_DELETE = 595, K_SK2 = 513, K_PLAY = 207, K_DISP = 608, K_FN = 520, K_AEL = 532, K_C1 = 622, K_S1 = 516, K_S2 = 518,
            K_WHEEL_CW = 522, K_WHEEL_CCW = 523, K_DIAL_CW = 525, K_DIAL_CCW = 526;

    private static final int ID_STYLE = 0x01070175, ID_CON = 0x01070178, ID_SAT = 0x01070187, ID_SHARP = 0x0107018a, ID_PP_NO = 0x0107031c,
            ID_WB_MODE = 0x01070019, ID_WB_TEMP = 0x01070018, ID_WB_AB = 0x01070017, ID_WB_GM = 0x01070016,
            ID_PE = 0x010706f1, ID_EV = 0x010700b8, ID_DRO = 0 /* unknown: preview only */;

    private static final int R_RECIPE = 0, R_STYLE = 1, R_SAT = 2, R_CON = 3, R_SHARP = 4, R_MTX = 5, R_WBMODE = 6, R_KELVIN = 7, R_AB = 8, R_GM = 9, R_PE = 10, R_EV = 11, R_DRO = 12;
    private static final String[] ROW_NAME = { "RECIPE", "STYLE", "SAT", "CON", "SHARP", "MATRIX", "WB", "KELVIN", "A-B", "G-M", "EFFECT", "EV", "DRO*" };
    private static final int[] ROW_ID = { 0, ID_STYLE, ID_SAT, ID_CON, ID_SHARP, ID_PP_NO, ID_WB_MODE, ID_WB_TEMP, ID_WB_AB, ID_WB_GM, ID_PE, ID_EV, ID_DRO };
    private static final int[] ROW_MIN = { 0, 1, -16, -8, -8, 0, 0, 25, -7, -7, 0, -15, 0 };
    private static final int[] ROW_MAX = { 0, 13, 16, 8, 8, 1, 20, 99, 7, 7, 13, 15, 6 };
    private static final int N = ROW_ID.length;
    // PP3 colour matrix measured on this body, Q10 fixed point (1.0 = 1024)
    private static final String PP3_MATRIX = "1331,-307,-51,-205,1331,-123,-20,-461,1485";

    private static final int ACCENT = 0xFFF2B85C, INK = 0xFF1A1208, WHITE = 0xFFFFFFFF, DIM = 0x99FFFFFF;

    private View panel;
    private PickerView picker;
    private HorizontalScrollView chipScroll;
    private boolean swallowMenuUp = false;
    private TextView name, badge, count, meta, mini, toast;
    private HintBar hints;
    private LinearLayout chips;
    private final TextView[] chipLabel = new TextView[N], chipValue = new TextView[N];
    private final View[] chip = new View[N];
    private final Handler handler = new Handler();
    private final Runnable hideToast = new Runnable() { public void run() { toast.setVisibility(View.GONE); } };

    private SurfaceHolder holder;
    private Object cameraEx; private Camera camera; private String origFlat;
    private int row = 0, recipe = 0, overlay = 0;     // overlay: 0 full, 1 pill, 2 hidden, 3 browser
    private final int[] cur = new int[N], edit = new int[N];
    private boolean protectedStore = false, previewOk = false;
    private String previewErr = "", cinematone = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.main);
        panel = findViewById(R.id.panel);
        picker = (PickerView) findViewById(R.id.picker);
        chipScroll = (HorizontalScrollView) findViewById(R.id.chipscroll);
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
            probeCinematone();
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

    /** Does this body expose Sony's camcorder Cinematone gamma at runtime? (result shown in the meta line) */
    private void probeCinematone() {
        try {
            Method mk = cameraEx.getClass().getMethod("createParametersModifier", Camera.Parameters.class);
            Object pm = mk.invoke(cameraEx, camera.getParameters());
            Object list = pm.getClass().getMethod("getSupportedCinemaTones").invoke(pm);
            cinematone = list == null ? "cinematone: null" : "cinematone: " + list;
        } catch (Throwable t) { cinematone = "cinematone: n/a (" + t.getClass().getSimpleName() + ")"; }
    }

    // ------------------------------------------------------------ stored settings
    private int rd(int id) throws NativeException { return (byte) NativeBackup.readByte(id); }
    private int rdu(int id) throws NativeException { return NativeBackup.readByte(id) & 0xff; }

    private void load() {
        try {
            for (int i = 1; i < N; i++) {
                int id = ROW_ID[i];
                if (id == 0) { cur[i] = edit[i] = Recipes.DRO_AUTO; continue; }      // no slot: assume camera default
                int v = (id == ID_WB_TEMP || id == ID_WB_MODE || id == ID_STYLE || id == ID_PE) ? rdu(id) : rd(id);
                if (id == ID_PP_NO) v = (v == 0) ? 0 : 1;
                cur[i] = v; edit[i] = v;
            }
            protectedStore = NativeBackup.isProtected();
        } catch (Throwable t) { showToast("Read failed: " + t.getMessage(), 0); }
    }

    private void stageRecipe() {
        Recipes.Recipe r = Recipes.ALL[recipe];
        edit[R_STYLE] = r.style; edit[R_SAT] = r.sat; edit[R_CON] = r.con; edit[R_SHARP] = r.sharp; edit[R_MTX] = r.matrix;
        if (r.wbMode != 0) { edit[R_WBMODE] = r.wbMode; if (r.wbMode == 14) edit[R_KELVIN] = r.kelvin / 100; }
        edit[R_AB] = r.ab; edit[R_GM] = r.gm;
        edit[R_PE] = r.pe; edit[R_EV] = r.ev; edit[R_DRO] = r.dro;
    }

    private boolean dirty() { for (int i = 1; i < N; i++) if (ROW_ID[i] != 0 && edit[i] != cur[i]) return true; return false; }

    private void writeAll() {
        if (!dirty()) { showToast("Already stored — nothing to write", 2500); return; }
        String msg;
        try {
            int n = 0;
            for (int i = 1; i < N; i++) {
                if (ROW_ID[i] == 0 || edit[i] == cur[i]) continue;
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

    // ------------------------------------------------------------ snapshot / diff of the whole settings store (Fn)
    private File snapFile() { return new File(getFilesDir(), "snapshot.bin"); }

    private List<int[]> idList() {
        List<int[]> ids = new ArrayList<int[]>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.ids)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] t = line.trim().split(" ");
                if (t.length == 2) ids.add(new int[] { (int) Long.parseLong(t[0], 16), Integer.parseInt(t[1]) });
            }
            br.close();
        } catch (Throwable t) {}
        return ids;
    }

    private void snapshotOrDiff() {
        List<int[]> ids = idList();
        File f = snapFile();
        try {
            if (!f.exists()) {
                FileOutputStream o = new FileOutputStream(f);
                for (int[] e : ids) { byte[] v; try { v = NativeBackup.read(e[0]); } catch (Throwable t) { v = new byte[0]; } o.write(v.length); o.write(v); }
                o.close();
                showToast("Snapshot of " + ids.size() + " settings taken. Change a menu setting, reopen, press Fn again.", 6000);
                return;
            }
            FileInputStream in = new FileInputStream(f);
            StringBuilder sb = new StringBuilder(); int changed = 0;
            for (int[] e : ids) {
                int len = in.read(); byte[] old = new byte[Math.max(0, len)]; if (len > 0) in.read(old);
                byte[] now; try { now = NativeBackup.read(e[0]); } catch (Throwable t) { now = new byte[0]; }
                if (!java.util.Arrays.equals(old, now)) {
                    changed++;
                    if (changed <= 8) sb.append(String.format("%08x:", e[0])).append(hex(old)).append(">").append(hex(now)).append("  ");
                }
            }
            in.close(); f.delete();
            String text = changed + " changed  " + sb;
            java.io.FileWriter w = new java.io.FileWriter(new File(getFilesDir(), "diff.txt"), true); w.write(text + "\n"); w.close();
            showToast(text, 0);
        } catch (Throwable t) { showToast("snapshot error: " + t, 0); }
    }

    private static String hex(byte[] b) { StringBuilder s = new StringBuilder(); for (int i = 0; i < Math.min(b.length, 4); i++) s.append(String.format("%02x", b[i])); return s.toString(); }

    // ------------------------------------------------------------ live preview (runtime params)
    private void applyPreview() {
        if (camera == null) return;
        try {
            Camera.Parameters p = camera.getParameters();
            int st = edit[R_STYLE];
            p.set("color-mode", st >= 1 && st < Recipes.STYLE_NAMES.length ? Recipes.STYLE_NAMES[st] : "standard");
            p.set("saturation", String.valueOf(edit[R_SAT]));
            p.set("contrast", String.valueOf(Math.max(-3, Math.min(3, edit[R_CON]))));
            p.set("sharpness", String.valueOf(Math.max(-3, Math.min(3, edit[R_SHARP]))));
            if (edit[R_MTX] == 1) { p.set("rgb-matrix", PP3_MATRIX); p.set("rgb-matrix-mode", "true"); } else p.set("rgb-matrix-mode", "false");
            if (edit[R_WBMODE] == 14) { p.set("whitebalance", "color-temp"); p.set("color-temperture-white-balance", String.valueOf(edit[R_KELVIN] * 100)); }
            else if (edit[R_WBMODE] == 1) p.set("whitebalance", "auto");
            p.set("light-balance-for-white-balance", String.valueOf(edit[R_AB]));
            p.set("color-compensation-for-white-balance", String.valueOf(edit[R_GM]));
            p.set("picture-effect", Recipes.PE_KEYS[edit[R_PE]]);
            p.set("exposure-compensation", String.valueOf(edit[R_EV]));
            int dro = edit[R_DRO];
            if (dro == Recipes.DRO_AUTO) p.set("dro-mode", "auto");
            else if (dro == 0) p.set("dro-mode", "off");
            else { p.set("dro-mode", "on"); p.set("dro-level", String.valueOf(dro)); }
            camera.setParameters(p);
            previewOk = true;
        } catch (Throwable t) { previewOk = false; previewErr = String.valueOf(t.getMessage()); }
    }

    // ------------------------------------------------------------ UI
    private String styleName(int v) { return v >= 1 && v < Recipes.STYLE_LABEL.length ? Recipes.STYLE_LABEL[v] : "?" + v; }

    private String fmt(int i, int v) {
        switch (i) {
            case R_STYLE: return styleName(v);
            case R_MTX: return v == 0 ? "off" : "PP3";
            case R_WBMODE: return v == 1 ? "auto" : v == 14 ? "kelvin" : String.valueOf(v);
            case R_KELVIN: return edit[R_WBMODE] == 14 ? (v * 100) + "K" : "-";
            case R_AB: return v == 0 ? "0" : (v > 0 ? "A" + v : "B" + (-v));
            case R_GM: return v == 0 ? "0" : (v > 0 ? "G" + v : "M" + (-v));
            case R_PE: return v >= 0 && v < Recipes.PE_LABEL.length ? Recipes.PE_LABEL[v] : "?" + v;
            case R_EV: return Recipes.evLabel(v);
            case R_DRO: return Recipes.droLabel(v);
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
        String grp = Recipes.GROUPS[r.group].toUpperCase();
        picker.setVisibility(overlay == 3 ? View.VISIBLE : View.GONE);
        if (overlay == 3) { panel.setVisibility(View.GONE); mini.setVisibility(View.GONE); picker.setSelected(recipe); return; }
        if (overlay == 0) {
            panel.setVisibility(View.VISIBLE); mini.setVisibility(View.GONE);
            name.setText(r.name);
            name.setTextColor(row == 0 ? ACCENT : WHITE);
            count.setText(grp + "   " + pos);
            if (protectedStore) { badge.setText("PROTECTED"); badge.setBackgroundResource(R.drawable.badge_err); }
            else if (dirty) { badge.setText("PREVIEW"); badge.setBackgroundResource(R.drawable.badge_warn); }
            else { badge.setText("STORED"); badge.setBackgroundResource(R.drawable.badge_ok); }
            StringBuilder m = new StringBuilder();
            if (edit[R_PE] != 0) m.append("Picture Effect ").append(Recipes.PE_LABEL[edit[R_PE]]).append(" (style ignored, JPEG only)");
            else m.append(styleName(edit[R_STYLE]));
            m.append("  ·  WB ").append(edit[R_WBMODE] == 14 ? (edit[R_KELVIN] * 100) + "K" : edit[R_WBMODE] == 1 ? "auto" : "mode " + edit[R_WBMODE]);
            if (edit[R_MTX] == 1 && edit[R_PE] == 0) m.append("  ·  PP3 matrix");
            if (edit[R_EV] != 0) m.append("  ·  EV ").append(Recipes.evLabel(edit[R_EV]));
            if (edit[R_DRO] != Recipes.DRO_AUTO) m.append("  ·  DRO ").append(Recipes.droLabel(edit[R_DRO])).append(" (preview only)");
            if (!previewOk) m.append("  ·  no live preview: ").append(previewErr);
            else if (row == R_DRO || row == R_PE) m.append("  ·  ").append(cinematone);
            meta.setText(m);
            for (int i = 1; i < N; i++) {
                boolean sel = i == row, ch = ROW_ID[i] != 0 && edit[i] != cur[i];
                chip[i].setBackgroundResource(sel ? R.drawable.chip_sel : R.drawable.chip);
                chipLabel[i].setTextColor(sel ? INK : DIM);
                chipValue[i].setTextColor(sel ? INK : ch ? ACCENT : WHITE);
                chipValue[i].setText(fmt(i, edit[i]));
            }
            if (row > 0) {
                final View c = chip[row];
                chipScroll.post(new Runnable() { public void run() {
                    int l = c.getLeft(), rgt = c.getRight(), sx = chipScroll.getScrollX(), w = chipScroll.getWidth();
                    if (l < sx) chipScroll.smoothScrollTo(l - dp(8), 0); else if (rgt > sx + w) chipScroll.smoothScrollTo(rgt - w + dp(8), 0);
                } });
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
        else if (row == R_WBMODE) edit[R_WBMODE] = edit[R_WBMODE] == 14 ? 1 : 14;
        else edit[row] = Math.max(ROW_MIN[row], Math.min(ROW_MAX[row], edit[row] + dir));
        applyPreview(); render();
    }

    private void nextRecipe(int dir) { recipe = (recipe + Recipes.ALL.length + dir) % Recipes.ALL.length; stageRecipe(); applyPreview(); render(); }

    private void nextGroup(int dir) {
        int g = (Recipes.ALL[recipe].group + Recipes.GROUPS.length + dir) % Recipes.GROUPS.length;
        recipe = Recipes.GROUP_START[g]; stageRecipe(); applyPreview(); render();
    }

    private void openBrowser(boolean open) { overlay = open ? 3 : 0; row = 0; render(); }

    private void stageFactory() { recipe = 0; stageRecipe(); applyPreview(); showToast("Factory values staged — ENTER to store", 3000); render(); }

    private boolean browserKey(int sc) {
        switch (sc) {
            case K_UP: case K_WHEEL_CCW: case K_DIAL_CCW: nextRecipe(-1); return true;
            case K_DOWN: case K_WHEEL_CW: case K_DIAL_CW: nextRecipe(+1); return true;
            case K_LEFT: nextGroup(-1); return true;
            case K_RIGHT: nextGroup(+1); return true;
            case K_ENTER: openBrowser(false); showToast(Recipes.ALL[recipe].name + " previewed — ENTER to store", 3000); return true;
            case K_MENU: case K_SK1: swallowMenuUp = true; openBrowser(false); return true;
            case K_C1: case K_AEL: case K_DISP: openBrowser(false); return true;
            case K_DELETE: case K_SK2: stageFactory(); return true;
            case K_S1: try { camera.autoFocus(null); } catch (Throwable t) {} return true;
            case K_S2: try { camera.takePicture(null, null, null); } catch (Throwable t) {} return true;
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (overlay == 3 && e.getScanCode() != K_PLAY) return browserKey(e.getScanCode());
        switch (e.getScanCode()) {
            case K_LEFT: step(-1); return true;
            case K_RIGHT: step(+1); return true;
            case K_WHEEL_CW: nextRecipe(+1); return true;
            case K_WHEEL_CCW: nextRecipe(-1); return true;
            case K_DIAL_CW: step(+1); return true;
            case K_DIAL_CCW: step(-1); return true;
            case K_UP: if (overlay == 0) { row = (row + N - 1) % N; render(); } return true;
            case K_DOWN: if (overlay == 0) { row = (row + 1) % N; render(); } return true;
            case K_AEL: case K_DISP: overlay = (overlay + 1) % 3; render(); return true;
            case K_C1: openBrowser(true); return true;
            case K_FN: snapshotOrDiff(); return true;
            case K_ENTER: writeAll(); return true;
            case K_DELETE: case K_SK2: stageFactory(); return true;
            case K_S1: try { camera.autoFocus(null); } catch (Throwable t) {} return true;
            case K_S2: try { camera.takePicture(null, null, null); } catch (Throwable t) {} return true;
            case K_MENU: case K_SK1: case K_PLAY: return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true; }
        return super.onKeyDown(keyCode, e);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent e) {
        switch (e.getScanCode()) {
            case K_MENU: case K_SK1: if (swallowMenuUp) { swallowMenuUp = false; return true; } finish(); return true;
            case K_S1: try { camera.cancelAutoFocus(); } catch (Throwable t) {} return true;
            case K_S2: try { cameraEx.getClass().getMethod("cancelTakePicture").invoke(cameraEx); } catch (Throwable t) {} return true;
            case K_UP: case K_DOWN: case K_LEFT: case K_RIGHT: case K_ENTER: case K_PLAY: case K_DISP: case K_FN:
            case K_DELETE: case K_SK2: case K_C1: case K_AEL: case K_WHEEL_CW: case K_WHEEL_CCW: case K_DIAL_CW: case K_DIAL_CCW: return true;
        }
        return super.onKeyUp(keyCode, e);
    }
}
