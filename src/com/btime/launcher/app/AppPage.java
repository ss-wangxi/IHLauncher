package com.btime.launcher.app;

public class AppPage extends AppType {
    public static final AppPage PAGE_TRAFFIC_CAROS_MAIN = new AppPage(100501);
    public static final AppPage PAGE_SETTINGS_CAROS_MAIN = new AppPage(100901);
    
    protected AppPage(int type) {
        super(type);
    }
}
