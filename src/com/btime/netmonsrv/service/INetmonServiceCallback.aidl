package com.btime.netmonsrv.service;

import com.btime.netmonsrv.service.Flux;

interface INetmonServiceCallback {
	void onUpdate( in Flux flux );
}
