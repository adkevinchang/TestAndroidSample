//
//  FindDeviceEvent
//	是否连接事件
//
//  Created by paracrakevin on 2018/1/24.
//  Copyright © 2018年 paracra. All rights reserved.
//
package com.logitow.plugin.bluetooth.events;

import com.logitow.plugin.bluetooth.DeviceInfo;

public class FindDeviceEvent {
	
	public final DeviceInfo dinfo;
	
	public FindDeviceEvent(DeviceInfo _info)
	{
		this.dinfo = _info;
	}
	
}
