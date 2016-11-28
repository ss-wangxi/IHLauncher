package cc.snser.launcher.ui.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import cc.snser.launcher.Launcher;
import cc.snser.launcher.LauncherSettings;
import cc.snser.launcher.util.ReflectionUtils;
import cc.snser.launcher.util.ResourceUtils;

import com.btime.launcher.util.XLog;
import com.shouxinzm.launcher.util.BitmapUtils;
import com.shouxinzm.launcher.util.ScreenDimensUtils;

import java.lang.reflect.Field;

import static cc.snser.launcher.Constant.DEBUG;

public class ShadingBackgroundView extends View {
    private final Launcher mLauncher;

    private float radio;
    private float defRadio;
    private Paint paint = new Paint();
    private Paint defPaint = new Paint();
    //用于设置背景的图
    private BitmapDrawable mBgBd;
    //是否开启mask
    private boolean isEnableMask = false;

    private Drawable mBackground;

    public ShadingBackgroundView(Launcher launcher) {
        this(launcher, 0.7f);
    }

    public ShadingBackgroundView(Launcher launcher, float radio) {
        super(launcher);

        this.radio = radio;
        paint.setColor(0xff000000);
        paint.setAlpha((int) (255 * radio));

        this.defRadio = radio;
        defPaint.setColor(0xff000000);
        defPaint.setAlpha((int) (255 * radio));

        this.mLauncher = launcher;
    }

    @Override
    protected boolean onSetAlpha(int alpha) {
        if (hasBgBd()) {
            paint.setAlpha((int) (alpha * radio));
        } else {
            defPaint.setAlpha((int) (alpha * defRadio));
        }
        if (hasBgBd()) {
            mBgBd.setAlpha(alpha);
        }
        if (mBackground != null) {
            mBackground.setAlpha(alpha);
            if (mBackground instanceof GradientDrawable) {
                Field field = ReflectionUtils.getDeclaredField(GradientDrawable.class, "mRectIsDirty");
                if (field != null) {
                    ReflectionUtils.invokeField(mBackground, field, true);
                }
            }
        }
        return true;
    }

	@Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!hasBgBd()) {
            canvas.drawPaint(defPaint);
        } else {
            int yOffset = (LauncherSettings.isEnableStatusBarAutoTransparent() || LauncherSettings.isEnableStatusBarAutoTransparentV2()) ? 0 : (ScreenDimensUtils.isFullScreen(mLauncher) ? 0 : ResourceUtils.getStatusBarHeight(mLauncher));

            mBgBd.setBounds(0, -yOffset, getWidth(), -yOffset + ScreenDimensUtils.getScreenRealHeight(mLauncher));
        	mBgBd.draw(canvas);

            if (isEnableMask) {
                canvas.drawPaint(paint);
            }
        }

        if (mBackground != null) {
            mBackground.setBounds(0, 0, getWidth(), getHeight());
            mBackground.draw(canvas);
        }
    }

	/**
	 * 设置用于平铺的图像
	 * @param bm
	 */
    public void setBgBm(Bitmap bm) {
        this.mBgBd = new BitmapDrawable(getResources(), bm);
        this.mBgBd.setBounds(0, 0, getWidth(), getHeight());
        /*paint.setColor(0xff000000);
        radio = 0.6f;*/
    }

    public Bitmap getBgBm() {
        return this.mBgBd == null ? null : this.mBgBd.getBitmap();
    }

    public void setBg(Drawable background) {
        this.mBackground = background;
    }

    /**
     * 设置一个蒙层mask
     * 若设置值合法(!=-1)，则会添加一层mask
     * @param maskColor mask颜色
     * @param maskAlpha mask透明度
     */
    public void setMaskColorAndAlpha(int maskColor, float maskAlpha) {
        if(maskColor != -1) {
            isEnableMask = true;
        } else {
            isEnableMask = false;
        }
        this.paint.setColor(maskColor);
        this.paint.setAlpha((int) (255 * maskAlpha));
        this.radio = maskAlpha;
    }

    /*public void setMaskColor(int maskColor) {
        this.paint.setColor(maskColor);
    }

    public void setMaskAlpha(float maskAlpha) {
        this.radio = maskAlpha;
    }*/

    @Override
    protected void onAttachedToWindow() {
    	super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
    	super.onDetachedFromWindow();
        if (DEBUG) {
            XLog.d("ShadingBackgroundView", "call onDetachedFromWindow");
        }
        if (mBgBd != null) {
            BitmapUtils.recycleBitmap(mBgBd.getBitmap());
            mBgBd = null;
        }
        if (mBackground != null) {
            mBackground = null;
        }
    }

    private boolean hasBgBd() {
        if (mBgBd == null || mBgBd.getBitmap() == null || mBgBd.getBitmap().isRecycled()) {
            return false;
        }
        return true;
    }
}
