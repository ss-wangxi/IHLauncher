package cc.snser.launcher.widget.traffic2x3;

import com.btime.launcher.R;

import android.app.AlertDialog;
import android.app.Service;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
public class ShowDialogService extends Service {
	@Override
	public IBinder onBind(Intent intent) {
		
		return null;
	}
    @Override
    public void onCreate() {
	    // TODO Auto-generated method stub
	   super.onCreate();
    }
    @Override
    public void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
    }
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub		
		View view = View.inflate(ShowDialogService.this, R.layout.flow_reminder_dialog, null);  
    	TextView msgTxt = (TextView)view.findViewById(R.id.flow_reminder_message);
    	String msg = intent.getStringExtra("FLOWMESSAGEREMINDER");
    	msgTxt.setText(msg);
    	AlertDialog.Builder builder = null;
    	if(builder == null){
    		builder = new Builder(ShowDialogService.this);
    	}
    	builder.setView(view);
		final AlertDialog dialog = builder.create();
		dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);	
		dialog.show();
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();                
        lp.width = 750;          
        dialog.getWindow().setAttributes(lp); 
		Button cancelBtn = (Button) view.findViewById(R.id.cancel_flow_reminder);
		Button valueBtn = (Button) view.findViewById(R.id.add_flow);
		cancelBtn.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v){
				dialog.dismiss();
				dialog.cancel();				
			}
		});
		valueBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent mintent = new Intent();
				mintent = getPackageManager().getLaunchIntentForPackage("com.caros.netmon");
				mintent.putExtra("NumFragment", 2);
				startActivity(mintent);
				dialog.dismiss();
				dialog.cancel();		
							
			}
		});
		Handler mhandler = new Handler();
		mhandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				dialog.dismiss();				
			}
		}, 10000);
		return super.onStartCommand(intent, flags, startId);
	}
	@Override
    public void onStart(Intent intent, int startId) {
    	// TODO Auto-generated method stub
    	super.onStart(intent, startId);    	
    }
}
