package cc.snser.launcher.widget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import cc.snser.launcher.App;

import com.btime.launcher.R;

public class UserServiceProtolHelper{
	private WindowManager mWindowManager;
	private WindowManager.LayoutParams mFloatingWndParams;
	private View mFloatingWndLayout;
	private WebView mWebView;
    private CheckBox mCheckBox;
	private Button acceptButton;
	private final static String USER_SERVICE_PROTOCOL_ACTION = "AGREED_USER_SERVICE_PROTOCOL";
	private static boolean isshow ;
	private UserServiceProtolHelper() {
		
    }
    private static class SingletonHolder {
        public static UserServiceProtolHelper sInstance = new UserServiceProtolHelper();
    }
    
    public static UserServiceProtolHelper getInstance() {
        return SingletonHolder.sInstance;
    }
    public boolean showUseService(){
    	return isshow;
    }
    
    public void createWindowManager() {	
    	mWindowManager = (WindowManager) App.getApp().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
		mFloatingWndParams = new WindowManager.LayoutParams();
		mFloatingWndParams.type = LayoutParams.TYPE_KEYGUARD_DIALOG; 
		mFloatingWndParams.format = PixelFormat.RGBA_8888; 
		mFloatingWndParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE; 
		mFloatingWndParams.gravity = Gravity.START | Gravity.TOP; 
		mFloatingWndParams.width = WindowManager.LayoutParams.MATCH_PARENT;
		mFloatingWndParams.height = WindowManager.LayoutParams.MATCH_PARENT;
		mFloatingWndParams.x = 1920;
		mFloatingWndParams.y = 480;
		LayoutInflater inflater = LayoutInflater.from(App.getApp().getApplicationContext());
		mFloatingWndLayout =  inflater.inflate(R.layout.user_service_protocal, null);
//		mFloatingWndLayout.setVisibility(View.INVISIBLE);
		mFloatingWndLayout.getBackground().setAlpha(0);
		mWebView = (WebView) mFloatingWndLayout.findViewById(R.id.user_service_protocal_webview);
		mCheckBox = (CheckBox) mFloatingWndLayout.findViewById(R.id.choose_user_service);
		acceptButton = (Button) mFloatingWndLayout.findViewById(R.id.accept_user_service);
	}
    public void showDesk() {
    	if(!isshow){
    		mWindowManager.addView(mFloatingWndLayout, mFloatingWndParams);	
    		isshow = true;
    	}
		mWebView.getSettings().setJavaScriptEnabled(false);
		mWebView.getSettings().setSupportZoom(false);
		mWebView.getSettings().setBuiltInZoomControls(false);
		mWebView.getSettings().setDefaultFontSize(30);
		mWebView.setBackgroundColor(0);
		mWebView.setWebViewClient(new WebViewClient() {
		    @Override
		    public void onPageFinished(WebView view, String url) {
//		    	mWebView.loadUrl("javascript:document.body.style.padding=\"8%\"; void 0");//webview设置左右padding
//		    	mFloatingWndLayout.setVisibility(View.VISIBLE);
		    	mFloatingWndLayout.getBackground().setAlpha(255);
		    }
		});
		mWebView.loadUrl("file:///android_asset/userprotocol.html"); 
		mWebView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
		mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					acceptButton.setEnabled(true);
					acceptButton.setBackgroundResource(R.drawable.settings_button_border1);
				}else{
					acceptButton.setEnabled(false);
					acceptButton.setBackgroundResource(R.drawable.settings_button_border);
				}
				
			}
		});
		acceptButton.setOnClickListener(new OnClickListener() {	 
			@Override
			public void onClick(View v) {				
				Intent intent = new Intent(USER_SERVICE_PROTOCOL_ACTION);
		        App.getApp().getApplicationContext().sendBroadcast(intent);
	    		SharedPreferences.Editor editor = App.getApp().getApplicationContext().getSharedPreferences("FRIST_SHOW_SERVICE_PROTOCAL", Context.MODE_PRIVATE).edit();
	    		editor.putBoolean("FirstShowUserServiceProtocal", false);
	    		editor.commit();
				isshow = false;
				mWindowManager.removeView(mFloatingWndLayout);
			}
		});
		
	}
}
