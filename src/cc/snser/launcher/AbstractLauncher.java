package cc.snser.launcher;


import android.view.View;
import android.view.ViewGroup;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.model.LauncherModel;

public interface AbstractLauncher {
	public View createShortcut(HomeDesktopItemInfo info);
	public View createShortcut(int layoutResId, ViewGroup parent, HomeDesktopItemInfo info);
	
	
	public void removeItem(HomeDesktopItemInfo itemInfo, final boolean isRemoveModelOnly);
	public void removeItemOnly(HomeDesktopItemInfo itemInfo);
	
	
	public LauncherModel getModel();
}
