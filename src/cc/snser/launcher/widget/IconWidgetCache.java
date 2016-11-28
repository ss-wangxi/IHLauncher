
package cc.snser.launcher.widget;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import cc.snser.launcher.apps.model.workspace.HomeDesktopItemInfo;
import cc.snser.launcher.ui.utils.Utilities;
import cc.snser.launcher.ui.utils.WorkspaceIconUtils;

import com.shouxinzm.launcher.ui.view.FastBitmapDrawable;

/**
 * Cache for icon widget.
 *
 * @author shixiaolei
 */
public class IconWidgetCache {
    private static final Map<Serializable, IconWigetInfoHolder> ALL = new HashMap<Serializable, IconWigetInfoHolder>(4);
    private static final SparseArray<Drawable> ICON_CACHE = new SparseArray<Drawable>();

    public static final void clear() {
        ALL.clear();
        ICON_CACHE.clear();
    }

    public static final List<IconWigetInfoHolder> all(Activity context) {
        if (ALL.isEmpty()) {
            
        }
        return new ArrayList<IconWigetInfoHolder>(ALL.values());
    }

    private static void loadBuiltIn(Activity context, int widgetTypeRes, String title, Drawable icon) {
        Resources resources = context.getResources();
        int widgetTypeId = resources.getInteger(widgetTypeRes);
        BuiltinWidget widget = BuiltinWidgetMgr.get(context, widgetTypeId);
        if (widget == null) {
            return;
        }

        if (icon == null) {
            icon = widget.getPreview();
        }

        ALL.put(widgetTypeId, new IconWigetInfoHolder(widgetTypeId, title, icon));
    }

    public static final Drawable getIcon(Context context, int widgetViewTypeResId, String themeResName, int defaultResId) {
        int widgetViewType = context.getResources().getInteger(widgetViewTypeResId);

        Drawable ret = ICON_CACHE.get(widgetViewType);
        if (ret == null) {
            boolean addBoard = false;
          
            if (ret == null) {
                ret = Utilities.getDrawableDefault(context, defaultResId, true);
            }
            if (ret != null) {
//            	Bitmap result = MiIconStyleSwitcher.getInstance().handleInternalWidget(widgetViewType, ret);
//            	BitmapDrawable iconBg = (result != null) ? new BitmapDrawable(context.getResources(),ret) : SimpleThemeAdapter.getInstance().getAppIconBg(ret);
//            	if(iconBg == null){
//            		ret = new FastBitmapDrawable(WorkspaceIconUtils.createIconBitmap(ret, context, addBoard || Theme.isUsingOldTheme(context), false));
//            	}
//            	else {
//            		ret =  new FastBitmapDrawable(iconBg.getBitmap());
//            	}
            	ICON_CACHE.put(widgetViewType, ret);
            }
        }
        return ret;
    }
    
    /**
     * 获取图标，进行加背景/适配等处理，但不读写Cache
     * @param context
     * @param themeResName
     * @param defaultResId
     * @author snsermail@gmail.com
     * @return
     */
    public static final Drawable getIconNoCache(Context context, String themeResName, int defaultResId) {
        Drawable ret = null;
        boolean addBoard = false;
        
        if (ret == null) {
            ret = Utilities.getDrawableDefault(context, defaultResId, true);
        }
        
        if (ret != null) {
        	ret = new FastBitmapDrawable(WorkspaceIconUtils.createIconBitmap(ret, context, addBoard, false));
        }
        
        return ret;
    }
    
    /**
     * 获取图标，进行加背景/适配等处理，但不读写Cache
     * @param context
     * @param themeResName
     * @param defaultResId
     * @author snsermail@gmail.com
     * @return
     */
    public static final Drawable getIconNoCache(Context context, Drawable drawable) {
		if (drawable != null) {
			drawable = new FastBitmapDrawable(WorkspaceIconUtils.createIconBitmap(drawable, context, false, false));
		}
		
		return drawable;
    }

    // TODO: 重构：讨论此处继承的意义 考虑去掉
    public static class FolderExsitingWidgetMockInfo extends HomeDesktopItemInfo {

        public Drawable icon;
        public Serializable identity;

        public FolderExsitingWidgetMockInfo(IconWigetInfoHolder holder) {
            super();
            this.identity = holder.getIdentity();
            this.defaultTitle = holder.getTitle();
            this.icon = holder.getIcon();
            this.mCustomType = HomeDesktopItemInfo.CUSTOM_TYPE_WIDGET;
        }

        public static List<FolderExsitingWidgetMockInfo> all(Activity context) {
            List<FolderExsitingWidgetMockInfo> infos = new ArrayList<FolderExsitingWidgetMockInfo>(4);
            List<IconWigetInfoHolder> holders = IconWidgetCache.all(context);
            for (IconWigetInfoHolder holder : holders) {
                infos.add(new FolderExsitingWidgetMockInfo(holder));
            }
            return infos;
        }
    }

    /**
     * a holder to descript icon widget.
     *
     * @author shixiaolei
     */
    public static class IconWigetInfoHolder {

        private Serializable identity;

        private String title;

        private Drawable icon;

        public IconWigetInfoHolder(Serializable identity, String title, Drawable icon) {
            super();
            this.identity = identity;
            this.title = title;
            this.icon = icon;
        }

        public String getTitle() {
            return title;
        }

        public Drawable getIcon() {
            return icon;
        }

        public Serializable getIdentity() {
            return identity;
        }

    }
}
