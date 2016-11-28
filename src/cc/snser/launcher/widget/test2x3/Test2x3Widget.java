package cc.snser.launcher.widget.test2x3;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import cc.snser.launcher.widget.ScreenCtrlWidget;

import com.btime.launcher.R;

public class Test2x3Widget extends ScreenCtrlWidget {
    
    public static final int SPANX = 2;
    public static final int SPANY = 3;
    
    private Test2x3View mRootView;

    public Test2x3Widget(Activity context) {
        super(context);
        
        setGravity(Gravity.CENTER);
        
        mRootView = (Test2x3View)inflate(context, R.layout.widget_test2x3_view, null);
        mRootView.setHost(this);
        addView(mRootView);
        
        mRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    @Override
    public int getSpanX() {
        return SPANX;
    }
    
    @Override
    public int getSpanY() {
        return SPANY;
    }

}
