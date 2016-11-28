package cc.snser.launcher.apps.model;

import cc.snser.launcher.LauncherSettings;
import android.content.ContentValues;
import android.content.Intent;

public class HiddenApplication {

    static final int NO_ID = -1;

    /**
     * The id in the settings database for this item
     */
    public long id = NO_ID;

    /**
     * The intent used to start the application.
     */
    public Intent intent;

    /**
     * Write the fields of this item to the DB
     *
     * @param values
     */
    public void onAddToDatabase(ContentValues values) {
        String uri = intent != null ? intent.getComponent().flattenToShortString() : null;
        values.put(LauncherSettings.AppHideList.INTENT, uri);
    }
}
