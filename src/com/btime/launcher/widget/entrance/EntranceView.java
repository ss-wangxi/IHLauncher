package com.btime.launcher.widget.entrance;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class EntranceView extends RelativeLayout {

	public EntranceView(Context context) {
		super(context);
	}
	public EntranceView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public EntranceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onFinishInflate() {
	    super.onFinishInflate();
	}
	
	public void setHost(EntranceWidget host) {
    }

}
