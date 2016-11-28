
package cc.snser.launcher.apps.model;

import android.graphics.drawable.Drawable;
import cc.snser.launcher.IconCache;

/**
 * Interface which can be locate on dockbar.
 *
 * @author shixiaolei
 */
public interface DockBarItemInfo {

    public Drawable getIcon(IconCache iconCache);

    public Cellable getCellable();

    public CharSequence getTitle();
}
