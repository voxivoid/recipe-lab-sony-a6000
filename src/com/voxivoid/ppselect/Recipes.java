package com.voxivoid.ppselect;

/**
 * Film-look approximations (only looks that map credibly onto this body's controls) (names follow the popular Sony recipe list; values are my own, built ONLY from
 * settings the A6000 can store persistently). No gamma / per-hue colour depth exists on this body.
 *
 * style   : Creative Style (runtime name is exact; stored enum: 1=standard confirmed, others provisional)
 * sat/con/sharp : Creative Style adjustments (menu range -3..+3; beyond = experimental)
 * matrix  : 1 = PP3 alternate colour matrix (~+45% chroma, blue/green cross-talk)
 * wbMode  : 0 = leave WB as is · 1 = auto · 14 = colour temperature (kelvin)
 * ab / gm : WB fine tune  amber(+)/blue(-)  green(+)/magenta(-)  (-7..+7)
 */
public class Recipes {
    public static class Recipe {
        public final String name; public final int style, sat, con, sharp, matrix, wbMode, kelvin, ab, gm;
        Recipe(String name, int style, int sat, int con, int sharp, int matrix, int wbMode, int kelvin, int ab, int gm) {
            this.name = name; this.style = style; this.sat = sat; this.con = con; this.sharp = sharp; this.matrix = matrix;
            this.wbMode = wbMode; this.kelvin = kelvin; this.ab = ab; this.gm = gm;
        }
    }

    public static final int STD = 1, VIVID = 2, NEUTRAL = 3, PORTRAIT = 4, LANDSCAPE = 5, MONO = 6, CLEAR = 7, DEEP = 8, LIGHT = 9, SUNSET = 10, NIGHT = 11, AUTUMN = 12, SEPIA = 13;
    /** index = stored enum guess; value = runtime color-mode name (API) */
    public static final String[] STYLE_NAMES = { "?", "standard", "vivid", "neutral", "portrait", "landscape", "mono", "clear", "deep", "light", "sunset", "night", "red-leaves", "sepia" };

    private static final int AUTO = 1, K = 14;

    public static final Recipe[] ALL = {
        new Recipe("FACTORY (standard 0/0/0, WB auto)",   STD,      0,  0,  0, 0, AUTO, 0,     0,  0),
        // ---- custom / colour negatives ----
        new Recipe("Fuji 400H",                           LIGHT,   -2, -1,  0, 0, AUTO, 0,    -1,  1),
        new Recipe("Ektar 100",                           VIVID,    3,  2,  1, 1, AUTO, 0,     1,  0),
        new Recipe("Kodak Portra 800",                    STD,     -1,  1,  0, 0, AUTO, 0,     2,  0),
        new Recipe("Kodak Gold",                          STD,      2,  1,  0, 0, AUTO, 0,     3,  1),
        new Recipe("Blue Velvet (Cinestill 50D)",         STD,     -1,  1,  0, 0, K,    5600, -2,  0),
        new Recipe("Cinestill 800T",                      NEUTRAL, -2,  0,  0, 0, K,    3200,  0, -1),
        new Recipe("Fuji Eterna",                         NEUTRAL, -6, -2, -1, 0, AUTO, 0,     0,  0),
        new Recipe("Classic Chrome",                      NEUTRAL, -5,  2,  0, 0, AUTO, 0,    -1,  0),
        new Recipe("Kodachrome 64 V1",                    DEEP,     1,  2,  1, 0, AUTO, 0,     1, -1),
        new Recipe("Kodak Ultra Max 400",                 STD,      3,  1,  0, 0, AUTO, 0,     2,  0),
        new Recipe("Kodak Portra 400",                    PORTRAIT,-1,  0,  0, 0, AUTO, 0,     2,  1),
        new Recipe("Astia",                               PORTRAIT, 0, -1,  0, 0, AUTO, 0,     1,  0),
        new Recipe("Classic Negative",                    STD,     -3,  3,  1, 0, AUTO, 0,     0,  1),
        new Recipe("Fuji Fortia 50",                      VIVID,    6,  2,  0, 1, AUTO, 0,     0, -1),
        new Recipe("Kodak Portra 160",                    PORTRAIT,-2, -1,  0, 0, AUTO, 0,     1,  0),
        new Recipe("Ektachrome",                          CLEAR,    2,  1,  0, 0, AUTO, 0,    -1,  0),
        new Recipe("Velvia Pro",                          VIVID,    5,  1,  0, 1, AUTO, 0,     0,  0),
        new Recipe("Provia RX",                           STD,      1,  0,  0, 0, AUTO, 0,     0,  0),
        new Recipe("Classic Cinema",                      NEUTRAL, -4, -2, -1, 0, K,    5000,  0,  0),
        new Recipe("Kodak Color Plus 200",                STD,      1,  1,  0, 0, AUTO, 0,     2,  1),
        new Recipe("Nostalgic Neg",                       PORTRAIT,-2, -1,  0, 0, AUTO, 0,     2,  0),
        new Recipe("Asteroid City (Vision 200T)",         LIGHT,    2, -1,  0, 0, K,    4300,  2,  2),
        // ---- black & white ----
        new Recipe("Delta 3200",                          MONO,     0,  3, -2, 0, AUTO, 0,     0,  0),
        new Recipe("T-Max",                               MONO,     0,  2,  3, 0, AUTO, 0,     0,  0),
        new Recipe("Kodak Tri-X 400",                     MONO,     0,  2,  2, 0, AUTO, 0,     0,  0),
        new Recipe("Acros X",                             MONO,     0,  1,  1, 0, AUTO, 0,     0,  0),
        new Recipe("Acros XY (yellow filter look)",       MONO,     0,  1,  1, 0, K,    4000,  0,  0),
        new Recipe("Acros XR (red filter look)",          MONO,     0,  2,  1, 0, K,    2500,  0,  0),
        new Recipe("Acros XG (green filter look)",        MONO,     0,  1,  1, 0, K,    5600,  0,  4),
        new Recipe("Ilford HP5",                          MONO,     0,  1,  0, 0, AUTO, 0,     0,  0),
    };
}
