package cc.snser.launcher.widget.setting2x3;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class Setting2x3View extends RelativeLayout {

	public Setting2x3View(Context context) {
		super(context);
	}
	public Setting2x3View(Context context, AttributeSet attrs) {
		super(context,attrs);
	}
	public Setting2x3View(Context context, AttributeSet attrs,int defStyle) {
		super(context,attrs,defStyle);
	}

	@Override
	protected void onFinishInflate() {
	    super.onFinishInflate();
	}
	
	public void setHost(Setting2x3Widget host) {
    }

}
