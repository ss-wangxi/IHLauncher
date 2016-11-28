package cc.snser.launcher.widget;

import android.app.Activity;
import android.content.Context;

public interface IWidgetContext {
    public boolean isScrolling();
    public Context getApplicationContext();
    public Activity getLauncher();
    public IWorkspaceContext getWorkspaceContext();
}
