package cc.snser.launcher.model;

public class FavoritesItemInfo {
	public int id;
	public String title;
	public String intent;
	public int container;
	public int screen;
	public int cellX;
	public int cellY;
	public int spanX;
	public int spanY;
	public int itemType;
	public int appWidgetId = -1;
	public boolean isShortcut;
	public int iconType;
	public String iconPackage;
	public String iconResource;
	public String titlePackage;
	public String titleResource;
	public byte[]  icon;
	public long   last_update_time = 0;
	public long   last_called_time = 0;
	public long   called_num = 0;
	public int    storage = -1;
	public boolean  system = false;
	public int    category = -1;
}
