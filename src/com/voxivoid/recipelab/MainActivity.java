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
 * Recipe Lab 0.26 — film recipes with LIVE PREVIEW, then persistent write (photo + video, survives power-cycle).
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
            ID_PE = 0x010706f1, ID_EV = 0x010700b8, ID_DRO = 0 /* unknown: preview only */,
            ID_QFMT = 0 /* still file format slot: unknown yet */, ID_QJPG = 0 /* jpeg quality slot: unknown yet */;
    // Quality: 0 RAW, 1 RAW+JPEG, 2 JPEG Fine, 3 JPEG Std  — runtime keys storage-fmt / jpeg-quality; stored codes provisional
    private static final String[] Q_LABEL = { "RAW", "RAW+JPG", "JPG Fine", "JPG Std" };
    private static final String[] Q_FMT = { "raw", "rawjpeg", "jpeg", "jpeg" };
    private static final String[] Q_JPG = { "50", "50", "50", "25" };
    private static final int[] Q_FMT_CODE = { 1, 2, 0, 0 }, Q_JPG_CODE = { 1, 1, 1, 2 };   // provisional stored bytes

    private static final int R_RECIPE = 0, R_STYLE = 1, R_SAT = 2, R_CON = 3, R_SHARP = 4, R_MTX = 5, R_PE = 6, R_SUB = 7, R_WBMODE = 8, R_KELVIN = 9, R_AB = 10, R_GM = 11, R_EV = 12, R_DRO = 13, R_QUAL = 14;
    private static final String[] ROW_NAME = { "RECIPE", "STYLE", "SAT", "CON", "SHARP", "MATRIX", "EFFECT", "SUB", "WB", "KELVIN", "A-B", "G-M", "EV", "DRO*", "QUALITY*" };
    private static final int[] ROW_ID = { 0, ID_STYLE, ID_SAT, ID_CON, ID_SHARP, ID_PP_NO, ID_PE, -1 /* depends on effect */, ID_WB_MODE, ID_WB_TEMP, ID_WB_AB, ID_WB_GM, ID_EV, ID_DRO, -2 /* two slots */ };
    private static final int[] ROW_MIN = { 0, 1, -16, -8, -8, 0, 0, 0, 0, 25, -7, -7, -15, 0, 0 };
    private static final int[] ROW_MAX = { 0, 13, 16, 8, 8, 1, 13, 4, 20, 99, 7, 7, 15, 6, 3 };
    private static final int N = ROW_ID.length;
    /** chip display / navigation order (quality first) */
    private static final int[] ORDER = { R_QUAL, R_STYLE, R_SAT, R_CON, R_SHARP, R_MTX, R_PE, R_SUB, R_WBMODE, R_KELVIN, R_AB, R_GM, R_EV, R_DRO };
    // PP3 colour matrix measured on this body, Q10 fixed point (1.0 = 1024)
    private static final String PP3_MATRIX = "1331,-307,-51,-205,1331,-123,-20,-461,1485";

    private static final int ACCENT = 0xFFF2B85C, INK = 0xFF1A1208, WHITE = 0xFFFFFFFF, DIM = 0x99FFFFFF;

    private View panel;
    private PickerView picker;
    private HorizontalScrollView chipScroll;
    private boolean swallowMenuUp = false;
    private TextView name, badge, tag, count, meta, mini, toast;
    private PromptView prompt;
    private int promptSel = 0, promptMode = 0; private boolean promptOpen = false; private long fnDown = 0;   // promptMode 1 raw-vs-effect, 2 quality change
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
        tag = (TextView) findViewById(R.id.tag);
        count = (TextView) findViewById(R.id.count);
        meta = (TextView) findViewById(R.id.meta);
        hints = (HintBar) findViewById(R.id.hints);
        mini = (TextView) findViewById(R.id.mini);
        toast = (TextView) findViewById(R.id.toast);
        prompt = (PromptView) findViewById(R.id.prompt);
        chips = (LinearLayout) findViewById(R.id.chips);
        buildChips();
        SurfaceView sv = (SurfaceView) findViewById(R.id.surface);
        holder = sv.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private void buildChips() {
        for (int i : ORDER) {
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
    /** settings slot for a row; SUB depends on which effect is staged */
    private int slot(int i) { return i == R_SUB ? Recipes.subId(edit[R_PE]) : ROW_ID[i]; }
    private int rd(int id) throws NativeException { return (byte) NativeBackup.readByte(id); }
    private int rdu(int id) throws NativeException { return NativeBackup.readByte(id) & 0xff; }

    private void load() {
        try {
            for (int i = 1; i < N; i++) {
                int id = ROW_ID[i];
                if (id == 0) { cur[i] = edit[i] = Recipes.DRO_AUTO; continue; }      // no slot: assume camera default
                if (id == -1) { int sid = Recipes.subId(cur[R_PE]); cur[i] = edit[i] = sid == 0 ? 0 : rdu(sid); continue; }
                if (id == -2) { cur[i] = edit[i] = readQuality(); continue; }
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
        edit[R_PE] = r.pe; edit[R_EV] = r.ev; edit[R_DRO] = r.dro; edit[R_SUB] = r.sub;
        edit[R_QUAL] = r.isEffect() ? 2 : 1;                     // Picture Effect recipes → JPEG Fine, everything else → RAW+JPEG
    }

    /** quality from the two stored bytes; falls back to the runtime value when the slots are not known yet */
    private int readQuality() {
        try {
            if (ID_QFMT != 0) {
                int f = rdu(ID_QFMT), j = ID_QJPG != 0 ? rdu(ID_QJPG) : Q_JPG_CODE[2];
                for (int q = 0; q < 4; q++) if (Q_FMT_CODE[q] == f && (q < 2 || Q_JPG_CODE[q] == j)) return q;
            }
            if (camera != null) {
                String fmt = camera.getParameters().get("storage-fmt"), jq = camera.getParameters().get("jpeg-quality");
                if ("raw".equals(fmt)) return 0; if ("rawjpeg".equals(fmt)) return 1; return "25".equals(jq) ? 3 : 2;
            }
        } catch (Throwable t) {}
        return 2;
    }
    private boolean qualityIsRaw() { return edit[R_QUAL] <= 1; }
    private boolean qualityPersistent() { return ID_QFMT != 0; }

    /** stored value of the SUB slot for the staged effect (the slot changes with the effect) */
    private int storedSub() { int sid = Recipes.subId(edit[R_PE]); if (sid == 0) return edit[R_SUB]; try { return rdu(sid); } catch (Throwable t) { return edit[R_SUB]; } }

    private boolean qualityChanges() { return edit[R_QUAL] != cur[R_QUAL]; }

    private boolean rowDirty(int i) {
        if (i == R_QUAL) return edit[i] != cur[i];
        if (i == R_SUB) return Recipes.subId(edit[R_PE]) != 0 && edit[i] != storedSub();
        return ROW_ID[i] != 0 && edit[i] != cur[i];
    }
    private boolean dirty() { for (int i = 1; i < N; i++) if (rowDirty(i)) return true; return false; }

    /** which chips make sense for what is staged */
    private boolean rowVisible(int i) {
        boolean pe = edit[R_PE] != 0;
        switch (i) {
            case R_STYLE: case R_SAT: case R_CON: case R_SHARP: case R_MTX: return !pe;
            case R_SUB: return pe && Recipes.subId(edit[R_PE]) != 0;
            case R_KELVIN: return edit[R_WBMODE] == 14;
            default: return true;
        }
    }

    private void writeAll() { writeAll(false); }

    private void writeAll(boolean confirmed) {
        if (!confirmed && qualityChanges()) { openPrompt(2); return; }
        if (!confirmed && edit[R_PE] != 0 && qualityIsRaw()) { openPrompt(1); return; }
        if (!dirty()) { showToast("Already stored — nothing to write", 2500); return; }
        String msg;
        try {
            int n = 0;
            for (int i = 1; i < N; i++) {
                if (!rowDirty(i)) continue;
                if (i == R_QUAL) {
                    if (!qualityPersistent()) continue;             // slot unknown yet: live view only
                    NativeBackup.writeByte(ID_QFMT, Q_FMT_CODE[edit[i]]);
                    if (ID_QJPG != 0) NativeBackup.writeByte(ID_QJPG, Q_JPG_CODE[edit[i]]);
                    n++; continue;
                }
                int id = slot(i), v = edit[i];
                if (id == ID_PP_NO) v = (v == 0) ? 0 : 3;
                NativeBackup.writeByte(id, v);
                n++;
            }
            NativeBackup.sync();
            msg = "Stored " + n + " value" + (n == 1 ? "" : "s") + " — power-cycle the camera to apply everywhere";
        } catch (Throwable t) { msg = "WRITE FAILED: " + t.getMessage(); }
        load(); stageRecipe();
        showToast(msg, 5000); render();
    }

    // ------------------------------------------------------------ RAW vs Picture Effect prompt
    /** the four quality choices as pills; the recipe's suggestion preselected, the current one marked */
    private String[] promptOpts() {
        String[] o = new String[4];
        for (int i = 0; i < 4; i++) o[i] = Q_LABEL[i] + (i == cur[R_QUAL] ? " (now)" : "");
        return o;
    }

    private void openPrompt(int mode) { promptMode = mode; promptOpen = true; promptSel = edit[R_QUAL]; renderPrompt(); }

    private void renderPrompt() {
        String t = "Store with which Quality?";
        String b = edit[R_PE] != 0
                ? "Picture Effect recipe: the camera drops the effect when RAW is on — JPEG needed."
                : "Creative Style recipe: RAW+JPEG keeps a RAW you can still edit; the look lands on the JPEG.";
        String n = edit[R_PE] != 0 && promptSel <= 1 ? "with this choice the effect will NOT be applied" : null;
        if (!qualityPersistent()) n = (n == null ? "" : n + "  ·  ") + "quality slot not located yet — live view only";
        prompt.set(t, b, promptOpts(), promptSel, n);
        prompt.setVisibility(View.VISIBLE);
    }

    private void closePrompt() { prompt.setVisibility(View.GONE); promptOpen = false; }

    private boolean promptKey(int sc) {
        switch (sc) {
            case K_LEFT: case K_WHEEL_CCW: case K_DIAL_CCW: promptSel = (promptSel + 3) % 4; renderPrompt(); return true;
            case K_RIGHT: case K_WHEEL_CW: case K_DIAL_CW: promptSel = (promptSel + 1) % 4; renderPrompt(); return true;
            case K_ENTER:
                closePrompt();
                edit[R_QUAL] = promptSel; applyPreview(); writeAll(true);
                render(); return true;
            case K_MENU: case K_SK1: swallowMenuUp = true; closePrompt(); render(); return true;
        }
        return true;
    }

    private void cycleQuality() {
        edit[R_QUAL] = (edit[R_QUAL] + 1) % 4; applyPreview(); render();
        showToast("Quality: " + Q_LABEL[edit[R_QUAL]] + (qualityPersistent() ? "  — ENTER to store" : "  (live view only until the slot is known)"), 2500);
    }

    // ------------------------------------------------------------ snapshot / diff of the whole settings store (Fn long-press)
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
            p.set("storage-fmt", Q_FMT[edit[R_QUAL]]); p.set("jpeg-quality", Q_JPG[edit[R_QUAL]]);
            p.set("picture-effect", Recipes.PE_KEYS[edit[R_PE]]);
            String sk = Recipes.subKey(edit[R_PE]); String[] sv = Recipes.subValues(edit[R_PE]);
            if (sk != null && sv != null && edit[R_SUB] >= 0 && edit[R_SUB] < sv.length) p.set(sk, sv[edit[R_SUB]]);
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
            case R_SUB: { String l = Recipes.subLabel(edit[R_PE], v); return l == null ? "-" : l; }
            case R_EV: return Recipes.evLabel(v);
            case R_DRO: return Recipes.droLabel(v);
            case R_QUAL: return v >= 0 && v < 4 ? Q_LABEL[v] : "?" + v;
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
            tag.setText(edit[R_PE] != 0 ? "PE" : "CS");
            tag.setTextColor(edit[R_PE] != 0 ? ACCENT : 0xDDFFFFFF);
            if (protectedStore) { badge.setText("PROTECTED"); badge.setBackgroundResource(R.drawable.badge_err); }
            else if (dirty) { badge.setText("PREVIEW"); badge.setBackgroundResource(R.drawable.badge_warn); }
            else { badge.setText("STORED"); badge.setBackgroundResource(R.drawable.badge_ok); }
            StringBuilder m = new StringBuilder();
            if (edit[R_PE] != 0) { m.append("Picture Effect ").append(Recipes.PE_LABEL[edit[R_PE]]); String sl = Recipes.subLabel(edit[R_PE], edit[R_SUB]); if (sl != null) m.append(' ').append(sl); m.append(" (Creative Style ignored, JPEG only)"); }
            else m.append(styleName(edit[R_STYLE]));
            m.append("  ·  WB ").append(edit[R_WBMODE] == 14 ? (edit[R_KELVIN] * 100) + "K" : edit[R_WBMODE] == 1 ? "auto" : "mode " + edit[R_WBMODE]);
            if (edit[R_MTX] == 1 && edit[R_PE] == 0) m.append("  ·  PP3 matrix");
            if (edit[R_EV] != 0) m.append("  ·  EV ").append(Recipes.evLabel(edit[R_EV]));
            if (edit[R_DRO] != Recipes.DRO_AUTO) m.append("  ·  DRO ").append(Recipes.droLabel(edit[R_DRO])).append(" (preview only)");
            if (qualityChanges()) m.append("  ·  QUALITY → ").append(Q_LABEL[edit[R_QUAL]]).append(" (now ").append(Q_LABEL[cur[R_QUAL]]).append(")");
            if (edit[R_PE] != 0 && qualityIsRaw()) m.append("  ·  RAW is on: effect ignored — ENTER asks to switch to JPEG");
            if (!previewOk) m.append("  ·  no live preview: ").append(previewErr);
            else if (row == R_DRO || row == R_PE) m.append("  ·  ").append(cinematone);
            meta.setText(m);
            for (int i = 1; i < N; i++) {
                chip[i].setVisibility(rowVisible(i) ? View.VISIBLE : View.GONE);
                boolean sel = i == row, ch = rowDirty(i);
                chip[i].setBackgroundResource(sel ? R.drawable.chip_sel : R.drawable.chip);
                chipLabel[i].setTextColor(sel ? INK : DIM);
                chipValue[i].setTextColor(sel ? INK : ch ? ACCENT : WHITE);
                chipValue[i].setText(fmt(i, edit[i]));
            }
            if (row == 0) chipScroll.post(new Runnable() { public void run() { chipScroll.smoothScrollTo(0, 0); } });
            else {
                final View c = chip[row];
                chipScroll.post(new Runnable() { public void run() {
                    int l = c.getLeft(), rgt = c.getRight(), sx = chipScroll.getScrollX(), w = chipScroll.getWidth();
                    if (l < sx) chipScroll.smoothScrollTo(l - dp(8), 0); else if (rgt > sx + w) chipScroll.smoothScrollTo(rgt - w + dp(8), 0);
                } });
            }
            hints.setEditMode(row != 0);
        } else if (overlay == 1) {
            panel.setVisibility(View.GONE); mini.setVisibility(View.VISIBLE);
            mini.setText((edit[R_PE] != 0 ? "PE  " : "CS  ") + r.name + "   " + pos + (dirty ? "   · preview" : "   · stored") + (qualityChanges() ? "   · quality → " + Q_LABEL[edit[R_QUAL]] : ""));
        } else {
            panel.setVisibility(View.GONE); mini.setVisibility(View.GONE);
        }
    }

    // ------------------------------------------------------------ input
    private void step(int dir) {              // LEFT/RIGHT or dial on current row
        if (row == 0 || overlay != 0) { recipe = (recipe + Recipes.ALL.length + dir) % Recipes.ALL.length; stageRecipe(); }
        else if (row == R_WBMODE) edit[R_WBMODE] = edit[R_WBMODE] == 14 ? 1 : 14;
        else if (row == R_SUB) { String[] sv = Recipes.subValues(edit[R_PE]); int n = sv == null ? 1 : sv.length; edit[R_SUB] = (edit[R_SUB] + n + dir) % n; }
        else {
            edit[row] = Math.max(ROW_MIN[row], Math.min(ROW_MAX[row], edit[row] + dir));
            if (row == R_PE) edit[R_SUB] = 0;                       // effect changed → sub-parameter starts at its first value
        }
        applyPreview(); render();
    }

    private void moveRow(int dir) {
        int pos = -1;                                              // -1 = recipe row
        for (int k = 0; k < ORDER.length; k++) if (ORDER[k] == row) pos = k;
        for (int k = 0; k <= ORDER.length; k++) {
            pos += dir;
            if (pos < -1) pos = ORDER.length - 1; else if (pos >= ORDER.length) pos = -1;
            row = pos < 0 ? 0 : ORDER[pos];
            if (row == 0 || rowVisible(row)) break;
        }
        render();
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

    /** handle keys before any focusable view (the chip scroller would otherwise eat LEFT/RIGHT) */
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN) return onKeyDown(e.getKeyCode(), e);
        if (e.getAction() == KeyEvent.ACTION_UP) return onKeyUp(e.getKeyCode(), e);
        return super.dispatchKeyEvent(e);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (promptOpen) return promptKey(e.getScanCode());
        if (e.getScanCode() == K_FN) { if (e.getRepeatCount() == 0) fnDown = e.getEventTime(); return true; }
        if (overlay == 3 && e.getScanCode() != K_PLAY) return browserKey(e.getScanCode());
        switch (e.getScanCode()) {
            case K_LEFT: step(-1); return true;
            case K_RIGHT: step(+1); return true;
            case K_WHEEL_CW: nextRecipe(+1); return true;
            case K_WHEEL_CCW: nextRecipe(-1); return true;
            case K_DIAL_CW: step(+1); return true;
            case K_DIAL_CCW: step(-1); return true;
            case K_UP: if (overlay == 0) moveRow(-1); return true;
            case K_DOWN: if (overlay == 0) moveRow(+1); return true;
            case K_AEL: case K_DISP: overlay = (overlay + 1) % 3; render(); return true;
            case K_C1: openBrowser(true); return true;
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
        if (promptOpen) { if (e.getScanCode() == K_MENU || e.getScanCode() == K_SK1) swallowMenuUp = false; return true; }
        switch (e.getScanCode()) {
            case K_FN: if (e.getEventTime() - fnDown > 1000) snapshotOrDiff(); else cycleQuality(); return true;
            case K_MENU: case K_SK1: if (swallowMenuUp) { swallowMenuUp = false; return true; } finish(); return true;
            case K_S1: try { camera.cancelAutoFocus(); } catch (Throwable t) {} return true;
            case K_S2: try { cameraEx.getClass().getMethod("cancelTakePicture").invoke(cameraEx); } catch (Throwable t) {} return true;
            case K_UP: case K_DOWN: case K_LEFT: case K_RIGHT: case K_ENTER: case K_PLAY: case K_DISP:
            case K_DELETE: case K_SK2: case K_C1: case K_AEL: case K_WHEEL_CW: case K_WHEEL_CCW: case K_DIAL_CW: case K_DIAL_CCW: return true;
        }
        return super.onKeyUp(keyCode, e);
    }
}
