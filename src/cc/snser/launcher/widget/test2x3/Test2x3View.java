package cc.snser.launcher.widget.test2x3;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class Test2x3View extends RelativeLayout implements View.OnClickListener {
    
    public Test2x3View(Context context) {
        super(context);
    }
    
    public Test2x3View(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public Test2x3View(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    public void onClick(View v) {
    }
    
    public void setHost(Test2x3Widget host) {
    }
    
}
