package com.voxivoid.recipelab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/** Two-column recipe browser: brands left, recipes of the highlighted brand right. Canvas-drawn. */
public class PickerView extends View {
    private static final int ACCENT = 0xFFF2B85C, INK = 0xFF1A1208;

    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG), edge = new Paint(Paint.ANTI_ALIAS_FLAG), sel = new Paint(Paint.ANTI_ALIAS_FLAG),
            head = new Paint(Paint.ANTI_ALIAS_FLAG), item = new Paint(Paint.ANTI_ALIAS_FLAG), small = new Paint(Paint.ANTI_ALIAS_FLAG), rule = new Paint();
    private final RectF r = new RectF();
    private final float d;
    private int selected = 0;

    public PickerView(Context c, AttributeSet a) {
        super(c, a);
        d = c.getResources().getDisplayMetrics().density;
        bg.setColor(0xE6121212);
        edge.setColor(0x66F2B85C); edge.setStyle(Paint.Style.STROKE); edge.setStrokeWidth(d);
        sel.setColor(ACCENT);
        head.setColor(0x99FFFFFF); head.setTextSize(9 * d); head.setFakeBoldText(true);
        item.setColor(0xFFFFFFFF); item.setTextSize(13 * d);
        small.setColor(0x99FFFFFF); small.setTextSize(10 * d);
        rule.setColor(0x33FFFFFF);
    }

    public void setSelected(int i) { selected = i; invalidate(); }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), pad = 10 * d, rad = 8 * d;
        r.set(0, 0, w, h); c.drawRoundRect(r, rad, rad, bg); c.drawRoundRect(r, rad, rad, edge);

        Recipes.Recipe cur = Recipes.ALL[selected];
        int g = cur.group;
        float colX = w * 0.32f;                                 // divider
        float top = pad + 12 * d, bottom = h - pad - 14 * d;    // header / footer reserved
        c.drawText("BRAND", pad, pad + 7 * d, head);
        c.drawText(Recipes.GROUPS[g].toUpperCase() + "  ·  " + Recipes.GROUP_COUNT[g], colX + pad, pad + 7 * d, head);
        c.drawLine(colX, pad, colX, h - pad, rule);
        c.drawLine(pad, top + 3 * d, w - pad, top + 3 * d, rule);

        // ---- left: groups
        int ng = Recipes.GROUPS.length;
        float rowH = Math.min(20 * d, (bottom - top - 6 * d) / ng);
        float y = top + 6 * d;
        for (int i = 0; i < ng; i++, y += rowH) {
            boolean on = i == g;
            if (on) { r.set(pad - 4 * d, y, colX - 6 * d, y + rowH); c.drawRoundRect(r, 3 * d, 3 * d, sel); }
            item.setColor(on ? INK : 0xCCFFFFFF); item.setFakeBoldText(on);
            c.drawText(Recipes.GROUPS[i], pad, y + rowH / 2 + item.getTextSize() * 0.36f, item);
            small.setColor(on ? 0xAA1A1208 : 0x66FFFFFF);
            String n = String.valueOf(Recipes.GROUP_COUNT[i]);
            c.drawText(n, colX - 10 * d - small.measureText(n), y + rowH / 2 + small.getTextSize() * 0.36f, small);
        }
        item.setFakeBoldText(false);

        // ---- right: recipes of group, windowed around selection
        int start = Recipes.GROUP_START[g], count = Recipes.GROUP_COUNT[g];
        float rh = 24 * d;
        int visible = Math.max(1, (int) ((bottom - top - 6 * d) / rh));
        int first = 0;
        if (count > visible) { first = Math.max(0, Math.min(selected - start - visible / 2, count - visible)); }
        float x = colX + pad, xr = w - pad;
        y = top + 6 * d;
        for (int k = first; k < Math.min(count, first + visible); k++, y += rh) {
            int idx = start + k; Recipes.Recipe rc = Recipes.ALL[idx];
            boolean on = idx == selected;
            if (on) { r.set(x - 4 * d, y, xr, y + rh); c.drawRoundRect(r, 3 * d, 3 * d, sel); }
            item.setColor(on ? INK : 0xFFFFFFFF); item.setFakeBoldText(on);
            c.drawText(rc.name, x, y + 12 * d, item);
            small.setColor(on ? 0xAA1A1208 : 0x80FFFFFF);
            c.drawText(rc.summary(), x, y + 21 * d, small);
        }
        item.setFakeBoldText(false);
        if (first > 0) c.drawText("...", xr - small.measureText("..."), top + 4 * d, small);
        if (first + visible < count) c.drawText("...", xr - small.measureText("..."), bottom, small);

        // ---- footer
        small.setColor(0x80FFFFFF);
        c.drawText("wheel / UP / DOWN  recipe      LEFT / RIGHT  brand      ENTER  choose      C1  close", pad, h - pad, small);
    }
}
