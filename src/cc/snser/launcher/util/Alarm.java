package cc.snser.launcher.util;

public class Alarm{
    // if we reach this time and the alarm hasn't been cancelled, call the listener
    private long mAlarmTriggerTime;
    private long mItemId = -1;

    // if we've scheduled a call to run() (ie called mHandler.postDelayed), this variable is true.
    // We use this to avoid having multiple pending callbacks
    private OnAlarmListener mAlarmListener;
    private boolean mAlarmPending = false;

    public Alarm() {
    	AlarmMgr.getInstance().addAlarm(this);
    }
    
    public Alarm(long id){
    	AlarmMgr.getInstance().addAlarm(this);
    	mItemId = id;
    }

    public void setOnAlarmListener(OnAlarmListener alarmListener) {
        mAlarmListener = alarmListener;
    }

    // Sets the alarm to go off in a certain number of milliseconds. If the alarm is already set,
    // it's overwritten and only the new alarm setting is used
    public void startAlarm(long millisecondsInFuture) {
        long currentTime = System.currentTimeMillis();
        mAlarmPending = true;
        mAlarmTriggerTime = currentTime + millisecondsInFuture;
        AlarmMgr.getInstance().startAlarm(millisecondsInFuture);
        AlarmMgr.getInstance().addAlarm(this);
    }

    public void restartAlarm(){
    	if(mAlarmPending == false || mAlarmTriggerTime <= 0) return;
    	
    	AlarmMgr.getInstance().startAlarm(mAlarmTriggerTime - System.currentTimeMillis());
    }
    
    public void cancelAlarm() {
        mAlarmTriggerTime = 0;
        mAlarmPending = false;
    }

    public boolean isCanTrigger(){
    	if(mAlarmTriggerTime == 0) return false;
    	
    	return mAlarmTriggerTime <= System.currentTimeMillis();
    }
    
    public void trigger(){
    	mAlarmPending = false;
    	if(mAlarmListener != null){
    		mAlarmListener.onAlarm(this);
    	}
    }
    
    public long getTriggerTime(){
    	return mAlarmTriggerTime;
    }

    public boolean alarmPending() {
        return mAlarmPending;
    }
    
    public long getId(){
    	return mItemId;
    }
}
