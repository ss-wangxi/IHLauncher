package cc.snser.launcher.widget.clock1x1;

import cc.snser.launcher.ui.utils.IconMetrics;
import cc.snser.launcher.widget.IconWidgetView;

import com.btime.launcher.R;
import com.shouxinzm.launcher.ui.view.BitmapDrawable;
import com.shouxinzm.launcher.util.BitmapUtils;
import com.shouxinzm.launcher.util.ScreenDimensUtils;
import com.shouxinzm.launcher.util.ToastUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.View;

public class Clock1x1Widget extends IconWidgetView {

	public static final int SPANX = 1;
	public static final int SPANY = 1;
	
    private static final String TAG = "widget.clock1.AnalogClock1";
	
    private static Bitmap mbitmapPanel = null;
    private static Bitmap mbitmapHour  = null;
    private static Bitmap mbitmapMinute = null;
    private static Bitmap mbitmapSecond = null; 
    
    public static int mClockCnts = 0;
			
	private final int MSG_SECOND = 1;
	private Handler mHandlerSecond = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if ( msg.what == MSG_SECOND ){
				updateClock();
				mHandlerSecond.sendEmptyMessageDelayed(MSG_SECOND, 500);
			}
			super.handleMessage(msg);
		}
	};
	
	private String mLabel = null;
	
	public Clock1x1Widget(Activity context) {
		super(context);
        initIcon();
    }
	
	private void initIcon() {
		if ( mbitmapPanel==null || mbitmapHour==null || mbitmapMinute==null || mbitmapSecond==null ){
	    	mbitmapPanel = BitmapFactory.decodeResource(getResources(), R.drawable.clock1x1_panel);
	    	mbitmapHour  = BitmapFactory.decodeResource(getResources(), R.drawable.clock1x1_panel_hour);
	    	mbitmapMinute= BitmapFactory.decodeResource(getResources(), R.drawable.clock1x1_panel_minute);
	    	mbitmapSecond= BitmapFactory.decodeResource(getResources(), R.drawable.clock1x1_panel_second);
		}
		
		Drawable drawablePanel = new BitmapDrawable(getResources(), mbitmapPanel);
//		Drawable drawableAdapter = IconWidgetCache.getIconNoCache(mContext, drawablePanel, false);
		
		Clock1x1BitmapDrawable drawable = new Clock1x1BitmapDrawable(mbitmapPanel, mbitmapHour, mbitmapMinute, mbitmapSecond);
		drawable.setHostView(mIcon);
		drawable.setIconMetrics(IconMetrics.getInstance(getContext()));
		drawable.setAdapterScale(IconMetrics.getIconWidth(getContext()) * 1.0f / drawablePanel.getIntrinsicWidth());
//		if (SimpleThemeAdapter.getInstance().isNeedAdapter()) {
//		} else {
//			drawable.setAdapterScale(1.0f);
//		}
		
		mIcon.setIcon(drawable);
		mIcon.setText(R.string.widget_name_clock1x1);
		
		mLabel = getResources().getString(R.string.widget_name_clock1x1);
	}
	
	private void updateClock() {
		Drawable iconDrawable = mIcon.getIcon();
		if (iconDrawable instanceof Clock1x1BitmapDrawable) {
			((Clock1x1BitmapDrawable)iconDrawable).refresh();
		}
	}
	
	private void recycleBmp(){
		BitmapUtils.recycleBitmap(mbitmapPanel);
		BitmapUtils.recycleBitmap(mbitmapHour);
		BitmapUtils.recycleBitmap(mbitmapMinute);
		BitmapUtils.recycleBitmap(mbitmapSecond);
		
		mbitmapPanel = null;
		mbitmapHour = null;
		mbitmapMinute = null;
		mbitmapSecond = null;
	}
	
	public void startTick(){
		if (!mHandlerSecond.hasMessages(MSG_SECOND)) {
			mHandlerSecond.sendEmptyMessageDelayed(MSG_SECOND, 500);
		}
	}

	private void stopTick() {
		mHandlerSecond.removeMessages(MSG_SECOND);
	}
	
	@Override
	public String getLabel() {
		return mLabel;
	}

	@Override
	public void onLauncherPause() {
		stopTick();
	}

	@Override
	public void onLauncherResume() {
		startTick();
	}

	@Override
	public void onAdded(boolean newInstance) {
		startTick();
		mClockCnts++;
	}

	@Override
	public void onRemoved(boolean permanent) {
		stopTick();

		if(--mClockCnts == 0){
			recycleBmp();	
		}
	}

	@Override
	public void onDestroy() {
		stopTick();
		
		if(mHandlerSecond != null){
			mHandlerSecond.removeMessages(MSG_SECOND);
		}
	}

	@Override
	public void onScreenOn() {
		startTick();
	}

	@Override
	public void onScreenOff() {
		stopTick();
	}

	@Override
	protected boolean handleOnScreenOutEvent() {
		return true;
	}
	
	@Override
	protected void onScreenIn() {
		startTick();
	}
	
	@Override
	protected void onScreenOut() {
		stopTick();
	}

	@Override
	protected boolean handleOnScreenInEvent() {
		return true;
	}

	@Override
	public void handleClickMainVew(View v) {
        boolean isDebugMode = false;
        try {  
            ApplicationInfo info= getContext().getApplicationInfo();
            isDebugMode = (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {  
             e.printStackTrace(); 
        }  
        
        String prop = System.getProperty("os.car", "null");
        
	    float unit = getResources().getDimension(R.dimen.snser_test_unit);
	    float screen = getResources().getDimension(R.dimen.snser_test_screen);
	    int screenwidth = ScreenDimensUtils.getScreenWidth(getContext());
	    int screenHeight = ScreenDimensUtils.getScreenHeight(getContext());
	    ToastUtils.showMessage(getContext(), "u=" + unit + " s=" + screen + " w=" + screenwidth + " h=" + screenHeight + " debug=" + isDebugMode + " prop=" + prop);
	    
/*		SystemSettingsUtils.gotoAlarmClock(getContext());
		super.handleClickMainVew(v);*/
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
    public void onLauncherLoadingFinished() {
        // TODO Auto-generated method stub
        
    }
     
}
