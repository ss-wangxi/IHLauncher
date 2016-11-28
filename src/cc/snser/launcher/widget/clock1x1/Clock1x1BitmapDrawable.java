package cc.snser.launcher.widget.clock1x1;

import cc.snser.launcher.ui.utils.IconMetrics;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.ui.view.FastBitmapDrawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.text.format.Time;
import android.view.View;

public class Clock1x1BitmapDrawable extends FastBitmapDrawable{
	
	private static final String TAG = "Clock1x1BitmapDrawable";
	private View mHostView;
	private IconMetrics mIconMetrics;
	private float mAdapterScale;
	
	private Bitmap mBitmapPanel;
	private Bitmap mBitmapHour;
	private Bitmap mBitmapMinute;
	private Bitmap mBitmapSecond;
	
	private Paint mPaint;
	
	private Time mNow;
	private int mHour;
	private int mMinute;
	private int mSecond;
	private float mDegreeHour;
	private float mDegreeMinute;
	private float mDegreeSecond;
	
	private final static PaintFlagsDrawFilter FILTER = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

	public Clock1x1BitmapDrawable(Bitmap bitmapPanel, Bitmap bitmapHour, Bitmap bitmapMinute, Bitmap bitmapSecond) {
		super(null);
		
		this.mBitmapPanel = bitmapPanel;
		this.mBitmapHour = bitmapHour;
		this.mBitmapMinute = bitmapMinute;
		this.mBitmapSecond = bitmapSecond;
		
		this.mPaint = new Paint(PAINT);
		
		this.mNow = new Time();
		this.mHour = this.mMinute = this.mSecond = 0;
		this.mDegreeHour = this.mDegreeMinute = this.mDegreeSecond = 0.0f;
	}
	
	public void setHostView(View hostView) {
		this.mHostView = hostView;
	}
	
	public void setIconMetrics(IconMetrics iconMatrics) {
		this.mIconMetrics = iconMatrics;
	}
	
	public void setAdapterScale(float adapterScale) {
		XLog.d(TAG,"setAdapterScale " + adapterScale);
		this.mAdapterScale = adapterScale;
	}
	
	public void refresh() {
		if (mHostView != null) {
			mNow.setToNow();
			if (this.mHour != mNow.hour || this.mMinute != mNow.minute || this.mSecond != mNow.second) {
				this.mHour = mNow.hour;
				this.mMinute = mNow.minute;
				this.mSecond = mNow.second;
				this.mDegreeHour = this.mHour * 30.0f + this.mMinute / 2.0f; 
				this.mDegreeMinute = this.mMinute * 6.0f + this.mSecond / 10.0f;
				this.mDegreeSecond = this.mSecond * 6.0f;
				this.mHostView.requestLayout();
				this.mHostView.invalidate();
			}
		}
	}
	
	@Override
	public void draw(Canvas canvas) {
		XLog.d(TAG,"Clock1x1BitmapDrawable DRAW");
		
		int restoreCount = canvas.save();
		
		// 绘制表盘
		if ( mBitmapPanel != null ) {
			drawBitmapRotation(mBitmapPanel, 0.0f, canvas, mPaint);
		}
		// 绘制时针
		if ( mBitmapHour != null ){
			drawBitmapRotation(mBitmapHour, mDegreeHour, canvas, mPaint);
		}
		// 绘制分针
		if ( mBitmapMinute != null ){
			drawBitmapRotation(mBitmapMinute, mDegreeMinute, canvas, mPaint);
		}
		// 绘制秒针
		if ( mBitmapSecond != null){
			drawBitmapRotation(mBitmapSecond, mDegreeSecond, canvas, mPaint);
		}
		
		canvas.restoreToCount(restoreCount);
	}
	
	// 旋转角度为0-360；
	void drawBitmapRotation(Bitmap bm, float nDegree, Canvas canvas, Paint paint) {
		if ( nDegree > 180 )
			nDegree = nDegree - 360;
		
		Matrix matrix = new Matrix();
		matrix.setRotate(nDegree, bm.getWidth() / 2.0f, bm.getHeight() / 2.0f);
		
		//modified by snsermail@gmail.com 2015-08-04 11:05修改拟物时钟widget过小的bug
		matrix.postScale(mAdapterScale, mAdapterScale);
		
		canvas.setDrawFilter(FILTER); //设置去锯齿
		canvas.drawBitmap(bm, matrix, paint);
	}
	
}
