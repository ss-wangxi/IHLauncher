package cc.snser.launcher.widget.traffic2x3;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class Traffic2x3View extends RelativeLayout {
    
    public Traffic2x3View(Context context) {
        super(context);
    }
    
    public Traffic2x3View(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public Traffic2x3View(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setHost(Traffic2x3Widget host) {
    }
    
    public void setTrafficFree(String strFree) {
        
    }
    
    public void setTrafficTotal(String strTotal) {
        
    }

}
