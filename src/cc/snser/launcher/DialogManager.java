package cc.snser.launcher;

import java.util.ArrayList;

import com.shouxinzm.launcher.util.DialogUtils;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Loader.ForceLoadContentObserver;
import android.provider.CalendarContract.Instances;

public class DialogManager {
	
	private static DialogManager INSTANCE = null;
	public static DialogManager instances(){
		if(INSTANCE == null){
			INSTANCE = new DialogManager();
		}
		return INSTANCE;
	}
	
	private ArrayList<DialogInterface> mDialogs;
	private DialogManager(){
		mDialogs = new ArrayList<DialogInterface>();
	}
	
	public void removeDialog(DialogInterface dialog){
		mDialogs.remove(dialog);
	}
	
	public void addDialog(DialogInterface dialog){
		mDialogs.add(dialog);
	}
	
	public void closeAllDialog(){
		for (int i = 0; i < mDialogs.size(); i++) {
			try {
				mDialogs.get(i).dismiss();
			} catch (Exception e) {
				//ignore this
			}
		}
		mDialogs.clear();
	}
	
	public int getDialogCount() {
		return mDialogs == null ? 0 : mDialogs.size();
	}
}
