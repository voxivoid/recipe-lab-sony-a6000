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
            head = new Paint(Paint.ANTI_ALIAS_FLAG), item = new Paint(Paint.ANTI_ALIAS_FLAG), small = new Paint(Paint.ANTI_ALIAS_FLAG), rule = new Paint(),
            track = new Paint(Paint.ANTI_ALIAS_FLAG), thumb = new Paint(Paint.ANTI_ALIAS_FLAG), tagBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float d;
    private final Legend legend;
    private static final int[] BRAND_ICONS = { Legend.UPDOWN, Legend.LEFTRIGHT, Legend.ENTER, Legend.C1 };
    private static final String[] BRAND_TEXT = { "brand", "column", "recipes", "close" };
    private static final int[] RECIPE_ICONS = { Legend.UPDOWN, Legend.LEFTRIGHT, Legend.ENTER, Legend.C1 };
    private static final String[] RECIPE_TEXT = { "recipe", "column", "pick", "close" };
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int selected = 0, column = 1;                         // column: 0 brands, 1 recipes

    public PickerView(Context c, AttributeSet a) {
        super(c, a);
        d = c.getResources().getDisplayMetrics().density;
        legend = new Legend(d);
        bg.setColor(0xF0101010);
        edge.setColor(0x66F2B85C); edge.setStyle(Paint.Style.STROKE); edge.setStrokeWidth(d);
        sel.setColor(ACCENT);
        outline.setColor(ACCENT); outline.setStyle(Paint.Style.STROKE); outline.setStrokeWidth(1.5f * d);
        head.setColor(0x99FFFFFF); head.setTextSize(9 * d); head.setFakeBoldText(true);
        item.setColor(0xFFFFFFFF); item.setTextSize(13 * d);
        small.setColor(0x99FFFFFF); small.setTextSize(10 * d);
        rule.setColor(0x33FFFFFF);
        track.setColor(0x26FFFFFF); thumb.setColor(0xCCF2B85C);
    }

    public void setSelected(int i, int col) { selected = i; column = col; invalidate(); }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), pad = 12 * d;
        c.drawRect(0, 0, w, h, bg);

        Recipes.Recipe cur = Recipes.ALL[selected];
        int g = cur.group;
        float colX = w * 0.30f;                                 // divider
        float top = pad + 12 * d, bottom = h - pad - 20 * d;    // header / footer reserved
        float sbW = 4 * d;                                      // scrollbar width
        head.setColor(column == 0 ? ACCENT : 0x99FFFFFF);
        c.drawText("BRAND", pad, pad + 7 * d, head);
        head.setColor(column == 1 ? ACCENT : 0x99FFFFFF);
        c.drawText(Recipes.GROUPS[g].toUpperCase() + "  ·  " + Recipes.GROUP_COUNT[g], colX + pad, pad + 7 * d, head);
        head.setColor(0x99FFFFFF);
        c.drawLine(colX, pad, colX, h - pad, rule);
        c.drawLine(pad, top + 3 * d, w - pad, top + 3 * d, rule);

        // ---- left: groups
        int ng = Recipes.GROUPS.length;
        float listTop = top + 6 * d, listH = bottom - listTop;
        float rowH = 24 * d;
        int gVisible = Math.max(1, (int) (listH / rowH));
        int gFirst = ng > gVisible ? Math.max(0, Math.min(g - gVisible / 2, ng - gVisible)) : 0;
        float gRight = colX - 8 * d - (ng > gVisible ? sbW + 4 * d : 0);
        float y = listTop;
        for (int i = gFirst; i < Math.min(ng, gFirst + gVisible); i++, y += rowH) {
            boolean on = i == g, active = on && column == 0;
            if (on) { r.set(pad - 4 * d, y, gRight, y + rowH); c.drawRoundRect(r, 3 * d, 3 * d, active ? sel : outline); }
            item.setColor(active ? INK : on ? ACCENT : 0xCCFFFFFF); item.setFakeBoldText(on);
            c.drawText(Recipes.GROUPS[i], pad, y + rowH / 2 + item.getTextSize() * 0.36f, item);
            small.setColor(active ? 0xAA1A1208 : 0x66FFFFFF);
            String n = String.valueOf(Recipes.GROUP_COUNT[i]);
            c.drawText(n, gRight - 6 * d - small.measureText(n), y + rowH / 2 + small.getTextSize() * 0.36f, small);
        }
        item.setFakeBoldText(false);
        if (ng > gVisible) scrollbar(c, colX - 6 * d - sbW, listTop, listH, sbW, gFirst, gVisible, ng);

        // ---- right: recipes of group, windowed around selection
        int start = Recipes.GROUP_START[g], count = Recipes.GROUP_COUNT[g];
        float rh = 26 * d;
        int visible = Math.max(1, (int) (listH / rh));
        int first = 0;
        boolean scroll = count > visible;
        if (scroll) { first = Math.max(0, Math.min(selected - start - visible / 2, count - visible)); }
        float x = colX + pad, xr = w - pad - (scroll ? sbW + 6 * d : 0);
        y = listTop;
        for (int k = first; k < Math.min(count, first + visible); k++, y += rh) {
            int idx = start + k; Recipes.Recipe rc = Recipes.ALL[idx];
            boolean on = idx == selected, active = on && column == 1;
            if (on) { r.set(x - 4 * d, y, xr, y + rh); c.drawRoundRect(r, 3 * d, 3 * d, active ? sel : outline); }
            item.setColor(active ? INK : on ? ACCENT : 0xFFFFFFFF); item.setFakeBoldText(on);
            c.drawText(rc.name, x, y + 13 * d, item);
            small.setColor(active ? 0xAA1A1208 : 0x80FFFFFF);
            c.drawText(rc.summary(), x, y + 22 * d, small);
            String tag = rc.isEffect() ? "PE" : "CS";
            float tw = head.measureText(tag) + 8 * d, tx = xr - 4 * d - tw;
            r.set(tx, y + 5 * d, tx + tw, y + 17 * d);
            tagBg.setColor(active ? 0x331A1208 : (rc.isEffect() ? 0x55B8741A : 0x33FFFFFF));
            c.drawRoundRect(r, 2 * d, 2 * d, tagBg);
            head.setColor(active ? INK : 0xCCFFFFFF);
            c.drawText(tag, tx + 4 * d, y + 14 * d, head);
            head.setColor(0x99FFFFFF);
        }
        item.setFakeBoldText(false);
        if (scroll) scrollbar(c, w - pad - sbW, listTop, listH, sbW, first, visible, count);

        // ---- footer: icon legend
        c.drawLine(pad, h - pad - 16 * d, w - pad, h - pad - 16 * d, rule);
        legend.draw(c, pad, h - pad - 6 * d, w - 2 * pad, column == 0 ? BRAND_ICONS : RECIPE_ICONS, column == 0 ? BRAND_TEXT : RECIPE_TEXT);
    }

    /** vertical scrollbar: track + thumb proportional to the visible window */
    private void scrollbar(Canvas c, float x, float top, float height, float width, int first, int visible, int total) {
        r.set(x, top, x + width, top + height); c.drawRoundRect(r, width / 2, width / 2, track);
        float thumbH = Math.max(12 * d, height * visible / total);
        float thumbY = top + (height - thumbH) * first / Math.max(1, total - visible);
        r.set(x, thumbY, x + width, thumbY + thumbH); c.drawRoundRect(r, width / 2, width / 2, thumb);
    }
}
