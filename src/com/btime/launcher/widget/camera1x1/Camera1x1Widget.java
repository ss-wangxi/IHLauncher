package com.btime.launcher.widget.camera1x1;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;

import com.btime.launcher.R;
import com.btime.launcher.app.AppController;
import com.btime.launcher.widget.entrance.EntranceWidget;

public class Camera1x1Widget extends EntranceWidget {

    public Camera1x1Widget(Activity context) {
        super(context);
        setLabel(R.string.widget_camera1x1_name);
        setLaunchIntent(getLaunchIntent());
    }
    
    private Intent getLaunchIntent() {
        final Intent intent = new Intent();
        intent.setPackage(AppController.PKGNAME_C601_CAMERA);
        intent.setComponent(new ComponentName(AppController.PKGNAME_C601_CAMERA, AppController.CLSNAME_C601_CAMERA));
        return intent;
    }

}
