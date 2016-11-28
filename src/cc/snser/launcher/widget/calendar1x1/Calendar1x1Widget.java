package cc.snser.launcher.widget.calendar1x1;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import cc.snser.launcher.widget.IconWidgetCache;
import cc.snser.launcher.widget.IconWidgetView;
import cc.snser.launcher.widget.calendar1x1.FullCalendar.DateOutOfRangeException;

import com.btime.launcher.util.XLog;
import com.btime.launcher.R;
import com.shouxinzm.launcher.ui.view.FastBitmapDrawable;
import com.shouxinzm.launcher.util.ToastUtils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.format.Time;
import android.view.View;

public class Calendar1x1Widget extends IconWidgetView {
	public static final int SPANX = 1;
	public static final int SPANY = 1;
    
	private Context mContext = null;    
    private String mLabel = null;
    
	private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;
    
	private static Bitmap  mBitmapBackground;
	private final Paint  mBitmapPaint = new Paint();
	
	private static List<SoftReference<Bitmap>> mBitmapCacheMonth = new ArrayList<SoftReference<Bitmap>>(12);
	
	private static List<SoftReference<Bitmap>> mBitmapCacheNumber = new ArrayList<SoftReference<Bitmap>>(10);

	public Calendar1x1Widget(Activity context) {
		super(context);
		XLog.i("liliang_calendar", "Calendar1x1Widget::Calendar1x1Widget" );
		
		for(int i = 0; i < 12; i ++){
			mBitmapCacheMonth.add(null);
		}
		
		for(int i = 0; i < 10; i ++){
			mBitmapCacheNumber.add(null);
		}
		
		mContext = context;
		setLabel(R.string.widget_name_calendar1x1);
		updateView();
	}
	
	
	private Bitmap getMonthBitmap(int month){
		if(month < 0 || month > 11) return null;
		Bitmap bitmap = null;
		
		SoftReference<Bitmap> soft = mBitmapCacheMonth.get(month);
		if(soft != null){
			bitmap = soft.get();
		}
		
		if(bitmap != null) return bitmap;
		
		int resId = getResources().getIdentifier("calendar1x1_mon_" + month,
				"drawable", getContext().getPackageName());
		bitmap = BitmapFactory.decodeResource(getResources(), resId);
		if(bitmap == null) return null;
		
		mBitmapCacheMonth.set(month, new SoftReference<Bitmap>(bitmap));
		
		return bitmap;
	}
	
	private Bitmap getNumberBitmap(int number){
		if(number < 0 || number > 9) return null;
		Bitmap bitmap = null;
		
		SoftReference<Bitmap> soft = mBitmapCacheNumber.get(number);
		if(soft != null){
			bitmap = soft.get();
		}
		
		if(bitmap != null) return bitmap;
		
		int resId = getResources().getIdentifier("calendar1x1_num_" + number,
				"drawable", getContext().getPackageName());
		bitmap = BitmapFactory.decodeResource(getResources(), resId);
		if(bitmap == null) return null;
		
		mBitmapCacheNumber.set(number, new SoftReference<Bitmap>(bitmap));
		
		return bitmap;
	}
	

	
    public void setLabel(int residLabel) {
		XLog.i("liliang_calendar", "Calendar1x1Widget::setLabel" );
    	try {
    		setLabel(getResources().getString(residLabel));
		} catch (Exception e) {
			//do nothing
		}
    }
    
    public void setLabel(String label) {
    	if (label != null) {
    		mIcon.setText(label);
    		mLabel = label;
    	}
    }
	
	private Time getTodayTime() {
		XLog.i("liliang_calendar", "Calendar1x1Widget::getTodayTime" );
        Time time = new Time();
        time.setToNow();
        return time;
    }
	
	private boolean isNewDay(Time today) {
		XLog.i("liliang_calendar", "Calendar1x1Widget::isNewDay" );
        int hour = today.hour;
        int min = today.minute;
        return hour == 0 && min == 0;
    }
    
	public void registerReceiver(){
		XLog.i("liliang_calendar", "Calendar1x1Widget::registerReceiver" );

		if (mReceiver == null) {
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();
					XLog.i("liliang", "atction = " + action );
					if (Intent.ACTION_TIME_TICK.equals(action)) {
                     
						Time today = getTodayTime();
						// 日期变化�?
						if (isNewDay(today)) {
							updateView();
						} 
					}
					if (Intent.ACTION_DATE_CHANGED.equals(action) || Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
						updateView();
					} 
				}
			};
		}

		if (mFilter == null) {
			mFilter = new IntentFilter();
			mFilter.addAction(Intent.ACTION_TIME_TICK);
			mFilter.addAction(Intent.ACTION_TIME_CHANGED);
			mFilter.addAction(Intent.ACTION_DATE_CHANGED);
			mFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		}

		try {
			this.getContext().registerReceiver(mReceiver, mFilter);
		} catch (Exception e) {
         // ignore
		}
	}

	private void unregisterReceiver() {
		XLog.i("liliang_calendar", "Calendar1x1Widget::unregisterReceiver" );
		try {
			this.getContext().unregisterReceiver(mReceiver);
		} catch (Exception e) {
         // ignore
		}
	}
	
	private Bitmap getBackgroundBitmap(){
		if(mBitmapBackground != null)
			return mBitmapBackground;
		mBitmapBackground = BitmapFactory.decodeResource(getResources(), R.drawable.calendar1x1_bg);
		
		return mBitmapBackground;
	}
	
	private Drawable getIcon(){
		
		Bitmap bitmapBackground = getBackgroundBitmap();
		
		if( null == bitmapBackground ){
			return null;
		}
		
		float margin = DisplayUtil.dip2px(getContext(), 13);
		
		XLog.i("liliang_calendar", "Calendar1x1Widget::getIcon bitmapBackground("+bitmapBackground.getWidth()+","+ bitmapBackground.getHeight() + ")" );
		
		Bitmap bm = Bitmap.createBitmap(bitmapBackground.getWidth(), bitmapBackground.getHeight(), Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bm);

		float left = 0.0f;
	    float top = 0.0f;
		
		canvas.drawBitmap(bitmapBackground, left, top, mBitmapPaint);
		
		FullCalendar calendar;
        try {
            calendar = new FullCalendar(getContext(), true);
        } catch (DateOutOfRangeException e) {
            return null;
        }
        
        int month = calendar.getGregorianMonth() - 1;
        int monthday = calendar.getGregorianDate();
        
        Bitmap bitmapMonth = getMonthBitmap(month);
        
        top += margin;
        
        if( null != bitmapMonth && null != bitmapBackground ){
        	left = bitmapBackground.getWidth()/2 - bitmapMonth.getWidth()/2;
			canvas.drawBitmap(bitmapMonth, left, top, mBitmapPaint);
        }        
        
				
		int digit = monthday / 10;
		Bitmap bitmap = getNumberBitmap(digit);
		if(bitmap != null&& null!=bitmapBackground){
			top = bitmapBackground.getHeight() - margin - bitmap.getHeight();
			left = bitmapBackground.getWidth()/2 - bitmap.getWidth();
			canvas.drawBitmap(bitmap, left, top, mBitmapPaint);
			left += bitmap.getWidth();
		}
		
		
		digit = monthday % 10;
		bitmap = getNumberBitmap(digit);
		if(bitmap != null){
			top = bitmapBackground.getHeight() - margin - bitmap.getHeight();
			canvas.drawBitmap(bitmap, left, top, mBitmapPaint);
			left += bitmap.getWidth();
		}
		
		return new FastBitmapDrawable(bm);
	}
	
	private void updateView(){
		XLog.i("liliang_calendar", "Calendar1x1Widget::updateView" );
    	try {
    		mIcon.setIcon(IconWidgetCache.getIconNoCache(mContext, getIcon()));
		} catch (Exception e) {
			//do nothing
		}
	}
	
	@Override
	public String getLabel() {
		XLog.i("liliang_calendar", "Calendar1x1Widget::getLabel" );
		return mLabel;
	}

	@Override
	public void onLauncherPause() {
		XLog.i("liliang_calendar", "Calendar1x1Widget::onPause" );
		unregisterReceiver();
	}

	@Override
	public void onLauncherResume() {
		XLog.i("liliang_calendar", "Calendar1x1Widget::onResume" );
		updateView();
		registerReceiver();
	}

	@Override
	public void onAdded(boolean newInstance) {
		XLog.i("liliang_calendar", "Calendar1x1Widget::onAdded" );
		updateView();
		registerReceiver();
	}

	@Override
	public void onRemoved(boolean permanent) {
		XLog.i("liliang_calendar", "Calendar1x1Widget::onRemoved" );
		unregisterReceiver();
	}

	@Override
	public void onDestroy() {
		XLog.i("liliang_calendar", "Calendar1x1Widget::onDestroy" );
	}

	@Override
	public void onScreenOn() {
		XLog.i("liliang_calendar", "Calendar1x1Widget::onScreenOn" );
		unregisterReceiver();
	}

	@Override
	public void onScreenOff() {
		XLog.i("liliang_calendar", "Calendar1x1Widget::onScreenOff" );
	}

	@Override
	public void onCloseSystemDialogs() {
		XLog.i("liliang_calendar", "Calendar1x1Widget::onCloseSystemDialogs" );
	}
	
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }
	
	@Override
	public int getSpanX() {
		XLog.i("liliang_calendar", "Calendar1x1Widget::getSpanX" );
		return SPANX;
	}

	@Override
	public int getSpanY() {
		XLog.i("liliang_calendar", "Calendar1x1Widget::getSpanY" );
	    return SPANY;
	}
	
    @Override
    public void handleClickMainVew(View target) {
        //Launcher.getInstance().hideNavigation();
        Context context = getContext();
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            ToastUtils.showMessage(getContext(), "vname=" + packageInfo.versionName + " vcode=" + packageInfo.versionCode);
        } catch (Exception e) {
            // TODO: handle exception
        }

/*        if (getContext() instanceof Launcher) {
        	 Context context = getContext().getApplicationContext();
             ClockWeatherCommon.gotoDatePage(context);
        }*/
    }


    @Override
    public void onLauncherLoadingFinished() {
        // TODO Auto-generated method stub
        
    }
	
//    int month = calendar.getGregorianMonth() - 1;
//    
//	int resId = getResources().getIdentifier("calendar1x1_mon_" + month,
//			"drawable", getContext().getPackageName());
//	Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);

}
