package cc.snser.launcher.apps.model;

import cc.snser.launcher.LauncherSettings;

/**
 * 界面排版项（桌面项、停靠条项、文件夹、抽屉项、图片项、视频项）
 * @author songzhaochun
 *
 */
public abstract class ItemInfo implements Cellable {

    public static final int NO_ID = -1;

    /**
     * The id in the settings database for this item
     */
    public long id = NO_ID;

    /**
     * One of: <br>
     * {@link LauncherSettings.BaseLauncherColumns#ITEM_TYPE_APPLICATION}<br>
     * {@link LauncherSettings.BaseLauncherColumns#ITEM_TYPE_SHORTCUT}<br>
     * {@link LauncherSettings.BaseLauncherColumns#ITEM_TYPE_USER_FOLDER}<br>
     * {@link LauncherSettings.Favorites#ITEM_TYPE_APPWIDGET}
     */
    public int itemType;

    /**
     * The id of the container that holds this item. For the desktop, this will
     * be {@link LauncherSettings.Favorites#CONTAINER_DESKTOP}. For the all
     * applications folder it will be {@link #NO_ID} (since it is not stored in
     * the settings DB). For user folders it will be the id of the folder.
     */
    public long container = NO_ID;

    /**
     * Iindicates the screen in which the shortcut appears.
     */
    public int screen = -1;

    /**
     * Indicates the X position of the associated cell.
     */
    public int cellX = -1;

    /**
     * Indicates the Y position of the associated cell.
     */
    public int cellY = -1;

    /**
     * Indicates the X cell span.
     */
    public int spanX = 1;

    /**
     * Indicates the Y cell span.
     */
    public int spanY = 1;
    
    public boolean isInSecondLayer = false;

    public void unbind() {
        // do nothing defaultly
    }

    @Override
    public int getScreen() {
        return screen;
    }

    @Override
    public void setScreen(int screen) {
        this.screen = screen;
    }

    @Override
    public int getCellX() {
        return cellX;
    }

    @Override
    public void setCellX(int cellX) {
        this.cellX = cellX;
    }

    @Override
    public int getCellY() {
        return cellY;
    }

    @Override
    public void setCellY(int cellY) {
        this.cellY = cellY;
    }

    @Override
    public long getContainer() {
        return container;
    }

    @Override
    public void setContainer(long container) {
        this.container = container;
    }

    public boolean isIcon() {
        return spanX == 1 && spanY == 1;
    }

    public boolean acceptByFolder() {
        return true;
    }

    public boolean acceptByDockbar() {
        return true;
    }

    public boolean isDeleteable() {
        return true;
    }

    public Object getNotificaiton() {
        return null;
    }
}


