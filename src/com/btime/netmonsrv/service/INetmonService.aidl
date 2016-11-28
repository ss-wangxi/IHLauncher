package com.btime.netmonsrv.service;

import com.btime.netmonsrv.service.INetmonServiceCallback;


interface INetmonService {
	void registerCallback(in INetmonServiceCallback cb);
	void unregisterCallback(in INetmonServiceCallback cb);
	
	void getFlux();
	
	int getCycleDay();
	long getCycleFlux();
	
	void setCycleDay(int day);
	void setCycleFlux(long flux);
	
	void setCycle(int day, long flux);
	
	void enableNetwork(int uid, int enable);
	
	boolean isVaild();
	
	List<String> getDispAppList();
}