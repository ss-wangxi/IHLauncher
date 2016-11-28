package com.btime.settings.externalcall;

interface IExternalCallService{
	
	int  callVoiceUpDownFunction(boolean bShow,boolean bUp,float percent);
	int  callVoiceMinFunction(boolean bShow);
	void callVoiceSilentFunction(boolean bShow);
	int  callVoiceMaxFunction(boolean bShow);
	void callVoiceReset(boolean bShow);
	
	void callBringhtnessUpDownFunction(boolean bShow,boolean bUp,float percent);
	void callBringhtnessMinFunction(boolean bShow);
	void callBringhtnessMaxFunction(boolean bShow);
	
	void callHotspotOpen(boolean bShow,boolean bOpen);
	
	void callGPRSOpen(boolean bShow,boolean bOpen);
	
	void callFMOpen(boolean bShow,boolean bOpen);
	
	void callBluetoothOpen(boolean bShow,boolean bOpen);
	
	void callWLANOpen(boolean bShow,boolean bOpen);
		
}
