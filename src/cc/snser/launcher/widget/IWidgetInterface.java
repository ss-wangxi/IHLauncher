package cc.snser.launcher.widget;

public interface IWidgetInterface {
    
    public abstract void onAdded(boolean newInstance);

    public abstract void onRemoved(boolean permanent);
    
    public abstract void onResume();
    
    public abstract void onPause();
    
    public abstract void onDestroy();

    public abstract void onScreenOn();

    public abstract void onScreenOff();
    
    public abstract void onScreenIn();

    public abstract void onScreenOut();

    public abstract void onCloseSystemDialogs();
    
}
