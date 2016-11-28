package com.btime.launcher.app;

import com.btime.launcher.app.AppType;
import com.btime.launcher.app.MediaStatusInfo;

interface IAppService {

    AppType getAppPage(in AppType type);
    boolean startApp(in AppType type);
    boolean stopApp(in AppType type);
    boolean notifyStartApp(in AppType type);
    boolean notifyStartAppWithExtra(in AppType type, String extra);
    
    /**
     * 判断是否支持指定方向滑屏
     * @param direction 滑屏方向(1:右滑 -1:左滑 0:滑到主屏)
     * @return 0: 支持</br>
     *         -1: 不支持(当前不在桌面)</br>
     *         -2: 不支持(不支持该方向滑屏)
     */
    int checkSupportScrollPage(int direction);
    /**
     * 指定方向滑屏
     * @param direction 滑屏方向(1:右滑 -1:左滑 0:滑到主屏)
     * @return 0: 成功</br>
     *         -1: 失败(当前不在桌面)</br>
     *         -2: 失败(不支持该方向滑屏)
     */
    int scrollPage(int direction);
    boolean isAgreeUserServiceProtocol();
}