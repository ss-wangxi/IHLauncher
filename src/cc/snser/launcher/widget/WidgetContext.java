package cc.snser.launcher.widget;

import android.app.Activity;
import android.content.Context;
import cc.snser.launcher.App;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.widget.IWidgetContext;

import com.btime.launcher.CarOSLauncherBase;

public class WidgetContext implements IWidgetContext {

    private Launcher mLauncher;
    private IWorkspaceContext mWorkspaceContext;
    
    public WidgetContext(Launcher launcher){
        mLauncher = launcher;
    }
    
    @Override
    public boolean isScrolling() {
        if(mLauncher != null){
            if (mLauncher.getWorkspace() != null){
                return mLauncher.getWorkspace().isScrolling();
            }
        }
        return false;
    }
    
    @Override
    public Context getApplicationContext() {
        return App.getAppContext();
    }

    @Override
    public Activity getLauncher() {
        return App.getApp().getLauncher();
    }

    @Override
    public IWorkspaceContext getWorkspaceContext() {
        if(mLauncher != null && mWorkspaceContext == null){
            mWorkspaceContext = (IWorkspaceContext)mLauncher.getWorkspace();
        }
        return mWorkspaceContext;
    }

}
