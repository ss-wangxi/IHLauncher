package com.btime.launcher.widget.gallery1x1;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;

import com.btime.launcher.R;
import com.btime.launcher.app.AppController;
import com.btime.launcher.widget.entrance.EntranceWidget;

public class Gallery1x1Widget extends EntranceWidget {

    public Gallery1x1Widget(Activity context) {
        super(context);
        setLabel(R.string.widget_gallery1x1_name);
        setLaunchIntent(getLaunchIntent());
    }
    
    private Intent getLaunchIntent() {
        final Intent intent = new Intent();
        intent.setPackage(AppController.PKGNAME_C601_GALLERY);
        intent.setComponent(new ComponentName(AppController.PKGNAME_C601_GALLERY, AppController.CLSNAME_C601_GALLERY));
        return intent;
    }

}
