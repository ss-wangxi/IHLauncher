package cc.snser.launcher.widget.transparent1x1;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class Transparent1x1View extends RelativeLayout {
    
    public Transparent1x1View(Context context) {
        super(context);
    }
    
    public Transparent1x1View(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public Transparent1x1View(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setHost(Transparent1x1Widget host) {
    }

}
