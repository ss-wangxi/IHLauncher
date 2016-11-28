package cc.snser.launcher.widget.traffic2x3;

import com.btime.launcher.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

public class SuperProgressWheel extends View {
	
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Shader mShader;
    private RectF mRect = new RectF();
      
    private float mTotalArcAngle = 280.0f; //显示的总角度
    
    private int mMainStepCount; //主环进度格子个数
    private float mMainStepBlankAngleRate; //主环进度格子和空隙格子的角度比
    private int mMainArcEdgeRadius; //主环边缘半径
    private int mMainArcWidth; //主环宽度
    private int mMainArcBackgroundColor; //主环背景色
    private int mMainArcStartColor; //主环前景渐变起始色
    private int mMainArcEndColor; //主环前景渐变终止色
    
    private int mOuterArcEdgeRadius; //外环边缘半径
    private int mOuterArcWidth; //外环宽度
    private int mOuterArcColor; //外环颜色
  
    private int mInnerStepCount; //内环进度格子个数
    private float mInnerStepBlankAngleRate; //内环进度格子和空隙格子的角度比
    private int mInnerArcEdgeRadius; //内环边缘半径
    private int mInnerArcWidth; //内环宽度
    private int mInnerArcColor; //内环颜色
    
    private int mProgress; //当前进度(0~100)
    
	public SuperProgressWheel(Context context) {
		this(context, null);
	}

	public SuperProgressWheel(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public SuperProgressWheel(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
		TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.SuperProgressWheel);
		
	    mTotalArcAngle = mTypedArray.getFloat(R.styleable.SuperProgressWheel_spwTotalArcAngle, 280.0f);
	    
	    mMainStepCount = mTypedArray.getInteger(R.styleable.SuperProgressWheel_spwMainStepCount, 35);
	    mMainStepBlankAngleRate = mTypedArray.getFloat(R.styleable.SuperProgressWheel_spwMainStepBlankAngleRate, 1.0f);;
	    mMainArcEdgeRadius = mTypedArray.getDimensionPixelSize(R.styleable.SuperProgressWheel_spwMainArcEdgeRadius, dp2px(context, 65));
	    mMainArcWidth = mTypedArray.getDimensionPixelSize(R.styleable.SuperProgressWheel_spwMainArcWidth, dp2px(context, 20));
	    mMainArcBackgroundColor = mTypedArray.getInteger(R.styleable.SuperProgressWheel_spwMainArcBackgroundColor, 0xFFCCCCCC);
	    mMainArcStartColor = mTypedArray.getInteger(R.styleable.SuperProgressWheel_spwMainArcStartColor, 0xFFF7BB43);
	    mMainArcEndColor = mTypedArray.getInteger(R.styleable.SuperProgressWheel_spwMainArcEndColor, 0xFFFF0000);
	    
        mOuterArcEdgeRadius = mTypedArray.getDimensionPixelSize(R.styleable.SuperProgressWheel_spwOuterArcEdgeRadius, dp2px(context, 70));
        mOuterArcWidth = mTypedArray.getDimensionPixelSize(R.styleable.SuperProgressWheel_spwOuterArcWidth, dp2px(context, 2));
        mOuterArcColor = mTypedArray.getInteger(R.styleable.SuperProgressWheel_spwOuterArcColor, 0xFF8080FF);
	    
	    mInnerStepCount = mTypedArray.getInteger(R.styleable.SuperProgressWheel_spwInnerStepCount, 70);
	    mInnerStepBlankAngleRate = mTypedArray.getFloat(R.styleable.SuperProgressWheel_spwInnerStepBlankAngleRate, 0.5f);
	    mInnerArcEdgeRadius = mTypedArray.getDimensionPixelSize(R.styleable.SuperProgressWheel_spwInnerArcEdgeRadius, dp2px(context, 40));
	    mInnerArcWidth = mTypedArray.getDimensionPixelSize(R.styleable.SuperProgressWheel_spwInnerArcWidth, dp2px(context, 2));
	    mInnerArcColor = mTypedArray.getInteger(R.styleable.SuperProgressWheel_spwInnerArcColor, 0xFF8080FF);
	    
	    mProgress = mTypedArray.getInteger(R.styleable.SuperProgressWheel_spwProgress, 85);
		
		mTypedArray.recycle();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
	    if (getWidth() != getHeight()) {
	        super.onDraw(canvas);
	        return;
	    }
	    
		final int canvasRadius = getWidth() / 2;
		final float rotateAngle = 270 - mTotalArcAngle / 2.0f;
		
		mPaint.setStyle(Paint.Style.STROKE);
		
		if (mShader == null) {
		    final float splitPostion = mTotalArcAngle / 360.f;
		    final int colors[] = new int[] {mMainArcStartColor, mMainArcEndColor, Color.TRANSPARENT, Color.TRANSPARENT};
		    final float positions[] = new float[] {0.0f, splitPostion, splitPostion, 1.0f};
		    final Matrix matrix = new Matrix();
		    matrix.setRotate(rotateAngle, canvasRadius, canvasRadius);
		    mShader = new SweepGradient(canvasRadius, canvasRadius, colors, positions);
            mShader.setLocalMatrix(matrix);
		}
		
		//画主圆环背景
		final float mainDrawRadius = mMainArcEdgeRadius - mMainArcWidth / 2.0f;
		final float mainBlankAngleUnit = mTotalArcAngle * 1.0f / (mMainStepCount * mMainStepBlankAngleRate + (mMainStepCount - 1));
		final float mainStepAngleUnit = mainBlankAngleUnit * mMainStepBlankAngleRate;
		mPaint.setStrokeWidth(mMainArcWidth);
		mPaint.setColor(mMainArcBackgroundColor);
		mPaint.setShader(null);
		mRect.set(canvasRadius - mainDrawRadius, 
		          canvasRadius - mainDrawRadius, 
		          canvasRadius + mainDrawRadius, 
		          canvasRadius + mainDrawRadius);
		float startAngle = rotateAngle;
		for (int i = 0 ; i != mMainStepCount; ++i) {
		    canvas.drawArc(mRect, startAngle, mainStepAngleUnit, false, mPaint);
		    startAngle += (mainStepAngleUnit + mainBlankAngleUnit);
		}
		
		//画主圆环前景
		mPaint.setShader(mShader);
        startAngle = rotateAngle;
        final int nMainStep = (int)(mProgress * mMainStepCount / 100.0f + 0.5f);
        for (int i = 0 ; i != nMainStep; ++i) {
            canvas.drawArc(mRect, startAngle, mainStepAngleUnit, false, mPaint);
            startAngle += (mainStepAngleUnit + mainBlankAngleUnit);
        }
        
        //画外侧圆环
        final float outerDrawRadius = mOuterArcEdgeRadius - mOuterArcWidth / 2.0f;
        final float outerAngle = mainStepAngleUnit * nMainStep + mainBlankAngleUnit * (nMainStep - 1);
        mPaint.setStrokeWidth(mOuterArcWidth);
        mPaint.setColor(mOuterArcColor);
        mPaint.setShader(null);
        mRect.set(canvasRadius - outerDrawRadius, 
                  canvasRadius - outerDrawRadius, 
                  canvasRadius + outerDrawRadius, 
                  canvasRadius + outerDrawRadius);
        startAngle = rotateAngle;
        canvas.drawArc(mRect, startAngle, outerAngle, false, mPaint);
        
		//画内侧圆环
        final float innerDrawRadius = mInnerArcEdgeRadius - mInnerArcWidth / 2.0f;
        final float innerBlankAngleUnit = mTotalArcAngle * 1.0f / (mInnerStepCount * mInnerStepBlankAngleRate + (mInnerStepCount - 1));
        final float innerStepAngleUnit = innerBlankAngleUnit * mInnerStepBlankAngleRate;
        mPaint.setStrokeWidth(mInnerArcWidth);
        mPaint.setColor(mInnerArcColor);
        mPaint.setShader(null);
        mRect.set(canvasRadius - innerDrawRadius, 
                  canvasRadius - innerDrawRadius, 
                  canvasRadius + innerDrawRadius, 
                  canvasRadius + innerDrawRadius);
        startAngle = rotateAngle;
        for (int i = 0 ; i != mInnerStepCount; ++i) {
            canvas.drawArc(mRect, startAngle, innerStepAngleUnit, false, mPaint);
            startAngle += (innerStepAngleUnit + innerBlankAngleUnit);
        }
	}
	
	/**
	 * 获取主环总步数
	 * @return 主环总步数
	 */
	public int getMainStepCount() {
	    return mMainStepCount;
	}
	
   /**
     * 获取内环总步数
     * @return 内环总步数
     */
    public int getInnerStepCount() {
        return mInnerStepCount;
    }
	
    /**
     * 获取当前进度
     * @return 当前进度(0~100)
     */
    public int getProgress() {
        return mProgress;
    }
    
    /**
     * 设置当前进度
     * @param progress 当前进度(0~100)
     */
    public void setProgress(int progress) {
        if (progress >= 0 && progress <= 100) {
            mProgress = progress;
            invalidate();
        }
    }
	
    private static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dpValue * scale + 0.5f);
    }

}