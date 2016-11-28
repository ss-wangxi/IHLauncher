package cc.snser.launcher.screens;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.ui.dragdrop.DropTarget;
import cc.snser.launcher.ui.effects.IAnimationCallback;
import cc.snser.launcher.ui.effects.SimpleAnimationListener;
import cc.snser.launcher.ui.effects.TranslateAndAlphaAnimation;

import com.btime.launcher.R;

public class NotifyZone extends FrameLayout  implements DropTarget{
	private TextView mNotifyText;
	private Launcher mLauncher;
	public static final int ANIMATION_DURATION = 400;

	private TranslateAndAlphaAnimation mHiddenAnimation;
	private TranslateAndAlphaAnimation mShowAnimation;

	public NotifyZone(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
	
	private void init() {
		if(mNotifyText != null){ 
			return;
		}
		
		mNotifyText = (TextView)findViewById(R.id.notify_text);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		init();
	}
	
	public void setLauncher(Launcher launcher){
		mLauncher = launcher;
	}

	/**
	 * TODO:以动画的方式显示
	 * @param resId
	 */
	public void showText(int resId) {
		if(mLauncher != null){
			if(mLauncher.getDeleteZone().getVisibility() == View.VISIBLE){
				return;
			}
			
			mLauncher.requestFullScreen(true);
		}

		if(mNotifyText != null){
			mNotifyText.setText("");
			mNotifyText.setText(resId);
		}

		if(this.getVisibility() == View.VISIBLE){
			return;
		}

		clearAnimation();
		mHiddenAnimation = null;

		//setTranslationY(-getHeight());
		setVisibility(View.VISIBLE);

		mShowAnimation = new TranslateAndAlphaAnimation(null, mNewAnimaCallback, false, true, true);

		mShowAnimation.setDuration(ANIMATION_DURATION);
		mShowAnimation.setInterpolator(new AccelerateInterpolator());
		mShowAnimation.setAnimationListener(null);
		mShowAnimation.setAnimationListener(new SimpleAnimationListener(null, mNewAnimaCallback));
		getParent().bringChildToFront(this);
		startAnimation(mShowAnimation);

	}
	
	/*
	 * TODO:动画
	 * */
	public void hide(){
		if(getVisibility() != View.VISIBLE){
			return;
		}

		if(mHiddenAnimation != null ){
			return;
		}

		clearAnimation();
		mShowAnimation = null;
		
		if(mLauncher != null && !mLauncher.getWorkspace().isInEditMode()){
			mLauncher.requestFullScreen(false);
		}

		mHiddenAnimation = new TranslateAndAlphaAnimation(null, mHiddenAnimaCallback, false, false, false);

		mHiddenAnimation.setDuration(ANIMATION_DURATION);
		mHiddenAnimation.setInterpolator(new AccelerateInterpolator());
		mHiddenAnimation.setAnimationListener(null);
		mHiddenAnimation.setAnimationListener(new SimpleAnimationListener(null, mHiddenAnimaCallback));
		startAnimation(mHiddenAnimation);

		//setVisibility(View.GONE);
		
	}

	private IAnimationCallback mHiddenAnimaCallback = new IAnimationCallback() {
		@Override
		public void onTransformation(float interpolatedTime, float dx, float dy) {
		}
		@Override
		public void onComplete(View view) {
			setVisibility(View.GONE);
			mHiddenAnimation = null;
		}
	};

	private IAnimationCallback mNewAnimaCallback = new IAnimationCallback() {
		@Override
		public void onTransformation(float interpolatedTime, float dx, float dy) {
		}
		@Override
		public void onComplete(View view) {
			mShowAnimation = null;
		}
	};

	@Override
	public void onDrop(DragObject dragObject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDragEnter(DragObject dragObject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDragOver(DragObject dragObject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDragExit(DragObject dragObject, DropTarget dropTarget) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean acceptDrop(DragObject dragObject) {
		// TODO Auto-generated method stub
		return false;
	}
}
