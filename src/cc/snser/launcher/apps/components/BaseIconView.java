package cc.snser.launcher.apps.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.BoringLayout;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.TypedValue;
import android.view.View;
import cc.snser.launcher.ui.utils.IconMetrics;

/**
 * 一个简单实现的IconView，代替原有的TextView的实现
 *
 * TODO: 初始化读取属性，invalidate()和requestLayout()的处理，TextView其他必要接口的实现
 * */
public class BaseIconView extends View {

    private Drawable mIcon;
    private CharSequence mText;
    private int mDrawablePadding;
    private Layout mLayout;
    private TextPaint mTextPaint;
    private int mTextWidth = Integer.MAX_VALUE / 2;
    private int mTextSpace = Integer.MAX_VALUE;
    private int mTextPadding;
    private int mTextOffset;
    private boolean isSingleLine = true;
    private boolean isDrawIcon = true;
    private boolean isDrawText = true;
    private TruncateAt mEllipsize = TruncateAt.END;
    private int mReserveLength = 30;
    private BoringLayout mSavedLayout;
    private BoringLayout.Metrics mBoring;
    private int mMaxLine;

    protected IconMetrics mIconMetrics;

    public BaseIconView(Context context) {
        this(context, null);
    }

    public BaseIconView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseIconView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mIconMetrics = IconMetrics.getInstance(context);
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        
//        TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.TextView, defStyle, 0);
//
//        mDrawablePadding = a.getDimensionPixelSize(com.android.internal.R.styleable.TextView_drawablePadding, 0);
//
//        int textSize = a.getDimensionPixelSize(com.android.internal.R.styleable.TextView_textSize, -1);
//        if (textSize > 0) {
//            mTextPaint.setTextSize(textSize);
//        }
//
//        int textColor = a.getColor(com.android.internal.R.styleable.TextView_textColor, 0xffffffff);
//        mTextPaint.setColor(textColor);
//
//        a.recycle();
    }

    public void setIcon(Drawable icon) {
        if (icon != mIcon) {
            mIcon = icon;
            if (mIcon != null) {
                mIcon.setBounds(0, 0, mIconMetrics.iconWidth, mIconMetrics.iconHeight);
            }
            requestLayout();
            invalidate();
        }
    }

    public Drawable getIcon() {
        return mIcon;
    }

    // TextView interface
    public void setText(int res) {
        setText(getResources().getString(res));
    }

    // 有点猥琐，为了保证布局，当text为空的时候，强制指定个" "
    public void setText(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
        	if(mText == null || (mText != null && !mText.equals(" "))){
        		mText = " ";
            	updateTextLayout();
        	}
            return;
        }
        
        if(mText != null && text != null && mText.equals(text)){
        	return;
        }
        
        mText = text;
        updateTextLayout();
    }

    // TextView interface
    public CharSequence getText() {
        return mText;
    }

    // TextView interface
    public void setCompoundDrawablePadding(int padding) {
        if (padding == mDrawablePadding) {
            return;
        }

        mDrawablePadding = padding;

        if (mLayout != null) {
            requestLayout();
            invalidate();
        }
    }

    private void updateTextLayout() {
        updateTextLayout(true);
    }

    private void updateTextLayout(boolean relayout) {
        if (mLayout instanceof BoringLayout) {
            mSavedLayout = (BoringLayout) mLayout;
        }

        mTextOffset = 0;

        if (mText == null || mText.length() == 0) {
            mLayout = null;
        } else {
            final int textWidth = getTextAreaWidth();
            Layout l;
            BoringLayout.Metrics bm = BoringLayout.isBoring(mText, mTextPaint, mBoring);
            if (bm != null) {
                mBoring = bm;
                if (bm.width <= textWidth) {
                    l = mSavedLayout == null ?
                            BoringLayout.make(mText, mTextPaint, textWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, bm, true) :
                                mSavedLayout.replaceOrMake(mText, mTextPaint, textWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, bm, true);
                } else if (isSingleLine) {
                    if (mEllipsize == null) {
                        l = mSavedLayout == null ?
                                BoringLayout.make(mText, mTextPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, bm, true) :
                                    mSavedLayout.replaceOrMake(mText, mTextPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, bm, true);
                    } else {
                        l = mSavedLayout == null ?
                                BoringLayout.make(mText, mTextPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, bm, true, mEllipsize, textWidth) :
                                    mSavedLayout.replaceOrMake(mText, mTextPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, bm, true, mEllipsize, textWidth);
                    }
                } else {
                    l = mEllipsize == null ?
                            new StaticLayout(mText, mTextPaint, textWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, true) :
                                new StaticLayout(mText, 0, mText.length(), mTextPaint, textWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, true, mEllipsize, textWidth);
                }
            } else {
                String workingText = mText.toString();
                final int want = isSingleLine ? (int) Math.max(textWidth, Layout.getDesiredWidth(mText, mTextPaint)) : textWidth;
                Layout layout = createWorkingLayout(workingText, want);
                int maxLines = isSingleLine ? 1 : 2;
                if (layout.getLineCount() > maxLines || want > textWidth) {
                    while (workingText.length() > 0
                            && (createWorkingLayout(workingText + "...", want).getLineCount() > maxLines 
                            		|| Layout.getDesiredWidth(workingText + "...", mTextPaint) > textWidth * maxLines)) {
                        workingText = workingText.substring(0, workingText.length() - 1);
                    }
                    workingText = workingText + "...";
                }

                l = createWorkingLayout(workingText, want);
                int left, right;
                try {
                    left = (int) FloatMath.floor(l.getLineLeft(0));
                    right = (int) FloatMath.ceil(l.getLineRight(0));
                } catch (Throwable t) {
                    left = 0;
                    right = textWidth;
                }
                if (right - left < textWidth) {
                    mTextOffset = (right + left) / 2 - textWidth / 2;
                } else {
                    if (l.getParagraphDirection(0) < 0) {
                        mTextOffset = right - textWidth;
                    } else {
                        mTextOffset = left;
                    }
                }
            }

            mLayout = l;
        }

        if (relayout) {
            requestLayout();
        }
        invalidate();
    }

    private Layout createWorkingLayout(String workingText, int width) {
        return new StaticLayout(workingText, mTextPaint, width, Layout.Alignment.ALIGN_CENTER,
                1.0f, 0.0f, true);
    }

    /**
     * 设置文字区域的宽度，此宽度与布局宽度无关
     * */
    public void setTextWidth(int width) {
        if (mTextWidth == width) {
            return;
        }

        mTextWidth = width;

        if (mLayout != null) {
            updateTextLayout();
        }
    }

    /**
     * 设置文字区域的左右padding
     * */
    public void setTextExtraPadding(int padding) {
        if (padding == mTextPadding) {
            return;
        }

        mTextPadding = padding;

        if (mLayout != null) {
            updateTextLayout();
        }
    }

    private int getTextAreaWidth() {
        int w = Math.min(mTextSpace, mTextWidth) - 2 * mTextPadding;
        return w < 0 ? 0 : w;
    }

    // TextView interface
    public void setTextSize(int sp) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
    }

    // TextView interface
    public void setTextSize(int unit, float v) {
        final float size = TypedValue.applyDimension(unit, v, getResources().getDisplayMetrics());
        if (size != mTextPaint.getTextSize()) {
            mTextPaint.setTextSize(size);
            invalidate();
        }
    }

    // TextView interface
    public void setShadowLayer(float radius, float dx, float dy, int color) {
        mTextPaint.setShadowLayer(radius, dx, dy, color);
        invalidate();
    }

    // TextView interface
    public void setMaxLines(int lines) {
        setSingleLine(lines == 1);
    }

    // TextView interface
    public void setSingleLine(boolean single) {
        if (single ^ isSingleLine) {
            isSingleLine = single;

            if (mLayout != null) {
                updateTextLayout();
            }
        }
    }

    // TextView interface
    public void setTextColor(int color) {
        if (color == mTextPaint.getColor()) {
            return;
        }

        mTextPaint.setColor(color);
        invalidate();
    }

    // TextView interface
    public void setTypeface(Typeface tf) {
        if (mTextPaint.getTypeface() != tf) {
            mTextPaint.setTypeface(tf);

            if (mLayout != null) {
                updateTextLayout();
            }
        }
    }

    // TextView interface
    /**
     * copy from textview
     * @see android.widget.TextView#setTypeface(Typeface, int)
     * */
    public void setTypeface(Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }

            setTypeface(tf);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSkewX(0);
            setTypeface(tf);
        }
    }

    // TextView interface
    public void setEllipsize(TextUtils.TruncateAt where) {
        // TruncateAt is an enum. != comparison is ok between these singleton objects.
        if (mEllipsize != where) {
            mEllipsize = where;

            if (mLayout != null) {
                updateTextLayout();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        final int width;
        if (widthMode == MeasureSpec.EXACTLY) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        } else {
            int desireWidth = mIconMetrics.iconWidth;
            desireWidth = Math.max(mTextWidth, desireWidth);
            desireWidth += getPaddingLeft() + getPaddingRight();
            width = resolveSize(desireWidth, widthMeasureSpec);
        }

        final int textSpace = width - getPaddingLeft() - getPaddingRight();
        if (textSpace != mTextSpace) {
            mTextSpace = textSpace;
            if (mLayout != null) {
                updateTextLayout(false);
            }
        }

        final int height;
        if (heightMode == MeasureSpec.EXACTLY) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            int desireHeight = (mIcon == null ? 0 : mIconMetrics.iconHeight + mDrawablePadding) + getPaddingTop() + getPaddingBottom();
            if (mLayout != null) {
                //desireHeight += mDrawablePadding;
                int maxLine = isSingleLine ? 1 : 2;
                if (maxLine <= mLayout.getLineCount()) {
                    desireHeight += mLayout.getLineTop(maxLine);
                } else {
                    desireHeight += mLayout.getHeight() + mTextPaint.getFontMetricsInt(null);
                }
            }
            height = resolveSize(desireHeight, heightMeasureSpec);
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int width = getWidth();

        if (isDrawIcon && mIcon != null) {
            final int dl = (width - mIconMetrics.iconWidth) / 2;
            final int dt = getPaddingTop();
            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.translate(dl, dt);
            mIcon.draw(canvas);
            canvas.restore();
        }

        if (isDrawText && mLayout != null) {
            final int textWidth = getTextAreaWidth();

            int tt = getPaddingTop() + (mIcon == null ? 0 : mIconMetrics.iconHeight + mDrawablePadding);
            int tb = tt;
            int maxLine = isSingleLine ? 1 : 2;
            if (maxLine <= mLayout.getLineCount()) {
                tb += mLayout.getLineTop(maxLine);
            } else {
                tb += mLayout.getHeight();
            }
            int tl = (width - textWidth) / 2;
            int tr = (int) ((width + textWidth) / 2f + 0.5f);

            canvas.save();
            canvas.clipRect(tl, tt, tr, tb);
            canvas.translate(tl - mTextOffset, tt);
            mLayout.draw(canvas);
            canvas.restore();
        }
    }

    protected void setDrawText(boolean draw) {
        isDrawText = draw;
    }

    ////////////////
    // ～: force disable fading edges

    @Override
    public final boolean isHorizontalFadingEdgeEnabled() {
        return false;
    }

    @Override
    public final boolean isVerticalFadingEdgeEnabled() {
        return false;
    }

    @Override
    public final int getHorizontalFadingEdgeLength() {
        return 0;
    }

    @Override
    public final int getVerticalFadingEdgeLength() {
        return 0;
    }

    @Override
    protected final float getLeftFadingEdgeStrength() {
        return 0.0f;
    }

    @Override
    protected final float getTopFadingEdgeStrength() {
        return 0.0f;
    }

    @Override
    protected final float getRightFadingEdgeStrength() {
        return 0.0f;
    }

    @Override
    protected final float getBottomFadingEdgeStrength() {
        return 0.0f;
    }

    public int getCompoundPaddingLeft() {
        return getPaddingLeft();
    }

    public int getCompoundPaddingRight() {
        return getPaddingRight();
    }

    @Override
    public int getBaseline() {
        if (mLayout == null) {
            return super.getBaseline();
        }
        return getPaddingTop() + (mIcon == null ? 0 : mIconMetrics.iconHeight + mDrawablePadding) + mLayout.getLineBaseline(0);
    }
}
