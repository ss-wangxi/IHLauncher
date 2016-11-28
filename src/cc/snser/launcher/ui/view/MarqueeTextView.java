package cc.snser.launcher.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class MarqueeTextView extends TextView {
	
	private boolean mIsFocused = true;
    public MarqueeTextView(Context context) {
        super(context);
    }

    public MarqueeTextView(Context paramContext, AttributeSet attributeSet) {
        super(paramContext, attributeSet);
    }

    public MarqueeTextView(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
    }

    public boolean isFocused() {
        return mIsFocused;
    }

	public void setFocusable(boolean mIsFocused) {
		this.mIsFocused = mIsFocused;
	}
}
