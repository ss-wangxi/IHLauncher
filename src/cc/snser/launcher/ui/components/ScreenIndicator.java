
package cc.snser.launcher.ui.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import cc.snser.launcher.Constant;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.screens.Workspace;
import cc.snser.launcher.ui.utils.Utilities;

import com.btime.launcher.R;
import com.shouxinzm.launcher.support.v4.util.ViewUtils;
import com.shouxinzm.launcher.util.ScreenDimensUtils;

/**
 * <p>
 * Represents the view for indicators.
 * </p>
 *
 * @author huangninghai
 * @version 1.0
 */
public class ScreenIndicator extends android.widget.LinearLayout implements View.OnClickListener {
    private static final int DEFAULT_MAX_COUNT = 10;

    public interface OnIndicatorChangedListener {
        public void snapToScreen(int whichScreen);
    }

    private int mMaxCount;
    private int mContainer;
    private int mScreenCount;
    private int mCurrentScreen;
    private int mHomeScreen;
    private OnIndicatorChangedListener mOnClickListener;

    private Drawable mSelectedDrawable;
    private Drawable mUnSelectedDrawable;
    private Drawable mSelectedHomeDrawable;
    private Drawable mUnSelectedHomeDrawable;
    private Drawable mSelectedFolderDrawable;
    private Drawable mUnSelectedFolderDrawable;
    
    private ScreenIndicatorLineView mLineView;
    private SpotScreenIndicatorController mSpotScreenIndicatorController;
    private int mViewPadding = 4;
    
    final static public int FOLDER_INDICATOR = 0x01;
    final static public int NORMAL_INDICATOR = 0x00;
    private Context mContext;

    public ScreenIndicator(Context context) {
        this(context, null);
        mContext = context;
    }

    public ScreenIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mViewPadding = Utilities.dip2px(context, mViewPadding);
        if(context instanceof Launcher)
        {
        }
    }
  
    public void init(int container, int screenCount, int currentScreen, OnIndicatorChangedListener onClickListener) {
        this.mContainer = container;
        this.mHomeScreen = 0;
        this.mScreenCount = screenCount;
        this.mCurrentScreen = currentScreen;
        this.mOnClickListener = onClickListener;
        this.mMaxCount = DEFAULT_MAX_COUNT;

        mSpotScreenIndicatorController = new SpotScreenIndicatorController();

        this.setReverse(false);

        removeAllViews();
        mLineView = null;

        initIndicators(true);

        if (screenCount > this.mMaxCount) {
            addLineView();
        } else {
            mSpotScreenIndicatorController.addToParent();
        }

        snapToScreenDirectly(this.mCurrentScreen);
    }
    
    public void init(int container, Workspace workspace, int screenCount, int currentScreen, OnIndicatorChangedListener onClickListener) {
        this.mContainer = container;
        this.mScreenCount = screenCount;
        this.mCurrentScreen = currentScreen;
        this.mOnClickListener = onClickListener;
        this.mMaxCount = DEFAULT_MAX_COUNT;

        mSpotScreenIndicatorController = new SpotScreenIndicatorController();
        this.setReverse(false);

        removeAllViews();
        mLineView = null;

        initIndicators(true);

        if (screenCount > this.mMaxCount) {
            addLineView();
        } else {
            mSpotScreenIndicatorController.addToParent();
        }

        snapToScreenDirectly(this.mCurrentScreen);
    }
    
    private void addLineView() {
    	
        mLineView = new ScreenIndicatorLineView(mContext, mSelectedHomeDrawable, mUnSelectedHomeDrawable,  mScreenCount, mCurrentScreen, mOnClickListener);
        addView(mLineView, getLineViewLayoutParams());
        updateLineLayoutParams();
    }

    public void reset() {
        this.mContainer = -1;
        this.mCurrentScreen = 0;
        this.mScreenCount = 0;
        this.mOnClickListener = null;

        if (mSpotScreenIndicatorController != null) {
            mSpotScreenIndicatorController.updateCallback();
        }

        this.removeAllViews();

        mSelectedDrawable = null;
        mUnSelectedDrawable = null;
        mSelectedHomeDrawable = null;
        mUnSelectedHomeDrawable = null;
        mSelectedFolderDrawable = null;
        mUnSelectedFolderDrawable = null;
    }

    private Drawable getIndicatorDrawable(boolean isSelected, String resName, int defaultResId) {
        if (resName == null) {
            return getContext().getResources().getDrawable(defaultResId);
        }

        Drawable drawable = null;

        try {
        	drawable = getContext().getResources().getDrawable(defaultResId);
        } catch (OutOfMemoryError e) {
        	// ignore
        }

        return drawable;
    }

    private void initIndicators(boolean resetMaxCount) {
        if (resetMaxCount) {
            initNormalDrawable();
            mMaxCount = mSpotScreenIndicatorController.getMaxCount();

            if (mScreenCount > this.mMaxCount) {
                initLinearDrawable();
            }
        } else {
            if (mScreenCount <= this.mMaxCount) {
                initNormalDrawable();
            } else {
                initLinearDrawable();
            }
        }
       
    }

    private void initIndicators() {
        initIndicators(false);
    }

    private void initNormalDrawable() {
        if (mContainer == Constant.CONTAINER_HOME) {
            
            mSelectedDrawable = getIndicatorDrawable(true, null, R.drawable.default_indicator_current);
            mUnSelectedDrawable = getIndicatorDrawable(false, null, R.drawable.default_indicator);
            mSelectedHomeDrawable = getIndicatorDrawable(true, null, R.drawable.home_indicator_current);
            mUnSelectedHomeDrawable = getIndicatorDrawable(true, null, R.drawable.home_indicator);
        	
        }  else {
            mSelectedDrawable = getIndicatorDrawable(true, null, R.drawable.default_indicator_current);
            mUnSelectedDrawable = getIndicatorDrawable(false, null, R.drawable.default_indicator);
            
            mSelectedHomeDrawable = mSelectedDrawable;
            mUnSelectedHomeDrawable = mUnSelectedDrawable;
            mSelectedFolderDrawable = mSelectedDrawable;
            mUnSelectedFolderDrawable = mUnSelectedDrawable;
        }
       
    }

    private void initLinearDrawable() {
        if (mContainer == Constant.CONTAINER_FOLDER_ADD) {
            mSelectedDrawable = getContext().getResources().getDrawable(R.drawable.indicator_applist_ex_selected);
            mUnSelectedDrawable = getContext().getResources().getDrawable(R.drawable.indicator_applist_ex_bg);
            
            mSelectedHomeDrawable = mSelectedDrawable;
            mUnSelectedHomeDrawable = mUnSelectedDrawable;
            mSelectedFolderDrawable = mSelectedDrawable;
            mUnSelectedFolderDrawable = mUnSelectedDrawable;
        } else {
            mSelectedDrawable = getIndicatorDrawable(true, null, R.drawable.indicator);
            mUnSelectedDrawable = getIndicatorDrawable(false, null, R.drawable.indicator_bg);
            
            mSelectedHomeDrawable = mSelectedDrawable;
            mUnSelectedHomeDrawable = mUnSelectedDrawable;
            mSelectedFolderDrawable = mSelectedDrawable;
            mUnSelectedFolderDrawable = mUnSelectedDrawable;
        }
    }
    
    public void addScreen(int mode) {
        if (mode != mCurrentScene) {
            return;
        }

        addScreen(-1, mode, 0);
    }

    public void addScreen(int index, int mode, int flag) {
        if (mode != mCurrentScene) {
            return;
        }

        mScreenCount++;
        if (mScreenCount == this.mMaxCount + 1) { // change dot mode to line
            removeAllViews();
            initIndicators();
            addLineView();
        } else if (mScreenCount > this.mMaxCount + 1) { // refresh line mode
            updateLineLayoutParams();
        } else { // refresh dot mode
            mSpotScreenIndicatorController.addScreen(index, flag);
        }

        if (index >= 0 && this.mCurrentScreen >= index) {
            this.mCurrentScreen++;
            snapToScreenDirectly(this.mCurrentScreen);
        } else if (mScreenCount == this.mMaxCount + 1) {
            snapToScreenDirectly(this.mCurrentScreen);
        }
    }
    
    public void setHomeScreen(int homeScreen){
    	mHomeScreen = homeScreen;
    	if (mScreenCount <= this.mMaxCount) {
    		mSpotScreenIndicatorController.update();
		}else{
			updateLineLayoutParams();
		}
    }

    public void removeScreen(int whichScreen) {
        removeScreen(mCurrentScene, whichScreen);
    }
    
    public void clear(){
    	removeAllViews();
    	mScreenCount = 0;
    	initIndicators();
    	
    	
    }

    public void removeScreen(int mode, int whichScreen) {
        if (mode != mCurrentScene) {
            return;
        }

        mScreenCount--;
        if (mScreenCount == this.mMaxCount) { // change line mode to dot mode
            removeAllViews();
            mLineView = null;
            initIndicators();

            mSpotScreenIndicatorController.addToParent();
        } else if (mScreenCount > this.mMaxCount) { // refresh line mode
            updateLineLayoutParams();
        } else { // refresh dot mode
            if (whichScreen < 0 || whichScreen > mScreenCount) {
                return;
            }
            mSpotScreenIndicatorController.removeScreen(whichScreen);
            //Remove the home indicator and reset the next view as home
            //if(whichScreen == mHomeScreen){
            	//ImageView childView = (ImageView) getChildAt(0);
            	//childView.setImageDrawable(new ScreenIndicatorDrawableWrapper(mSelectedHomeDrawable, mUnSelectedHomeDrawable));
				//childView.invalidate();
            //}
        }

        if (whichScreen <= mCurrentScreen) {
            if (mCurrentScreen > 0) {
                snapToScreen(mCurrentScreen - 1);
            } else {
                snapToScreenDirectly(mCurrentScreen);
            }
        } else if (mScreenCount == this.mMaxCount) {
            snapToScreenDirectly(this.mCurrentScreen);
        }
    }

    private void updateLineLayoutParams() {
        if (mScreenCount > mMaxCount) {
            if (mLineView != null) {
                mLineView.updateScreens(mScreenCount, mCurrentScreen, 0);
                mLineView.setLayoutParams(getLineViewLayoutParams());
            }
            return;
        }
    }

    private ViewGroup.LayoutParams getLineViewLayoutParams() {
        int width = 0;
        int height = 0;

        width = ViewGroup.LayoutParams.MATCH_PARENT;
        height = mSelectedDrawable.getIntrinsicHeight();

        ViewGroup.LayoutParams params = mLineView.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(width, height);
        } else {
            params.width = width;
            params.height = height;
        }
        
        return params;
    }

    private void setImageResource(ImageView childView, int index, boolean selected) {
        if (mScreenCount > this.mMaxCount) {
            return;
        }

        boolean odlSelected = childView.isSelected();
        if (odlSelected != selected) {
            childView.setSelected(selected);

            if (Workspace.sInEditMode) { // for bug 276515
                ((View) getParent()).invalidate();
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (mScreenCount > mMaxCount) {
            return;
        }

        int whichScreen = this.indexOfChild(view);

        if (mCurrentScreen != whichScreen && mOnClickListener != null) {
            mOnClickListener.snapToScreen(whichScreen);
        }
    }

    public void snapToScreenWithTouching(int whichScreen) {
        if (mLineView == null && mSpotScreenIndicatorController.mCurrentMode == SpotScreenIndicatorController.INDICATOR_MODE_WATER) {
            return;
        }
        snapToScreen(whichScreen);
    }

    public void snapToScreen(int whichScreen) {
        if (whichScreen < 0 || whichScreen >= mScreenCount) {
            return;
        }

        if (mCurrentScreen < 0 || mCurrentScreen >= mScreenCount) {
            snapToScreenDirectly(whichScreen);
            return;
        }

        if (mCurrentScreen != whichScreen) {
            snapToScreenDirectly(whichScreen);
        }
    }

    public void snapToScreenDirectly(int whichScreen) {
        if (mScreenCount > mMaxCount) {
            if (mLineView != null) {
                mLineView.updateScreens(mScreenCount, whichScreen, 0);
                mLineView.invalidate();
            }
        } else {
            mSpotScreenIndicatorController.snapToScreen(whichScreen);
        }

        mCurrentScreen = whichScreen;

        invalidate();
    }

    public boolean canSnapToPosition() {
        return mScreenCount <= mMaxCount && mSpotScreenIndicatorController.mCurrentMode == SpotScreenIndicatorController.INDICATOR_MODE_WATER || mScreenCount > mMaxCount && mLineView != null;
    }

    public void snapToPosition(float scrollX, int currentScreen, boolean invalidate) {
        if (mScreenCount > mMaxCount && mLineView != null) {
            mCurrentScreen = currentScreen;

            if (mLineView.updateScreens(mScreenCount, mCurrentScreen, scrollX) && invalidate && ViewUtils.isHardwareAccelerated(this)) {
                mLineView.invalidate();
            }
        } else if (mScreenCount <= mMaxCount && mSpotScreenIndicatorController != null && mSpotScreenIndicatorController.mCurrentMode == SpotScreenIndicatorController.INDICATOR_MODE_WATER) {
            mCurrentScreen = currentScreen;
            mSpotScreenIndicatorController.updateRatio(scrollX);
        }
    }

    private boolean mReverse;

    public void setReverse(boolean reverse) {
        mReverse = reverse;
    }

    public boolean isReverse() {
        return mReverse;
    }

    @Override
    public View getChildAt(int index) {
        if (mScreenCount > mMaxCount) {
            return super.getChildAt(0);
        }

        if (mReverse) {
            return super.getChildAt(this.getChildCount() - 1 - index);
        } else {
            return super.getChildAt(index);
        }
    }

    @Override
    public void removeViewAt(int index) {
        if (mScreenCount > mMaxCount) {
            super.removeViewAt(0);
            return;
        }

        if (mReverse) {
            super.removeViewAt(this.getChildCount() - 1 - index);
        } else {
            super.removeViewAt(index);
        }
    }
    
    public void forceUpdateIndicator(int whichScreen){
    	if (this.mScreenCount > this.mMaxCount) {
    		//<bug7960> 屏幕数较多时，Indicator是ScreenIndicatorLineView，不能forceUpdateIndicator
    		return;
    	}
    	if(mSpotScreenIndicatorController != null){
    		mSpotScreenIndicatorController.forceUpdateIndicator(whichScreen);
    	}
    }

    public static final int INDICATOR_SCENE_HOME = 0;

    private int mCurrentScene = INDICATOR_SCENE_HOME;

    private class SpotScreenIndicatorController {
        private static final int INDICATOR_MODE_NORMAL = 0;
        private static final int INDICATOR_MODE_WATER = 1;
        private int mCurrentMode = 0;
        private ScreenIndicatorWaterView mWaterView;

        public SpotScreenIndicatorController() {
        }

        public int getMaxCount() {
            initMode();
            if (mWaterView != null) {
                return mWaterView.getMaxScreenCount(ScreenDimensUtils.getScreenWidth(mContext));
            } else {
                return ScreenDimensUtils.getScreenWidth(mContext) / (mSelectedDrawable.getIntrinsicWidth() + 2 * (mViewPadding + getExtraPadding()));
            }
        }

        public void addToParent() {
            if (mCurrentMode == INDICATOR_MODE_WATER) {
                mWaterView.setScreen(mScreenCount, mCurrentScreen);
                if (mWaterView.getParent() == null) {
                    addView(mWaterView);
                }
            } else {
                for (int i = 0; i < mScreenCount; i++) {
                	//if(mLauncher != null && mLauncher.getModel().isFolderScreen(i))
        			//{
                		//addNormalImageView(i, FOLDER_INDICATOR);
        			//}
        			//else
        			//{
        				addNormalImageView(i, NORMAL_INDICATOR);
        			//}
                    
                }
            }
        }

        public void update() {
            int oldMode = mCurrentMode;
            initMode();

            if (mCurrentMode != oldMode) {
                ScreenIndicator.this.removeAllViews();
                addToParent();
            } else {
                if (oldMode == INDICATOR_MODE_WATER) {
                    if (mWaterView.getParent() == null || mWaterView.getParent() != ScreenIndicator.this) {
                        ScreenIndicator.this.removeAllViews();
                        addToParent();
                    } else {
                        mWaterView.setScreen(mScreenCount, mCurrentScreen);
                    }
                } else {
                    if (mWaterView != null && mWaterView.getParent() == ScreenIndicator.this) {
                        ScreenIndicator.this.removeAllViews();
                        addToParent();
                    } else if (ScreenIndicator.this.getChildAt(0) != null && ScreenIndicator.this.getChildAt(0) instanceof ScreenIndicatorWaterView || ScreenIndicator.this.getChildCount() != mScreenCount) {
                        ScreenIndicator.this.removeAllViews();
                        addToParent();
                    } else {
                        int count = getChildCount();
                        for (int i = 0; i < mScreenCount; i++) {
                            if (i < 0) {
                                i = count;
                            }
                            ImageView childView = (ImageView) getChildAt(i);
                            
                            if(i == mHomeScreen)
                            	childView.setImageDrawable(new ScreenIndicatorDrawableWrapper(mSelectedHomeDrawable, mUnSelectedHomeDrawable));
                            else
                            	childView.setImageDrawable(new ScreenIndicatorDrawableWrapper(mSelectedDrawable, mUnSelectedDrawable));
                            
                        }

                    }
                }
            }
        }

        public void addScreen(int index, int flag) {
            if (mCurrentMode == INDICATOR_MODE_NORMAL) {
                addNormalImageView(index, flag);
            } else {
                mWaterView.setScreen(mScreenCount, mCurrentScreen);
            }
        }
        public void removeScreen(int index) {
            if (mCurrentMode == INDICATOR_MODE_NORMAL) {
                removeViewAt(index);
            } else {
                mWaterView.setScreen(mScreenCount, mCurrentScreen);
            }
        }
        
        public void forceUpdateIndicator(int whichScreen){
        	for (int i = 0; i < mScreenCount; i++) {
        		final View childView = getChildAt(i);
        		if (childView instanceof ImageView) {
        			setImageResource((ImageView)childView, i, ScreenIndicator.this.isReverse() ? (mScreenCount - i - 1 == whichScreen) : (i == whichScreen));
        		}
            }
        }

        public void snapToScreen(int whichScreen) {
            if (mCurrentMode == INDICATOR_MODE_WATER) {
                mWaterView.setScreen(mScreenCount, whichScreen);
            } else {
                for (int i = 0; i < mScreenCount; i++) {
                    final ImageView childView = (ImageView) getChildAt(i);
                    setImageResource(childView, i, ScreenIndicator.this.isReverse() ? (mScreenCount - i - 1 == whichScreen) : (i == whichScreen));
                }
            }
        }

        public void updateRatio(float ratio) {
            if (mCurrentMode == INDICATOR_MODE_WATER && mWaterView != null) {
                mWaterView.updateRatio(mCurrentScreen, ratio);
            }
        }

        public void updateCallback() {
            if (mWaterView != null) {
                mWaterView.setOnClickCallback(mOnClickListener);
            }
        }

        private void initMode() {
            mCurrentMode = (isWaterIndicatorUsable() ? INDICATOR_MODE_WATER : INDICATOR_MODE_NORMAL);
            if (mCurrentMode == INDICATOR_MODE_WATER && mWaterView == null) {
                mWaterView = new ScreenIndicatorWaterView(mContext, Color.WHITE);
                mWaterView.setOnClickCallback(mOnClickListener);
            } else if (mCurrentMode == INDICATOR_MODE_NORMAL && mWaterView != null) {
                if (mWaterView.getParent() != null) {
                    ((ViewGroup) mWaterView.getParent()).removeView(mWaterView);
                }
                mWaterView = null;
            }
        }

        private void addNormalImageView(int index, int flag) {
        	int count = getChildCount();
            if (index < 0) {
                index = count;
            }
            ImageView childView = (ImageView) inflate(mContext, R.layout.screen_indicator_item, null);
            int padding = getExtraPadding();
            if (padding != 0) {
                childView.setPadding(padding, padding, padding, padding);
            }
           
            addView(childView, index, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            if(index == mHomeScreen)
            	childView.setImageDrawable(new ScreenIndicatorDrawableWrapper(mSelectedHomeDrawable, mUnSelectedHomeDrawable));
            else if(flag == FOLDER_INDICATOR)
            	childView.setImageDrawable(new ScreenIndicatorDrawableWrapper(mSelectedFolderDrawable, mUnSelectedFolderDrawable));
            else
            	childView.setImageDrawable(new ScreenIndicatorDrawableWrapper(mSelectedDrawable, mUnSelectedDrawable));
            
            childView.setPadding(mViewPadding, 0, mViewPadding, 0);
            childView.setOnClickListener(ScreenIndicator.this);
        }

        private boolean isWaterIndicatorUsable() {
            //return (mContainer == Constant.CONTAINER_HOME || mContainer == Constant.CONTAINER_CLOCKWEATHER/* || mContainer == Constant.CONTAINER_THEME*/) && Theme.isUsingBuiltin(mContext);
        	return false;
        }
    }

    private int getExtraPadding() {
        if (mContainer == Constant.CONTAINER_FOLDER_ADD || mContainer == Constant.CONTAINER_THEME) {
            return Utilities.dip2px(mContext, 3.5f);
        }
        return 0;
    }
}
