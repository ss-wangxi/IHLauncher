package cc.snser.launcher.component.themes.base.model.local;

import android.graphics.Bitmap;

public interface LocalModel {
    public String getId();

    public Bitmap getOverviewFromCache();

    public Bitmap getOverview();

    public void clear();
}
