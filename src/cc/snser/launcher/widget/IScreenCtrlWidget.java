package cc.snser.launcher.widget;

import android.graphics.Rect;

public interface IScreenCtrlWidget {
    
    public void setWorkspaceInfo(int workspaceWidth, int minScrollX, int maxScrollX);
    
    public void setWidget2x3Info(Rect size, Rect margin);
    
    public void setVisibleRegion(Rect region, int screen);
    
    public void onWorkspaceScroll(int x, int y);
    
    public void onWorkspaceScrollStart(int currentScreen, int scrollX, int scrollY);
    
    public void onWorkspaceScrollStop(int currentScreen, int scrollX, int scrollY);
    
    public void onWorkspaceDragStart();
    
    public void onWorkspaceDragStop();
    
    public void onWidgetScreenIn();
    
    public void onWidgetScreenOut();
    
    public void onActionDown(int currentScreen);
    
    public void onActionUp(int currentScreen);
    
    public void onAccOn();
    
    public void onAccOff();
    
}
