package com.voxivoid.ppselect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Key legend drawn with Canvas (camera firmware font has no arrow / symbol glyphs). */
public class HintBar extends View {
    static final int WHEEL = 0, UPDOWN = 1, LEFTRIGHT = 2, DIAL = 3, ENTER = 4, AEL = 5, TRASH = 6, MENU = 7;

    private static final int[] VIEW_ICONS = { WHEEL, UPDOWN, ENTER, TRASH, AEL, MENU };
    private static final String[] VIEW_TEXT = { "recipe", "parameter", "store", "factory", "hide", "exit" };
    private static final int[] EDIT_ICONS = { DIAL, UPDOWN, ENTER, TRASH, AEL, MENU };
    private static final String[] EDIT_TEXT = { "adjust", "parameter", "store", "factory", "hide", "exit" };

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG), text = new Paint(Paint.ANTI_ALIAS_FLAG), keyText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final float d;
    private boolean edit;

    public HintBar(Context c, AttributeSet a) {
        super(c, a);
        d = c.getResources().getDisplayMetrics().density;
        fill.setColor(0xCCFFFFFF); fill.setStyle(Paint.Style.FILL);
        stroke.setColor(0xCCFFFFFF); stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(1.2f * d);
        text.setColor(0x99FFFFFF); text.setTextSize(10 * d);
        keyText.setColor(0xCCFFFFFF); keyText.setTextSize(6.5f * d); keyText.setTextAlign(Paint.Align.CENTER); keyText.setFakeBoldText(true);
    }

    public void setEditMode(boolean e) { if (edit != e) { edit = e; invalidate(); } }

    @Override
    protected void onMeasure(int w, int h) { setMeasuredDimension(MeasureSpec.getSize(w), (int) (16 * d)); }

    @Override
    protected void onDraw(Canvas c) {
        int[] icons = edit ? EDIT_ICONS : VIEW_ICONS;
        String[] labels = edit ? EDIT_TEXT : VIEW_TEXT;
        float x = 0, cy = getHeight() / 2f, s = 6 * d;            // s = icon half-size
        float ty = cy - (text.ascent() + text.descent()) / 2f;
        for (int i = 0; i < icons.length; i++) {
            float w = drawIcon(c, icons[i], x, cy, s);
            x += w + 4 * d;
            c.drawText(labels[i], x, ty, text);
            x += text.measureText(labels[i]) + 14 * d;
        }
    }

    private void tri(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3) {
        path.reset(); path.moveTo(x1, y1); path.lineTo(x2, y2); path.lineTo(x3, y3); path.close(); c.drawPath(path, fill);
    }

    /** draws icon with left edge at x, vertically centred on cy; returns width */
    private float drawIcon(Canvas c, int icon, float x, float cy, float s) {
        float a = s * 0.55f;                                       // arrow size
        switch (icon) {
            case WHEEL: {                                          // ring + left/right arrows outside
                float cx = x + a + s + 1.5f * d;
                c.drawCircle(cx, cy, s * 0.8f, stroke);
                c.drawCircle(cx, cy, s * 0.25f, fill);
                tri(c, x, cy, x + a, cy - a * 0.8f, x + a, cy + a * 0.8f);
                float r = cx + s + 1.5f * d;
                tri(c, r + a, cy, r, cy - a * 0.8f, r, cy + a * 0.8f);
                return 2 * a + 2 * s + 3 * d;
            }
            case DIAL: {                                           // top dial: half ring with ticks + arrows
                float cx = x + a + s + 1.5f * d;
                rect.set(cx - s, cy - s * 0.4f, cx + s, cy + s * 1.6f);
                c.drawArc(rect, 200, 140, false, stroke);
                for (int i = -2; i <= 2; i++) {
                    double ang = Math.toRadians(270 + i * 28);
                    float ox = (float) Math.cos(ang), oy = (float) Math.sin(ang), ccy = cy + s * 0.6f;
                    c.drawLine(cx + ox * s * 0.75f, ccy + oy * s * 0.75f, cx + ox * s, ccy + oy * s, stroke);
                }
                tri(c, x, cy, x + a, cy - a * 0.8f, x + a, cy + a * 0.8f);
                float r = cx + s + 1.5f * d;
                tri(c, r + a, cy, r, cy - a * 0.8f, r, cy + a * 0.8f);
                return 2 * a + 2 * s + 3 * d;
            }
            case UPDOWN: {
                float cx = x + a;
                tri(c, cx, cy - s, cx - a * 0.8f, cy - s + a, cx + a * 0.8f, cy - s + a);
                tri(c, cx, cy + s, cx - a * 0.8f, cy + s - a, cx + a * 0.8f, cy + s - a);
                return 2 * a;
            }
            case LEFTRIGHT: {
                tri(c, x, cy, x + a, cy - a * 0.8f, x + a, cy + a * 0.8f);
                float r = x + a + 3 * d;
                tri(c, r + a, cy, r, cy - a * 0.8f, r, cy + a * 0.8f);
                return 2 * a + 3 * d;
            }
            case ENTER: {                                          // centre button: ring + dot
                float cx = x + s;
                c.drawCircle(cx, cy, s * 0.85f, stroke);
                c.drawCircle(cx, cy, s * 0.4f, fill);
                return 2 * s;
            }
            case AEL: {                                            // rounded key labelled AEL
                float w = 2.6f * s;
                rect.set(x, cy - s * 0.8f, x + w, cy + s * 0.8f);
                c.drawRoundRect(rect, 2 * d, 2 * d, stroke);
                c.drawText("AEL", x + w / 2, cy - (keyText.ascent() + keyText.descent()) / 2f, keyText);
                return w;
            }
            case TRASH: {                                          // bin: lid + body
                float w = 1.6f * s, cx = x + w / 2, top = cy - s * 0.9f, bot = cy + s * 0.9f;
                c.drawLine(x, top + 2 * d, x + w, top + 2 * d, stroke);                       // lid line
                c.drawLine(cx - 2 * d, top, cx + 2 * d, top, stroke);                         // handle
                rect.set(x + 2 * d, top + 2 * d, x + w - 2 * d, bot);
                c.drawRoundRect(rect, 1.5f * d, 1.5f * d, stroke);
                c.drawLine(cx, top + 5 * d, cx, bot - 3 * d, stroke);
                return w;
            }
            case MENU: {                                           // rounded key with three lines
                rect.set(x, cy - s * 0.8f, x + 2 * s, cy + s * 0.8f);
                c.drawRoundRect(rect, 2 * d, 2 * d, stroke);
                for (int i = -1; i <= 1; i++) c.drawLine(x + 3.5f * d, cy + i * 3 * d, x + 2 * s - 3.5f * d, cy + i * 3 * d, stroke);
                return 2 * s;
            }
        }
        return 0;
    }
}
