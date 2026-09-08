package com.voxivoid.recipelab;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

/** Key legend under the main panel. */
public class HintBar extends View {
    private static final int[] VIEW_ICONS = { Legend.LEFTRIGHT, Legend.UPDOWN, Legend.C1, Legend.ENTER, Legend.TRASH, Legend.AEL, Legend.MENU };
    private static final String[] VIEW_TEXT = { "recipe", "param", "browse", "store", "factory", "hide", "exit" };
    private static final int[] EDIT_ICONS = { Legend.LEFTRIGHT, Legend.UPDOWN, Legend.C1, Legend.ENTER, Legend.TRASH, Legend.AEL, Legend.MENU };
    private static final String[] EDIT_TEXT = { "adjust", "param", "browse", "store", "factory", "hide", "exit" };

    private final Legend legend;
    private boolean edit;

    public HintBar(Context c, AttributeSet a) {
        super(c, a);
        legend = new Legend(c.getResources().getDisplayMetrics().density);
    }

    public void setEditMode(boolean e) { if (edit != e) { edit = e; invalidate(); } }

    @Override
    protected void onMeasure(int w, int h) { setMeasuredDimension(MeasureSpec.getSize(w), (int) legend.height()); }

    @Override
    protected void onDraw(Canvas c) {
        legend.draw(c, 0, getHeight() / 2f, getWidth(), edit ? EDIT_ICONS : VIEW_ICONS, edit ? EDIT_TEXT : VIEW_TEXT);
    }
}
