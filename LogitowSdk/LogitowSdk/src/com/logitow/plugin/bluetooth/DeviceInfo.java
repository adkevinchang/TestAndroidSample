//
//  DeviceInfo
//  设备信息对象
//
//  Created by paracrakevin on 2018/1/24.
//  Copyright © 2018年 paracra. All rights reserved.
//

package com.logitow.plugin.bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;

public class DeviceInfo {

	 	private String addr = "";

	    private int connected = 0;

	    private int power = 0;
	    
	    private BluetoothGattCharacteristic characteristic;
	    
	    private BluetoothGattCharacteristic modelCharacteristic;

	    /*
	     * 硬件设备的唯一地址
	     * 
	     */
	    public String getAddr() {
	        return addr;
	    }

	    public void setAddr(String _addr) {
	        this.addr = _addr;
	    }
	    
	    /*
	     * 此设备是否已经连接
	     * 
	     */
	    public int getConnected() {
	        return connected;
	    }

	    public void setConnected(int _connected) {
	        this.connected = _connected;
	    }
	    
	    /*
	     * 此连接设备电量
	     * 
	     */
	    public int getPower() {
	        return power;
	    }

	    public void setPower(int _power) {
	        this.power = _power;
	    }
	    
	    /*
	     * 此设备功能描述对象
	     * 
	     */
	    public BluetoothGattCharacteristic getCharacteristic() {
	        return characteristic;
	    }
	    
	    public void setCharacteristic(BluetoothGattCharacteristic _cha) {
	        this.characteristic = _cha;
	    }
	    
	    /*
	     * 此设备模块描述对象
	     * 
	     */
	    public BluetoothGattCharacteristic getModelCharacteristic() {
	        return modelCharacteristic;
	    }
	    
	    public void setModelCharacteristic(BluetoothGattCharacteristic _cha) {
	        this.modelCharacteristic = _cha;
	    }
	    
	    
}
