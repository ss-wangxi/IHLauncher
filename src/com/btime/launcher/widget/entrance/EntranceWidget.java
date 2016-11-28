package com.btime.launcher.widget.entrance;

import com.btime.launcher.R;
import com.btime.launcher.app.PackageEventReceiver;
import com.btime.launcher.app.PackageEventReceiver.IPackageEventCallback;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import cc.snser.launcher.App;
import cc.snser.launcher.widget.ScreenCtrlWidget;

public class EntranceWidget extends ScreenCtrlWidget implements View.OnClickListener, IPackageEventCallback {
    public static final int SPANX = 1;
    public static final int SPANY = 1;
    
    private EntranceView mEntranceView;
    
    private View mBase;
    private ImageView mIcon;
    private TextView mLabel;
    
    private String mPkg;
    private Intent mLaunchIntentSpecified;
    private Intent mLaunchIntent;

    public EntranceWidget(Activity context) {
        super(context);
        setGravity(Gravity.CENTER);
        
        mEntranceView =  (EntranceView)inflate(context, R.layout.widget_entrance_view, null);
        mEntranceView.setHost(this);
        mBase = mEntranceView.findViewById(R.id.widget_entrance_base);
        mBase.setOnClickListener(this);
        mIcon = (ImageView)mEntranceView.findViewById(R.id.widget_entrance_icon);
        mLabel = (TextView)mEntranceView.findViewById(R.id.widget_entrance_label);
        addView(mEntranceView);
        
        PackageEventReceiver.registerPackageEventCallback(this);
    }
    
    @Override
    public void onDestroy() {
        onRemoved(true);
        super.onDestroy();
    }
    
    @Override
    public void onRemoved(boolean permanent) {
        PackageEventReceiver.unregisterPackageEventCallback(this);
    }

    @Override
    public void onClick(View v) {
        if (mLaunchIntent != null) {
            try {
                final Intent intent = new Intent(mLaunchIntent);
                getContext().startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void setLaunchPackage(String pkgname) {
        if (!TextUtils.isEmpty(pkgname)) {
            mPkg = pkgname;
            final PackageManager pm = getContext().getPackageManager();
            if (pm != null) {
                PackageInfo pkgInfo = null;
                try {
                    final int flags = PackageManager.GET_ACTIVITIES | PackageManager.GET_DISABLED_COMPONENTS;
                    pkgInfo = pm.getPackageInfo(pkgname, flags);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (pkgInfo != null && pkgInfo.activities != null && pkgInfo.applicationInfo != null) {
                    String label = null;
                    Drawable icon = null;
                    Intent launchIntent = null;
                    if (mLaunchIntentSpecified != null) {
                        //通过setLaunchIntent方法设置了明确的Intent
                        String clsSpecified = mLaunchIntentSpecified.getComponent().getClassName();
                        ActivityInfo activityInfoSpecified = null;
                        for (ActivityInfo activityInfo : pkgInfo.activities) {
                            if (activityInfo.name.equals(clsSpecified)) {
                                activityInfoSpecified = activityInfo;
                                break;
                            }
                        }
                        if (activityInfoSpecified != null) {
                            label = activityInfoSpecified.loadLabel(pm).toString();
                            icon = activityInfoSpecified.loadIcon(pm);
                            launchIntent = mLaunchIntentSpecified;
                        }
                    } else {
                        //默认，读取该应用的Launcher Intent
                        label = pkgInfo.applicationInfo.loadLabel(pm).toString();
                        icon = pkgInfo.applicationInfo.loadIcon(pm);
                        launchIntent = pm.getLaunchIntentForPackage(pkgname);
                    }
                    setLabel(label);
                    setIcon(icon);
                    setLaunchIntentInternal(launchIntent);
                }
            }
        }
    }
    
    public void setLaunchIntent(Intent intent) {
        if (intent != null) {
            mLaunchIntentSpecified = intent;
            setLaunchPackage(getIntentPackage(intent));
        }
    }
    
    private void setLaunchIntentInternal(Intent intent) {
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            mLaunchIntent = intent;
        }
    }
    
    public void setIcon(int resId) {
        if (mIcon != null) {
            mIcon.setImageResource(resId);
        }
    }
    
    public void setIcon(Drawable drawable) {
        if (mIcon != null && drawable != null) {
            mIcon.setImageDrawable(drawable);
        }
    }
    
    public void setLabel(int resId) {
        if (mLabel != null) {
            mLabel.setText(resId);
        }
    }
    
    public void setLabel(String label) {
        if (mLabel != null && label != null) {
            mLabel.setText(label);
        }
    }
    
    @Override
    public void onPackageEvent(Context context, String action, String pkgname) {
        if (filterPackageChanged() && pkgname != null && pkgname.equals(mPkg)) {
            switch (action) {
                case Intent.ACTION_PACKAGE_ADDED:
                case Intent.ACTION_PACKAGE_CHANGED:
                case Intent.ACTION_PACKAGE_REPLACED:
                    handlePackageChanged();
                    break;
                default:
                    break;
            }
        }
    }
    
    protected boolean filterPackageChanged() {
        return true;
    }
    
    protected void handlePackageChanged() {
        App.getApp().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setLaunchPackage(mPkg);
            }
        });
    }
    
    private String getIntentPackage(Intent intent) {
        String pkg = null;
        if (intent != null) {
            pkg = intent.getPackage();
            if (pkg == null && intent.getComponent() != null) {
                pkg = intent.getComponent().getPackageName();
            }
        }
        return pkg;
    }
    
    @Override
    public int getSpanX() {
        return SPANX;
    }
    
    @Override
    public int getSpanY() {
        return SPANY;
    }

}
