package cc.snser.launcher.widget.setting2x3;

import cc.snser.launcher.widget.ScreenCtrlWidget;

import com.btime.launcher.app.AppController;
import com.btime.launcher.app.AppType;
import com.btime.launcher.R;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;

public class Setting2x3Widget extends ScreenCtrlWidget implements View.OnClickListener {
	
	public static final int SPANX = 2;
	public static final int SPANY = 3;
	private Setting2x3View mWidgetView;
	public Setting2x3Widget(Activity context) {
		super(context);
		setGravity(Gravity.CENTER);
		mWidgetView =  (Setting2x3View)inflate(context, R.layout.widget_setting2x3_view, null);
        mWidgetView.setHost(this);
        mWidgetView.findViewById(R.id.widget_setting2x3_base).setOnClickListener(this);
        mWidgetView.findViewById(R.id.widget_setting2x3_btn).setOnClickListener(this);
        addView(mWidgetView);
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
    public void onClick(View v) {
        AppController.getInstance().startApp(AppType.TYPE_SETTINGS);
    }
    
}
