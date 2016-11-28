package cc.snser.launcher.ui.view;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.TextView;

@SuppressLint("HandlerLeak")
public class AutoScrollTextView extends TextView{
	private int mDuration; //文字从出现到显示消失的时间
    private List<String> mTexts; //显示文字的数据源
    private int mX = 0; //文字的X坐标
    private int mIndex = 0; //当前的数据下标
    private Paint mPaintBack; //绘制内容的画笔
    private boolean hasInit = false;//是否初始化刚进入时候文字的纵坐标
    private boolean mIsEmpty = true;
    private Rect indexBound = new Rect();
    @SuppressLint("HandlerLeak")
    private final  Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what==1){
            	invalidate();
            }
        }
    };
    public AutoScrollTextView(Context context) {
        this(context, null);
        init();
    }
    public AutoScrollTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }   
    //设置数据源
    public void setmTexts(List<String> mTexts) {
        this.mTexts = mTexts;
        postInvalidate();//通知重绘
    }
    
    //设置文字从出现到消失的时长
    public void setmDuration(int mDuration) {
        this.mDuration = mDuration;
    }
    public void setBackColor(int mBackColor) {
        mPaintBack.setColor(mBackColor);
    }
    public void setTextcontent(boolean mIsEmpty) {
		this.mIsEmpty = mIsEmpty;
		postInvalidate();//通知重绘
	}
    //初始化默认值
    private void init() {
        mDuration = 5000;
        mIndex = 0;
        mPaintBack = new Paint();
        mPaintBack.setAntiAlias(true);
        mPaintBack.setDither(true);
        mPaintBack.setTextSize(22);
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }
    
	@Override
    protected void onDraw(Canvas canvas) { 	
    	if(mIsEmpty == false){
    		if (mTexts != null) {
                String back = mTexts.get(mIndex);             
                mPaintBack.getTextBounds(back, 0, back.length(), indexBound);           
                if (mX == 0 && hasInit == false) {
                    mX = getMeasuredWidth() - indexBound.left;
                    hasInit = true;
                }
                //移动到最左边
                if (mX <= 0-indexBound.right) {
                    mX = getMeasuredWidth()- indexBound.left;
                    mIndex++;
                }                
                canvas.drawText(back, 0, back.length(),  mX,indexBound.bottom-indexBound.top , mPaintBack);
                mX -= 2;
                if (mIndex == mTexts.size()) {
                    mIndex = 0;
                }
                mHandler.sendEmptyMessage(1);
            }
    	} else{
        	super.onDraw(canvas);
        }
    }
}