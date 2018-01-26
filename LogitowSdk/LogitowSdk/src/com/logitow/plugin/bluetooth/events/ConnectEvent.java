//
//  ConnectEvent
//	是否连接事件
//
//  Created by paracrakevin on 2018/1/24.
//  Copyright © 2018年 paracra. All rights reserved.
//

package com.logitow.plugin.bluetooth.events;

public class ConnectEvent {
	
	public final String addr;
	public final Boolean connected;
	 
    public ConnectEvent(String addr,Boolean connected) {
        this.addr = addr;
        this.connected = connected;
    }
	
}
