package cc.snser.launcher;

import android.R.integer;

/**
 * 桌面运行时配置
 * @author songzhaochun
 *
 */
public class RuntimeConfig {
    /**
     * 桌面主界面正处于touch状态，建议一切非UI任务暂停
     */
    public static boolean sLauncherInTouching = false;

    /**
     * 桌面主界面正处于滑屏动画状态，建议一切非UI任务暂停
     */
    public static boolean sLauncherInScrolling = false;

    /**
     * 当icon角标有变化，刷新DrawingCache避免icon显示角标
     */
    public static boolean sDirtyDrawingCacheByTipUpdated = false;

    public static boolean sLowerPerformance = false;

    public static boolean sProgressDialogInLoading = false;

    public static boolean sMenuInAnimation = false;

    //public static int sExtraTopPaddingInNormal;
    //public static int sExtraTopPaddingInDragging;

    public static int sGlobalBottomPadding;
    
    //TODO:move it to dimens
    //编辑模式下，离顶部的距离
    public static final int EDITMODE_TOP_OFFSET = 60;
}