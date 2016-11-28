package cc.snser.launcher.util;

import java.util.Iterator;
import java.util.Vector;

import android.os.Handler;
/**
 * 解决postDelayed在cpu进入deep sleep时失效的问题
 * 没有使用android提供的闹钟机制了
 * @author zgl
 *
 */

class AlarmMgr implements Runnable{
	private static AlarmMgr mInstance = new AlarmMgr();
	private Handler mHandler = new Handler();
	private Vector<Alarm> mAlarms = new Vector<Alarm>();
	private Vector<Alarm> mRemoveAlarms = new Vector<Alarm>()	;
	public static AlarmMgr getInstance() {
		if(mInstance == null){
			mInstance = new AlarmMgr();
		}
		return mInstance;
	}
	
	public void onScreenOn(){
		Iterator<Alarm> iterator = mAlarms.iterator();
		while(iterator.hasNext()){
			final Alarm alarm = iterator.next();
			if(!checkTrigger(alarm)){
				alarm.restartAlarm();
			}else {
				iterator.remove();
			}
		}
	}
	
	public void addAlarm(Alarm alarm){
		Iterator<Alarm> iterator = mAlarms.iterator();
		while(iterator.hasNext()){
			final Alarm alarmInfo = iterator.next();
			if(alarm.getId() != -1 && alarmInfo.getId() == alarm.getId()){
				iterator.remove();
			}
		}

		mAlarms.addElement(alarm);
	}
	
	public void removeAlarm(Alarm alarm){
		mAlarms.remove(alarm);
	}
	
	public void startAlarm(long millisecondsInFuture){
		mHandler.postDelayed(this, millisecondsInFuture);
	}

	private boolean checkTrigger(Alarm alarm){
		final long triggerTime = alarm.getTriggerTime();
		if(triggerTime > 0){
			if(alarm.isCanTrigger()){
				alarm.trigger();
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public void run() {
		mRemoveAlarms.clear();
		Vector<Alarm> cloneAlarms = (Vector<Alarm>)mAlarms.clone();
		Iterator<Alarm> iterator = cloneAlarms.iterator();
		
		while(iterator.hasNext()){
			final Alarm alarm = iterator.next();
			if(checkTrigger(alarm) || alarm.getTriggerTime() <= 0){
				mRemoveAlarms.add(alarm);
			}
		}
		
		for(Alarm alarm : mRemoveAlarms){
			mAlarms.remove(alarm);
		}
	}
}
