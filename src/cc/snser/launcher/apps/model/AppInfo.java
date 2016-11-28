package cc.snser.launcher.apps.model;

import android.content.Intent;
import cc.snser.launcher.IconCache;

import com.shouxinzm.launcher.ui.view.FastBitmapDrawable;

/**
 * 可启动/可打开项（抽象意义上的应用）
 * @author all
 *
 */
public interface AppInfo {

    /**
     * 启动Intent
     * @return
     */
    public Intent getIntent();

    /**
     * 图标
     * @return
     */
    public FastBitmapDrawable getIcon();

    /**
     * ?
     * @param iconCache
     * @return
     */
    public FastBitmapDrawable getIcon(IconCache iconCache);

    /**
     * 指定图标
     * @param icon
     */
    public void setIcon(FastBitmapDrawable icon);

    /**
     * 显示名称
     * @return
     */
    public String getTitle();

    /**
     * 指定显示名称
     * @param title
     */
    public void setTitle(String title);

    /**
     * 最后更新时间
     * @return
     */
    public long getLastUpdateTime();

    /**
     * 最后打开时间
     * @return
     */
    public long getLastCalledTime();

    /**
     * 打开次数
     * @return
     */
    public int getCalledNum();

    /**
     * ?
     * @return
     */
    public int getStorage();

    /**
     * 是否系统级的
     * @return
     */
    public boolean isSystem();
}
