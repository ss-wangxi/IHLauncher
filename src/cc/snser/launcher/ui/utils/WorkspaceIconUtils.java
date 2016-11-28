
package cc.snser.launcher.ui.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import cc.snser.launcher.App;
import cc.snser.launcher.IconCache;
import cc.snser.launcher.component.themes.iconbg.model.local.IconBg;
import cc.snser.launcher.model.font.TextTypeface;
import cc.snser.launcher.ui.bitmap.BackgroundMatcher;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.util.BitmapUtils;
import com.shouxinzm.launcher.util.DeviceUtils;

import static cc.snser.launcher.Constant.LOGD_ENABLED;

/**
 * util method for icons, include icon size,create icon.... moved from {@link Utilities}
 *
 * @author yangkai
 * @version 1.0
 */
public class WorkspaceIconUtils {

    static final String TAG = "Launcher.WorkspaceIconUtils";

    private static final int MAX_VALID_ICON_SIZE = 256;

    private static final PaintFlagsDrawFilter FILTER = new PaintFlagsDrawFilter(Paint.DITHER_FLAG, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private static int sCustomIconSize = -1;
    private static float sAppIconSize = -1;
    private static float sDefaultIconSize = -1;
    private static float sIconBgOutterPadding = -1;
    private static float sIconBgInnerPadding = -1;

    private static final Rect OLD_BOUNDS = new Rect();
    private static final Rect ICON_RECT = new Rect();
    private static final Rect BOARD_RECT = new Rect();
    private static final Rect RECT = new Rect();

    private static Bitmap sIconTempBitmap;

    private static int mWorkspaceIconTextSize = -1;
    private static Integer mWorkspaceIconTextColor = null;
    private static boolean mNeedshadowRadius = true;

    private static Typeface mWorkspaceIconTextTypeface = null;

    private static Object mLock = new Object();

    private static Paint sIconTempPaint = new Paint();

    public static boolean isInvalidIconSize(int width, int height) {
        return width > MAX_VALID_ICON_SIZE || height > MAX_VALID_ICON_SIZE || width != height;
    }

    public static int getIconHeightWidthPadding(Context context) {
        float ret = WorkspaceIconUtils.getIconSizeWithPadding(context) + context.getResources().getDimensionPixelSize(R.dimen.workspace_icon_drawable_padding);
        sIconTempPaint.setTextSize(WorkspaceIconUtils.getWorkspaceIconTextSizeInPx(context));
        sIconTempPaint.setColor(WorkspaceIconUtils.getWorkspaceIconTextColor(context));
        Typeface t = WorkspaceIconUtils.getWorkspaceIconTypeface(context);
        sIconTempPaint.setTypeface(t);
        FontMetrics fontMetrics = sIconTempPaint.getFontMetrics();
        ret += fontMetrics.bottom - fontMetrics.top;
        return (int) ret;
    }

    public static int getIconSizeWithPadding(Context context) {
        synchronized (mLock) { // we share the statics :-(
            if (sAppIconSize < 0) {
                initStatics(context);
            }

            int ret;

            String iconSizeType = getIconSizeType(context);
            if (ICON_SIZE_TYPE_CUSTOM.equals(iconSizeType)) {
                ret = getCustomIconSizeWithPadding(context);
            } else if (ICON_SIZE_TYPE_BIG.equals(iconSizeType) && !IconBg.isUsingNoBg(context)) {
                ret = getBigIconSizeWithPadding(context);
            } else {
                ret = getSmallIconSizeWithPadding(context);
            }

            if (ret % 2 == 1) {
                ret++;
            }

            return ret;
        }
    }

    private static int getSmallIconSizeWithPadding(Context context) {
        synchronized (mLock) {
            if (sAppIconSize < 0) {
                initStatics(context);
            }
        }
        return (int) sDefaultIconSize;
    }

    /**
     * get big icon size (you must insure icon has bg setted)
     *
     * @param context
     * @return
     */
    private static int getBigIconSizeWithPadding(Context context) {
        synchronized (mLock) {
            if (sAppIconSize < 0) {
                initStatics(context);
            }
        }

        return (int) (sAppIconSize + 2 * sIconBgOutterPadding);
    }

    private static int getCustomIconSizeWithPadding(Context context) {
        synchronized (mLock) {
            if (sAppIconSize < 0) {
                initStatics(context);
            }
        }

        if (LOGD_ENABLED) {
            XLog.d(TAG, "getCustomIconSizeWithPadding sCustomIconSize " + sCustomIconSize + " sDefaultIconSize " + sAppIconSize);
        }

        return getCustomIconSize(context);
    }

    private static int getIconSizeWithoutPadding(Context context) {
        synchronized (mLock) { // we share the statics :-(
            if (sAppIconSize < 0) {
                initStatics(context);
            }

            int ret;

            String iconSizeType = getIconSizeType(context);
            if (ICON_SIZE_TYPE_CUSTOM.equals(iconSizeType)) {
                ret = getCustomIconSizeWithoutPadding(context);
            } else if (ICON_SIZE_TYPE_BIG.equals(iconSizeType) && !IconBg.isUsingNoBg(context)) {
                ret = getBigIconSizeWithoutPadding(context);
            } else {
                ret = getSmallIconSizeWithoutPadding(context);
            }

            if (ret % 2 == 1) {
                ret--;
            }

            return ret;
        }
    }

    /**
     * get small icon drawable size (you must insure icon has bg setted)
     *
     * @param context
     * @return
     */
    private static int getSmallIconSizeWithoutPadding(Context context) {
        synchronized (mLock) {
            if (sAppIconSize < 0) {
                initStatics(context);
            }
        }

        return (int) (sDefaultIconSize - 2 * sIconBgInnerPadding);
    }

    /**
     * get big icon drawable size (you must insure icon has bg setted)
     *
     * @param context
     * @return
     */
    public static int getBigIconSizeWithoutPadding(Context context) {
        synchronized (mLock) {
            if (sAppIconSize < 0) {
                initStatics(context);
            }
        }

        return (int) sAppIconSize;
    }

    private static int getCustomIconSizeWithoutPadding(Context context) {
        synchronized (mLock) {
            if (sAppIconSize < 0) {
                initStatics(context);
            }
        }

        int ret = getCustomIconSizeWithPadding(context);

        if (ret <= sAppIconSize) {// use small icon size logic
            float innerPadding = ret * sIconBgInnerPadding / sAppIconSize;
            return (int) (ret - innerPadding * 2);
        } else { // use large icon size logic
            int bigIconSizeWithPadding = getBigIconSizeWithPadding(context);
            // if (ret >= bigIconSizeWithPadding) {
                // large enough
            //     return getBigIconSizeWithoutPadding(context);
            // } else {
                // not that large
                return (int) (ret * sAppIconSize / bigIconSizeWithPadding);
            // }
        }
    }

    public static float getIconRadio(Context context) {
        if (DeviceUtils.isOppo() && (IconCache.getInstance(context).getBackDrawable() == null && IconCache.getInstance(context).getFrontDrawable() == null)) {
            return 144.f / 168;
        }

        return 1.0f;
    }

    /**
     * Returns a bitmap suitable for the all apps view. The bitmap will be a power of two sized ARGB_8888 bitmap that
     * can be used as a gl texture.
     */
    public static Bitmap createIconBitmap(Drawable icon, Context context, boolean addBoard, boolean isExternalIcon) {
        Drawable backDrawable = null;
        Drawable iconMaskDrawable = null;
        Drawable frontDrawable = null;

        if (addBoard) {
            IconCache iconCache = IconCache.getInstance(context);
            backDrawable = iconCache.getBackDrawable();
            iconMaskDrawable = iconCache.getIconMaskDrawable();
            frontDrawable = iconCache.getFrontDrawable();
        }

        return createIconBitmap(icon, context, backDrawable, iconMaskDrawable, frontDrawable, isExternalIcon);
    }

    /**
     * Returns a bitmap suitable for the all apps view. The bitmap will be a power of two sized ARGB_8888 bitmap that
     * can be used as a gl texture.
     */

    public static Bitmap createIconBitmap(Drawable icon, Context context,
            Drawable backDrawable, Drawable iconMaskDrawable, Drawable frontDrawable,
            boolean isExternalIcon) {
        return createIconBitmap(icon, context, backDrawable, iconMaskDrawable, frontDrawable,
                isExternalIcon, 1.0f);
    }

    public static Bitmap createIconBitmap(Drawable icon, Context context,
            Drawable backDrawable, Drawable iconMaskDrawable, Drawable frontDrawable,
            boolean isExternalIcon, float iconRatio) {
        if (icon == null) {
            return null;
        }

        synchronized (mLock) { // we share the statics :-(
            final boolean isIconInside = backDrawable != null || frontDrawable != null;

            final int iconSizeWithPadding = getIconSizeWithPadding(context, -1);
            final int iconSizeWithoutPadding = getIconSizeWithoutPadding(context, -1);

            final int textureWidth = iconSizeWithPadding;
            final int textureHeight = iconSizeWithPadding;

            int iconWidth, iconHeight;
            if (isIconInside) {
                iconWidth = iconHeight = iconSizeWithoutPadding;
            } else {
                if (!isExternalIcon) {
                    iconWidth = iconHeight = (int) (iconSizeWithPadding * getIconRadio(context));
                } else {
                    iconWidth = iconHeight = iconSizeWithPadding;
                }
            }

            boolean isNeedCheckRoundRect = backDrawable != null;
            Bitmap bitmapToCheck = null;

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(iconWidth);
                painter.setIntrinsicHeight(iconHeight);
            } else if (icon instanceof BitmapDrawable) {
                // Ensure the bitmap has a density.
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                Bitmap bitmap = bitmapDrawable.getBitmap();

                if (bitmap != null) {
                    if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                        bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                    }
                    bitmapToCheck = bitmap;
                }
            }

            final int sourceWidth = icon.getIntrinsicWidth();
            final int sourceHeight = icon.getIntrinsicHeight();

            if (sourceWidth > 0 && sourceHeight > 0) {
                // There are intrinsic sizes.
                if (iconWidth < sourceWidth || iconHeight < sourceHeight) {
                    // It's too big, scale it down.
                    final float ratio = (float) sourceWidth / sourceHeight;
                    if (sourceWidth > sourceHeight) {
                        iconHeight = (int) (iconWidth / ratio);
                    } else if (sourceHeight > sourceWidth) {
                        iconWidth = (int) (iconHeight * ratio);
                    }
                } else if (sourceWidth < iconWidth && sourceHeight < iconHeight) {
                    // It's small, use the size they gave us.
                    float min = Math.min(iconWidth * 1.0f / sourceWidth, iconHeight * 1.0f / sourceHeight);
                    iconWidth = (int) (sourceWidth * min);
                    iconHeight = (int) (sourceHeight * min);
                }
            }

            iconWidth *= iconRatio;
            iconHeight *= iconRatio;

            if (iconWidth % 2 == 1) {
                iconWidth--;
            }
            if (iconHeight % 2 == 1) {
                iconHeight--;
            }

            final int boardLeft = (textureWidth - iconSizeWithPadding) / 2;
            final int boardTop = (textureHeight - iconSizeWithPadding) / 2;
            final int boardRight = boardLeft + iconSizeWithPadding;
            final int boardBottom = boardTop + iconSizeWithPadding;
            BOARD_RECT.set(boardLeft, boardTop, boardRight, boardBottom);

            final int iconLeft = (textureWidth - iconWidth) / 2;
            final int iconTop = (textureWidth - iconHeight) / 2;
            ICON_RECT.set(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight);

            boolean isBitmapChecked = false;
            boolean isRoundRect = false;
            if (isNeedCheckRoundRect && bitmapToCheck != null) {
                if (bitmapToCheck.getWidth() <= iconWidth && bitmapToCheck.getHeight() <= iconHeight) {
                    isRoundRect = BackgroundMatcher.isRoundRect(context, bitmapToCheck);
                    isBitmapChecked = true;
                }
            }

            final Bitmap bitmap = createCanvasBitmap(textureWidth, textureHeight);
            final Canvas canvas = new Canvas(bitmap);
            canvas.setDrawFilter(FILTER);

            if (iconMaskDrawable == null && backDrawable != null && (!isNeedCheckRoundRect || isBitmapChecked)) {
                backDrawable.setBounds(BOARD_RECT);
                backDrawable.draw(canvas);
            }

            if (!isNeedCheckRoundRect || !isBitmapChecked || !isRoundRect) {
                OLD_BOUNDS.set(icon.getBounds());
                icon.setBounds(ICON_RECT);
                icon.draw(canvas);
                icon.setBounds(OLD_BOUNDS);
                if (isNeedCheckRoundRect && !isBitmapChecked) {
                    bitmapToCheck = bitmap;
                    isRoundRect = BackgroundMatcher.isRoundRect(context, bitmapToCheck);
                }
            } else {
                final Paint paint = UiConstants.TEMP_PAINT;
                paint.setAntiAlias(true);
                paint.setFilterBitmap(true);
                paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
                canvas.drawBitmap(bitmapToCheck, BackgroundMatcher.sOpaqueRect, BOARD_RECT, paint);
                paint.reset();
            }

            if (iconMaskDrawable != null) {
                // 使用icon大小来画mask裁剪icon
                RECT.set(boardLeft, boardTop, boardRight, boardBottom);
                final Paint paint = UiConstants.TEMP_PAINT;
                paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
                canvas.drawBitmap(((BitmapDrawable) iconMaskDrawable).getBitmap(), null, RECT, paint);
                paint.reset();
            }

            Bitmap out = bitmap;
            if (backDrawable != null && (iconMaskDrawable != null || isNeedCheckRoundRect && !isBitmapChecked && bitmapToCheck != null)) {
                out = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
                canvas.setBitmap(out);

                backDrawable.setBounds(BOARD_RECT);
                backDrawable.draw(canvas);

                if (!isRoundRect) {
                    canvas.drawBitmap(bitmap, 0, 0, null);
                } else {
                    final Paint paint = UiConstants.TEMP_PAINT;
                    paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
                    paint.setAntiAlias(true);
                    paint.setFilterBitmap(true);
                    canvas.drawBitmap(bitmapToCheck, BackgroundMatcher.sOpaqueRect, BOARD_RECT, paint);
                    paint.reset();
                }

                sIconTempBitmap = bitmap;
            }

            if (frontDrawable != null) {
                frontDrawable.setBounds(BOARD_RECT);
                frontDrawable.draw(canvas);
            }

            return out;
        }
    }

    static int getIconSizeWithPadding(final Context context, int iconSizeType) {
        final int iconWidthWithPadding;
        switch (iconSizeType) {
            case -1:
                iconWidthWithPadding = getIconSizeWithPadding(context);
                break;
            case 0:
                iconWidthWithPadding = getSmallIconSizeWithPadding(context);
                break;
            case 1:
                iconWidthWithPadding = getBigIconSizeWithPadding(context);
                break;
            default:
                iconWidthWithPadding = getIconSizeWithPadding(context);
                break;
        }

        return iconWidthWithPadding;
    }

    private static int getIconSizeWithoutPadding(final Context context, int iconSizeType) {
        final int iconWidthWithoutPadding;
        switch (iconSizeType) {
            case -1:
                iconWidthWithoutPadding = getIconSizeWithoutPadding(context);
                break;
            case 0:
                iconWidthWithoutPadding = getSmallIconSizeWithoutPadding(context);
                break;
            case 1:
                iconWidthWithoutPadding = getBigIconSizeWithoutPadding(context);
                break;
            default:
                iconWidthWithoutPadding = getIconSizeWithoutPadding(context);
                break;
        }

        return iconWidthWithoutPadding;
    }

    private static Bitmap createCanvasBitmap(final int textureWidth, final int textureHeight) {
        final Bitmap bitmap;
        if (sIconTempBitmap != null) {
            if (sIconTempBitmap.getWidth() == textureWidth && sIconTempBitmap.getHeight() == textureHeight) {
                bitmap = sIconTempBitmap;
                sIconTempBitmap = null;
                bitmap.eraseColor(0x00000000);
            } else {
                BitmapUtils.recycleBitmap(sIconTempBitmap);
                sIconTempBitmap = null;
                bitmap = BitmapUtils.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
            }
        } else {
            bitmap = BitmapUtils.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
        }
        return bitmap;
    }

    private static void initStatics(Context context) {
        final Resources resources = context.getResources();
        sAppIconSize = resources.getDimension(R.dimen.app_icon_size);
        sDefaultIconSize = resources.getDimension(R.dimen.default_icon_size);

        sIconBgOutterPadding = resources.getDimension(R.dimen.app_icon_bg_outter_padding);
        sIconBgInnerPadding = resources.getDimension(R.dimen.app_icon_bg_inner_padding);

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.2f);
    }
    
    public static void refreshIconSize(){
    	final Context context = App.getApp().getApplicationContext();
    	final Resources resources = context.getResources();
    	sAppIconSize = resources.getDimension(R.dimen.app_icon_size);
    	sDefaultIconSize = resources.getDimension(R.dimen.default_icon_size);
    }

    /************************************
     * work space icon text settings
     ******************************/

    public static int getWorkspaceIconTextSizeInPx(Context context) {
        if (mWorkspaceIconTextSize < 0) {
            mWorkspaceIconTextSize = PrefUtils.getIntPref(context, PrefConstants.KEY_ICON_TEXT_SIZE, -1);
            if (mWorkspaceIconTextSize < 0) {
                mWorkspaceIconTextSize = getDefaultWorkspaceIconTextSizeInPx(context);
            }
        }
        return mWorkspaceIconTextSize;
    }

    /**
     * @param context
     * @param size -1 set to default value
     */
    public static void setAndSaveWorkspaceIconTextSizeInPx(Context context, int size) {
        if (size >= 0) {
            mWorkspaceIconTextSize = size;
            PrefUtils.setIntPref(context, PrefConstants.KEY_ICON_TEXT_SIZE, size);
        } else {
            mWorkspaceIconTextSize = getDefaultWorkspaceIconTextSizeInPx(context);
            PrefUtils.removePref(context, PrefConstants.KEY_ICON_TEXT_SIZE);
        }
    }

    public static void resetWorkspaceIconTextSize() {
        mWorkspaceIconTextSize = -1;
    }

    public static int getDefaultWorkspaceIconTextSizeInPx(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.bubbtextview_textsize);
    }

    public static boolean usingDefaultWorkspaceIconTextSize(Context context) {
        return !PrefUtils.containsPref(context, PrefConstants.KEY_ICON_TEXT_SIZE);
    }

    public static int getWorkspaceIconTextColor(Context context) {
        if (mWorkspaceIconTextColor == null) {
            if (PrefUtils.containsPref(context, PrefConstants.KEY_ICON_TEXT_COLOR)) {
                mWorkspaceIconTextColor = PrefUtils.getIntPref(context, PrefConstants.KEY_ICON_TEXT_COLOR, getDefaultWorkspaceIconTextColor());
                resetNeedShadowRadius();
            } else {
                mWorkspaceIconTextColor = getDefaultWorkspaceIconTextColor();
                resetNeedShadowRadius();
            }
        }
        return mWorkspaceIconTextColor;
    }

    private static void resetNeedShadowRadius() {
        mNeedshadowRadius = mWorkspaceIconTextColor == getDefaultWorkspaceIconTextColor();
    }

    public static boolean needshadowRadius() {
        return mNeedshadowRadius;
    }

    public static int getDefaultWorkspaceIconTextColor() {
        return Color.WHITE;
    }

    /**
     * @param context
     * @param size -1 set to default value
     */
    public static void setAndSaveWorkspaceIconTextColor(Context context, int color) {
        if (color != -1) {
            mWorkspaceIconTextColor = color;
            resetNeedShadowRadius();
            PrefUtils.setIntPref(context, PrefConstants.KEY_ICON_TEXT_COLOR, color);
        } else {
            mWorkspaceIconTextColor = getDefaultWorkspaceIconTextColor();
            resetNeedShadowRadius();
            PrefUtils.removePref(context, PrefConstants.KEY_ICON_TEXT_COLOR);
        }
    }

    public static void resetWorkspaceIconTextColor() {
        mWorkspaceIconTextColor = null;
    }

    public static void setAndSaveWorkspaceIconTextTypeface(Context context, TextTypeface textTypeface) {
        try {
            mWorkspaceIconTextTypeface = textTypeface.getTypeface();
        } catch (Exception e) {
        }
    }

    public static Typeface getWorkspaceIconTypeface(Context context) {
        if (mWorkspaceIconTextTypeface == null) {
            try {
                mWorkspaceIconTextTypeface = TextTypeface.getCurrentTypeface(context).getTypeface();
            } catch (Exception e) {
            }
        }
        return mWorkspaceIconTextTypeface;
    }

    /************************************
     * work space icon text settings end
     *************************************/

    /************************************
     * work space icon size type settings
     *************************************/

    public static final String ICON_SIZE_TYPE_SMALL = "0";

    public static final String ICON_SIZE_TYPE_BIG = "1";

    public static final String ICON_SIZE_TYPE_CUSTOM = "2";

    private static String sIconSizeType;

    public static synchronized String getIconSizeType(Context context) {
        if (sIconSizeType == null) {
            sIconSizeType = PrefUtils.getStringPref(context, PrefConstants.KEY_ICON_SIZE_TYPE, ICON_SIZE_TYPE_BIG);
        }

        return sIconSizeType;
    }

    public static synchronized void setIconSizeType(Context context, String iconSizeType) {
        PrefUtils.setStringPref(context, PrefConstants.KEY_ICON_SIZE_TYPE, iconSizeType);
        sIconSizeType = iconSizeType;

        if (ICON_SIZE_TYPE_CUSTOM.equals(iconSizeType)) {
            PrefUtils.removePref(context, PrefConstants.KEY_CUSTOM_ICON_SIZE);
        }
    }

    public static synchronized void setCustomIconSize(Context context, int size) {
        if (LOGD_ENABLED) {
            XLog.d(TAG, "setCustomIconSize sCustomIconSize " + size);
        }

        if (!ICON_SIZE_TYPE_CUSTOM.equals(sIconSizeType)) {
            setIconSizeType(context, ICON_SIZE_TYPE_CUSTOM);
        }

        PrefUtils.setIntPref(context, PrefConstants.KEY_CUSTOM_ICON_SIZE, size);
        sCustomIconSize = size;
    }

    public static synchronized int getCustomIconSize(Context context) {
        if (sCustomIconSize < 0) {
            sCustomIconSize = PrefUtils.getIntPref(context, PrefConstants.KEY_CUSTOM_ICON_SIZE, (int)(sDefaultIconSize + 0.5f));
        }

        return sCustomIconSize;
    }

    public static synchronized void resetIconSizeTypeAndCustomIconSize() {
        sIconSizeType = null;
        sCustomIconSize = -1;
    }
    
    public static synchronized void resetIconSize(){
    	sAppIconSize = -1;
    }

    /************************************
     * work space icon size type settings end
     *************************************/

    /***********************************************
     * work space icon text line number settings
     *************************************/

    private static int sIconTextMaxLine = -1;

    public static int getIconTextMaxLine(Context context) {
        if (sIconTextMaxLine < 0) {
            sIconTextMaxLine = PrefUtils.getIntPref(context, PrefConstants.KEY_ICON_TEXT_MAX_LINE, 1);
        }

        return sIconTextMaxLine;
    }

    /*************************************************
     * work space icon text line number settings end
     *************************************/
}