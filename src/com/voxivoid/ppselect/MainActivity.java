package com.voxivoid.ppselect;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import java.lang.reflect.Method;

/**
 * PP Select 0.5 — film recipes with LIVE PREVIEW, then persistent write (photo + video, survives power-cycle).
 *
 * Preview = runtime camera parameters (exact for colour mode / sat / con / sharp / WB / matrix).
 * ENTER  = write the recipe's stored bytes (Creative Style + WB slots) + sync → power-cycle applies it everywhere.
 *
 * Keys: LEFT/RIGHT recipe (previewed instantly) · UP/DOWN row (full overlay only) · DISP overlay: full → bar → hidden
 *       ENTER write+sync · PLAY stage factory · SHUTTER photo (preview look) · MENU exit (runtime look reverts)
 */
public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final int K_UP = 103, K_DOWN = 108, K_LEFT = 105, K_RIGHT = 106, K_ENTER = 232, K_MENU = 514, K_SK1 = 229,
            K_DELETE = 595, K_SK2 = 513, K_PLAY = 207, K_DISP = 608, K_FN = 520, K_S1 = 516, K_S2 = 518;

    private static final int ID_STYLE = 0x01070175, ID_CON = 0x01070178, ID_SAT = 0x01070187, ID_SHARP = 0x0107018a, ID_PP_NO = 0x0107031c,
            ID_WB_MODE = 0x01070019, ID_WB_TEMP = 0x01070018, ID_WB_AB = 0x01070017, ID_WB_GM = 0x01070016;

    private static final String[] ROW_NAME = { "Recipe", "Style", "Saturation", "Contrast", "Sharpness", "Colour matrix", "WB mode", "WB Kelvin", "WB A-B", "WB G-M" };
    private static final int[] ROW_ID = { 0, ID_STYLE, ID_SAT, ID_CON, ID_SHARP, ID_PP_NO, ID_WB_MODE, ID_WB_TEMP, ID_WB_AB, ID_WB_GM };
    private static final int[] ROW_MIN = { 0, 1, -16, -8, -8, 0, 0, 25, -7, -7 };
    private static final int[] ROW_MAX = { 0, 13, 16, 8, 8, 1, 20, 99, 7, 7 };
    private static final int N = ROW_ID.length;
    // PP3 colour matrix measured on this body, Q10 fixed point (1.0 = 1024)
    private static final String PP3_MATRIX = "1331,-307,-51,-205,1331,-123,-20,-461,1485";

    private TextView text, bar;
    private SurfaceHolder holder;
    private Object cameraEx; private Camera camera; private String origFlat;
    private int row = 0, recipe = 0, overlay = 0;     // overlay: 0 full, 1 bar only, 2 hidden
    private final int[] cur = new int[N], edit = new int[N];
    private String status = "", preview = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.main);
        text = (TextView) findViewById(R.id.text);
        bar = (TextView) findViewById(R.id.bar);
        SurfaceView sv = (SurfaceView) findViewById(R.id.surface);
        holder = sv.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
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
            preview = "preview ready";
        } catch (Throwable t) { preview = "no preview: " + t; }
        stageRecipe(); applyPreview(); render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        holder.removeCallback(this);
        try { if (camera != null && origFlat != null) { Camera.Parameters p = camera.getParameters(); p.unflatten(origFlat); camera.setParameters(p); } } catch (Throwable t) {}
        try { if (camera != null) camera.stopPreview(); } catch (Throwable t) {}
        try { if (cameraEx != null) cameraEx.getClass().getMethod("release").invoke(cameraEx); } catch (Throwable t) {}
        cameraEx = null; camera = null;
    }

    public void surfaceCreated(SurfaceHolder h) { try { camera.setPreviewDisplay(h); camera.startPreview(); } catch (Throwable t) { preview = "preview: " + t; render(); } }
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
            status = "stored settings loaded · protection=" + (NativeBackup.isProtected() ? "ON" : "off");
        } catch (Throwable t) { status = "read failed: " + t.getMessage(); }
    }

    private void stageRecipe() {
        Recipes.Recipe r = Recipes.ALL[recipe];
        edit[1] = r.style; edit[2] = r.sat; edit[3] = r.con; edit[4] = r.sharp; edit[5] = r.matrix;
        if (r.wbMode != 0) { edit[6] = r.wbMode; if (r.wbMode == 14) edit[7] = r.kelvin / 100; }
        edit[8] = r.ab; edit[9] = r.gm;
    }

    private void writeAll() {
        StringBuilder s = new StringBuilder();
        try {
            for (int i = 1; i < N; i++) {
                if (edit[i] == cur[i]) continue;
                int v = edit[i];
                if (ROW_ID[i] == ID_PP_NO) v = (v == 0) ? 0 : 3;
                NativeBackup.writeByte(ROW_ID[i], v);
                s.append(ROW_NAME[i]).append(' ');
            }
            if (s.length() == 0) { status = "nothing to write (already stored)"; render(); return; }
            NativeBackup.sync();
            s.insert(0, "WROTE ").append("-> synced. Power-cycle to apply everywhere.");
        } catch (Throwable t) { s.append(" WRITE FAILED: ").append(t.getMessage()); }
        String st = s.toString();
        load(); stageRecipe();
        status = st; render();
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
            preview = "preview: live";
        } catch (Throwable t) { preview = "preview error: " + t.getMessage(); }
    }

    // ------------------------------------------------------------ UI
    private String fmt(int i, int v) {
        switch (i) {
            case 0: return Recipes.ALL[recipe].name;
            case 1: return v + "=" + (v >= 0 && v < Recipes.STYLE_NAMES.length ? Recipes.STYLE_NAMES[v] : "?") + (v == 1 ? "" : "?");
            case 5: return v == 0 ? "off" : "ON (PP3)";
            case 6: return v == 1 ? "1=auto" : v == 14 ? "14=colour temp" : String.valueOf(v);
            case 7: return (v * 100) + "K";
            case 8: return v == 0 ? "0" : (v > 0 ? "A+" + v : "B" + v);
            case 9: return v == 0 ? "0" : (v > 0 ? "G+" + v : "M" + v);
            default: return (v > 0 ? "+" : "") + v;
        }
    }

    private void render() {
        boolean dirty = false;
        for (int i = 1; i < N; i++) if (edit[i] != cur[i]) dirty = true;
        String head = "[" + recipe + "/" + (Recipes.ALL.length - 1) + "] " + Recipes.ALL[recipe].name + (dirty ? "  (not stored)" : "  (stored)");
        if (overlay == 0) {
            StringBuilder sb = new StringBuilder("PP Select 0.5  ·  DISP hides text  ·  " + preview + "\n");
            for (int i = 0; i < N; i++) {
                sb.append(i == row ? ">" : " ").append(String.format("%-14s", ROW_NAME[i])).append(fmt(i, edit[i]));
                if (i > 0 && edit[i] != cur[i]) sb.append("  (stored ").append(fmt(i, cur[i])).append(")");
                sb.append('\n');
            }
            sb.append("LEFT/RIGHT recipe · UP/DOWN row · ENTER write · PLAY factory · SHUTTER photo · MENU exit\n").append(status);
            text.setText(sb); text.setVisibility(android.view.View.VISIBLE);
            bar.setVisibility(android.view.View.GONE);
        } else if (overlay == 1) {
            text.setVisibility(android.view.View.GONE);
            bar.setText(head + "   LEFT/RIGHT · ENTER write · DISP"); bar.setVisibility(android.view.View.VISIBLE);
        } else {
            text.setVisibility(android.view.View.GONE); bar.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        switch (e.getScanCode()) {
            case K_LEFT:
                if (row == 0 || overlay != 0) { recipe = (recipe + Recipes.ALL.length - 1) % Recipes.ALL.length; stageRecipe(); }
                else if (row == 6) edit[6] = edit[6] == 14 ? 1 : 14;
                else edit[row] = Math.max(ROW_MIN[row], edit[row] - 1);
                applyPreview(); render(); return true;
            case K_RIGHT:
                if (row == 0 || overlay != 0) { recipe = (recipe + 1) % Recipes.ALL.length; stageRecipe(); }
                else if (row == 6) edit[6] = edit[6] == 14 ? 1 : 14;
                else edit[row] = Math.min(ROW_MAX[row], edit[row] + 1);
                applyPreview(); render(); return true;
            case K_UP: if (overlay == 0) { row = (row + N - 1) % N; render(); } return true;
            case K_DOWN: if (overlay == 0) { row = (row + 1) % N; render(); } return true;
            case K_DISP: case K_FN: overlay = (overlay + 1) % 3; render(); return true;
            case K_ENTER: writeAll(); return true;
            case K_PLAY: recipe = 0; stageRecipe(); applyPreview(); status = "factory staged — ENTER to write"; render(); return true;
            case K_S1: try { camera.autoFocus(null); } catch (Throwable t) {} return true;
            case K_S2: try { camera.takePicture(null, null, null); } catch (Throwable t) {} return true;
            case K_MENU: case K_SK1: case K_DELETE: case K_SK2: return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true; }
        return super.onKeyDown(keyCode, e);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent e) {
        switch (e.getScanCode()) {
            case K_MENU: case K_SK1: case K_DELETE: case K_SK2: finish(); return true;
            case K_S1: try { camera.cancelAutoFocus(); } catch (Throwable t) {} return true;
            case K_S2: try { cameraEx.getClass().getMethod("cancelTakePicture").invoke(cameraEx); } catch (Throwable t) {} return true;
            case K_UP: case K_DOWN: case K_LEFT: case K_RIGHT: case K_ENTER: case K_PLAY: case K_DISP: case K_FN: return true;
        }
        return super.onKeyUp(keyCode, e);
    }
}
