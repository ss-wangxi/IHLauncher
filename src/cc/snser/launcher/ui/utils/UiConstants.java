package cc.snser.launcher.ui.utils;

import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;

public class UiConstants {

    public static final PaintFlagsDrawFilter ANTI_ALIAS_FILTER = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public static final int DEFAULT_PAINT_FLAGS = Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;

    public static final Paint PAINT = new Paint(DEFAULT_PAINT_FLAGS);

    /**
     * 公用临时Paint，使用后记得调用reset还原
     */
    public static final Paint TEMP_PAINT = new Paint();
}
