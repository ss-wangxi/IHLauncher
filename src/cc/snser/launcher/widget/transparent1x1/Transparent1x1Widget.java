package cc.snser.launcher.widget.transparent1x1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.widget.BuiltinWidget;
import cc.snser.launcher.widget.BuiltinWidgetMgr;
import cc.snser.launcher.widget.WidgetView;

import com.btime.launcher.R;

public class Transparent1x1Widget extends WidgetView {
    
    public static final int SPANX = 1;
    public static final int SPANY = 1;
    
    private Transparent1x1View mRootView;

    public Transparent1x1Widget(Activity context) {
        super(context);
        
        setGravity(Gravity.CENTER);
        
        mRootView = (Transparent1x1View)inflate(context, R.layout.widget_transparent_1x1_view, null);
        mRootView.setHost(this);
        mRootView.setOnClickListener(new OnWidgetClickListenser());
        addView(mRootView);
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public void onLauncherPause() {
    }

    @Override
    public void onLauncherResume() {
    }

    @Override
    public void onAdded(boolean newInstance) {
    }

    @Override
    public void onRemoved(boolean permanent) {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onScreenOn() {
    }

    @Override
    public void onScreenOff() {
    }

    @Override
    public void onCloseSystemDialogs() {
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }
    
    @Override
    public int getSpanX() {
        return SPANX;
    }
    
    @Override
    public int getSpanY() {
        return SPANY;
    }
    
    @Override
    protected boolean checkForLongClick() {
        return false;
    }

    @Override
    public boolean acceptByFolder() {
        return false;
    }
    
    private class OnWidgetClickListenser implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final Context context = getContext();
            final int widgetType = context.getResources().getInteger(R.integer.widget_view_type_transparent1x1);
            final BuiltinWidget widget = BuiltinWidgetMgr.get(context, widgetType);
            Launcher.getInstance().completeAddWidgetView(widget);
        }
    }

    @Override
    public void onLauncherLoadingFinished() {
        // TODO Auto-generated method stub
        
    }

}
