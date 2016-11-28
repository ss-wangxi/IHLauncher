package com.btime.launcher.adapter;

import com.btime.launcher.R;

import android.content.Context;
import android.content.res.Resources;

public class ChannelLayoutAdapter {

    /**
     * 获取Workspace layout的资源id
     * @param context
     * @return
     */
    public static int getWorkspaceContentViewResid(Context context) {
        final int channel = ChannelRecognizeUtils.getCurrentChannel(context);
        switch (channel) {
            case ChannelRecognizeUtils.CHANNEL_C601:
                return R.xml.workspace_c601_default;
            default:
                return R.xml.workspace_phone_default;
        }
    }
    
    /**
     * Workspace是否支持滑屏动画
     * @return
     */
    public static boolean isSupportScrollAnim(Context context) {
        final int channel = ChannelRecognizeUtils.getCurrentChannel(context);
        switch (channel) {
            case ChannelRecognizeUtils.CHANNEL_PHONE_DEFAULT:
                return false;
            default:
                return false;
        }
    }
    
    /**
     * 是否支持双屏
     * @return
     */
    public static boolean isSupportMultiScreen(Context context) {
        final int channel = ChannelRecognizeUtils.getCurrentChannel(context);
        switch (channel) {
            case ChannelRecognizeUtils.CHANNEL_PHONE_DEFAULT:
            default:
                return false;
        }
    }
    
    /**
     * 是否支持FMManager
     * @return
     */
    public static boolean isSupportFMManager(Context context) {
        final int channel = ChannelRecognizeUtils.getCurrentChannel(context);
        switch (channel) {
            case ChannelRecognizeUtils.CHANNEL_PHONE_DEFAULT:
            default:
                return false;
        }
    }
    
    /**
     * 是否支持FMManager
     * @return
     */
    public static int getIndicatorMarginLeft(Context context) {
        final int channel = ChannelRecognizeUtils.getCurrentChannel(context);
        final Resources res = context.getResources();
        switch (channel) {
            case ChannelRecognizeUtils.CHANNEL_PHONE_DEFAULT:
                return res.getDimensionPixelSize(R.dimen.indicator_margin_left_phone_default);
            default:
                return res.getDimensionPixelSize(R.dimen.indicator_margin_left_phone_default);
        }
    }
    
    public static int getLauncherMarginLeft(Context context) {
        final String navibarOrientagion = ChannelRecognizeUtils.getNavibarOrientation();
        final Resources res = context.getResources();
        if (ChannelRecognizeUtils.CHANNEL_NAVIBAR_ORIENTATION_VALUE_LEFT.equals(navibarOrientagion)) {
            return res.getDimensionPixelSize(R.dimen.launcher_margin_none);
        } else {
            return res.getDimensionPixelSize(R.dimen.launcher_margin_none);
        }
    }
    
    public static int getLauncherMarginRight(Context context) {
        final String navibarOrientagion = ChannelRecognizeUtils.getNavibarOrientation();
        final Resources res = context.getResources();
        if (ChannelRecognizeUtils.CHANNEL_NAVIBAR_ORIENTATION_VALUE_LEFT.equals(navibarOrientagion)) {
            return res.getDimensionPixelSize(R.dimen.launcher_margin_none);
        } else {
            return res.getDimensionPixelSize(R.dimen.launcher_margin_navibar);
        }
    }
    
}
