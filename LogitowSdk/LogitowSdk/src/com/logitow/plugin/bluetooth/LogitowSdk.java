//
//  LogitowSdk
//	逻辑塔接口类
//
//  Created by paracrakevin on 2018/1/24.
//  Copyright © 2018年 paracra. All rights reserved.
//


package com.logitow.plugin.bluetooth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.greenrobot.eventbus.EventBus;

import com.logitow.plugin.bluetooth.events.ConnectEvent;
import com.logitow.plugin.bluetooth.events.DevicePowerEvent;
import com.logitow.plugin.bluetooth.events.FindDeviceEvent;
import com.logitow.plugin.bluetooth.events.UpdateBleValueEvent;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

public class LogitowSdk {
	
	public static final String TAG = "LogitowSdk";
	
	public static final int RESULT_COMPLETE = -1;
	
	//激活手机蓝牙设置
	private static final int REQUEST_ENABLE_BT = 1;
	
	//蓝牙适配器
 	private BluetoothAdapter mBluetoothAdapter;
 	
	//当前运行的逻辑塔sdk单例
	private static LogitowSdk instance;
	
	//当前正运行的activity;
	private Activity currBleActvity;
	
	//logitow sdk当前连接状态
	private boolean connected = false;
	
	//是否开启搜索
	private boolean mScanning = false;
	
	//搜索到的所有设备
	private List<DeviceInfo> currFindDevices = new ArrayList<>();
	
	//低功耗蓝牙服务
	private BluetoothLeService mBluetoothLeService;
	
	//蓝牙服务连接状态处理
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "logtiw--info-Unable to initialize Bluetooth");
                //finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            //mBluetoothLeService.connect(mDeviceAddress);
            Log.e(TAG, "logtiw--info-onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //mBluetoothLeService = null;
        	 Log.e(TAG, "logtiw--onServiceDisconnected");
        }
    };
    
  //蓝牙广播通讯动作处理，蓝牙设备连接状态，断开状态，以及数据通信的逻辑处理
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
            		
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                String addr = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_ADDR);
                Log.e(TAG,"主机断开连接"+addr);
                //发送断开连接的事件
                EventBus.getDefault().post(new ConnectEvent(addr,false));
                
                DeviceInfo dinfo = getDeviceInfoByAddr(addr);
                if(dinfo == null)return;
                dinfo.setConnected(0);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.e(TAG,"主机已经连接");	        	
                String addr = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_ADDR);
                DeviceInfo dinfo = getDeviceInfoByAddr(addr);
                if(dinfo == null)return;
                dinfo.setConnected(1);
                enableFuncNotification(mBluetoothLeService.getSupportedGattServices(),addr);
                
                //发送已连接的事件
                EventBus.getDefault().post(new ConnectEvent(addr,true));
            } else if(BluetoothLeService.ACTION_ENABLEDTX_END.equals(action))
            {
            	Log.e(TAG,"主机功能模块通知激活结束");
            	String addr = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_ADDR);
            	enableModelNotification(mBluetoothLeService.getSupportedGattServices(),addr);
            	
            	
            }else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            	if(intent!=null)
            	{
            		String extdata = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
            		String extdlevel = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_LEVEL);
            		if(extdata!=null)
            		{
            			Log.e(TAG,intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            			
            			//发送更新搜索到设备的事件  
            			EventBus.getDefault().post(new UpdateBleValueEvent(extdata));
            			return;
            		}
            		if(extdlevel!=null)
            		{
            			Log.e(TAG,intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_LEVEL));
            			String lvstr = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_LEVEL);
            			Float lvnum = Float.parseFloat(lvstr);
            			Float num = (lvnum - 1.5f)*100.0f;
            			Float fnum = (num/60f)*100f;
            			if(fnum<=0f)
            			{
            				//WebAppActivity.deviceLevel = 0;
            				 EventBus.getDefault().post(new DevicePowerEvent(0));
            			}else if(fnum>=100f)
            			{
            				//WebAppActivity.deviceLevel = 100;
            				 EventBus.getDefault().post(new DevicePowerEvent(100));
            			}else
            			{
            				//WebAppActivity.deviceLevel = fnum.intValue();
            				 EventBus.getDefault().post(new DevicePowerEvent(fnum.intValue()));
            			}
            			
            			//发送更新电量的事件
            		}
            	}
            }
        }
    };
    
    //蓝牙设备搜索时的硬件找寻
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
    	
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        	
        	if(device.getName()!=null&&device.getName().equals(LogitowBlueConstants.LOGITOW_DEVICE))
        	{
        		Log.e(TAG, "识别设备："+device.getName()+"//"+device.getAddress());
        		DeviceInfo dinfo = new DeviceInfo();
            	dinfo.setAddr(device.getAddress());
            	currFindDevices.add(dinfo);
            	//发送更新搜索到设备的事件
            	 EventBus.getDefault().post(new FindDeviceEvent(dinfo));
        	}
        }
    };
    
    //----------------------------------sdk 构造
    
	public LogitowSdk()
	{
	}
	
	public static LogitowSdk getInstance()
	{
		if(instance==null)
		{
			instance = new LogitowSdk();
		}
		return instance;
	}
	
	private void init()
	{
		Intent gattServiceIntent = new Intent(this.currBleActvity, BluetoothLeService.class);
		this.currBleActvity.bindService(gattServiceIntent, mServiceConnection, 0x0001);
		if (!this.currBleActvity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			return;
		}
		final BluetoothManager bluetoothManager =
		(BluetoothManager) this.currBleActvity.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		if(mBluetoothAdapter == null) {
			 return;
		}
	}
	
	 /**
     * 获取sdk版本号
     * 
     * @param 
     *         
     * @return 
     */
	public static String sdkVersion()
	{
		return "v1.0";
	}
	
	//----------------------------------------------对外接口方法
	/**
     * 初始化sdk的当前操作蓝牙activity
     * 
     * @param  Activity _currAct
     */
	public void initSdk(Activity _currAct)
	{
		if(this.currBleActvity !=null)return;
		this.currBleActvity = _currAct;
		init();
	}
	
	/**
     * 改变之前的运行activity.切换蓝牙操作的activity
     * 
     * @param  Activity _currAct
     */
	public void changeActivity(Activity _currAct)
	{
		//清除之前activity 所有的绑定服务
		this.currBleActvity = _currAct;
		this.clearAllBlueTooth();
		init();
	}
	
	/**
     * 连接指定的唯一地址设备
     * 
     * @param String daddr
     */
	public void connectLogitowDevice(String daddr)
	{
		DeviceInfo dinfo = this.getDeviceInfoByAddr(daddr);
		if(dinfo!=null&&dinfo.getConnected() == 0)
		{
			if (mBluetoothLeService != null) {
                final boolean result = mBluetoothLeService.connect(dinfo.getAddr());
                Log.d("连接蓝牙", "connectLogitowDevice："+daddr+ "//result：" + result);
            }
		}
	}
	
	/**
     * 开始搜索设备
     * 
     * @param 
     */
	public void startSearchDevice()
	{
		//蓝牙处理如果蓝牙权限没有开启权限
		if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.currBleActvity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
		this.currBleActvity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		scanLeDevice(true);
	}
	
	/**
     * 停止搜索设备
     * 
     * @param 
     */
	public void stopSearchDevice()
	{
		scanLeDevice(false);
	}
	
	/**
     * 获取设备电量
     * 
     * @param 
     */
	public void getDevicePower(String daddr)
	{
		DeviceInfo dinfo = this.getDeviceInfoByAddr(daddr);
		if(dinfo!=null&&dinfo.getConnected() == 1&&dinfo.getModelCharacteristic()!=null)
		{
			byte[] wdata = new byte[]{(byte)0xAD,(byte)0x02};
			String currDataStr = "";
			final StringBuilder stringBuilder = new StringBuilder(wdata.length);
			for(int i=0;i<wdata.length;i++)
	        {
	        	byte byteChar = wdata[i];
	        	String str = String.format("%02X ", byteChar);
	        	stringBuilder.append(str);
	        	str = str.trim(); 
	        	currDataStr  += str;
	        }
			Log.e(TAG,currDataStr+"//"+dinfo.getModelCharacteristic().getUuid());
			// byte[] arrayOfByte = { -83, 2 };
			dinfo.getModelCharacteristic().setValue(wdata);
			dinfo.getModelCharacteristic().setWriteType(2);
			mBluetoothLeService.writeCharacteristic(dinfo.getModelCharacteristic());
		}
	}
	
	/**
     * 清除所有蓝牙连接
     * 
     * @param 
     */
	public void clearAllBlueTooth()
	{
		if(mBluetoothLeService!=null)
		{
			mBluetoothLeService.disconnect();
		}
		scanLeDevice(false);
		this.currBleActvity.unregisterReceiver(mGattUpdateReceiver);
		this.currBleActvity.unbindService(mServiceConnection);
		mBluetoothLeService = null;
		currFindDevices.clear();
	}
	
	//----------------------------------------------私有属性
	//搜索附近的蓝牙开启设备
	private void scanLeDevice(final boolean enable) {
	   if (enable) {
	     mScanning = true;
	     mBluetoothAdapter.startLeScan(mLeScanCallback);
	     Log.e(TAG,"scanLeDevice-enable:true");
	   } else {
	     mScanning = false;
	     mBluetoothAdapter.stopLeScan(mLeScanCallback);
	     Log.e(TAG,"scanLeDevice-enable:false");
	   }
	}
	
	/**
     * 激活功能描述对象的通知
     * 
     * @param 
     */
	private void enableFuncNotification(List<BluetoothGattService> gattServices,String addr)
	{
		if (gattServices == null) return;
		 	
		DeviceInfo dinfo = this.getDeviceInfoByAddr(addr);
		if(dinfo == null)return;

	    String uuid = null;
	    for (BluetoothGattService gattService : gattServices) {
	        uuid = gattService.getUuid().toString();
	        //Log.e(TAG,"enableTXNotification:"+uuid);
	        //数据传送服务
	        if(uuid.equals(LogitowBlueConstants.HEART_RATE_MEASUREMENT))
	        {
	        	List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
	        	for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
	        		uuid = gattCharacteristic.getUuid().toString();
	        		//找到该服务UUID匹配的蓝牙描述对象
	        		if(uuid.equals(LogitowBlueConstants.CLIENT_CHARACTERISTIC_CONFIG))
	        		{
	                    Log.e(TAG,"enableFuncNotification-CLIENT_CHARACTERISTIC_CONFIG-UUID:"+uuid);
	                    dinfo.setCharacteristic(gattCharacteristic);
	                    connectCurrService(dinfo);
	                 }
	            }
	        }
	     }
	}
	
	//连接当前蓝牙服务器
    public void connectCurrService(DeviceInfo dinfo)
	{
    	if(dinfo.getCharacteristic()==null)
		{
			Log.e(TAG,"蓝牙未成功连接该设备服务");
			return;
		}
		final int charaProp = dinfo.getCharacteristic().getProperties();
		
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
        	//Log.e(TAG, "connectCurrServiceB");
        	mBluetoothLeService.setCharacteristicNotification(dinfo.getCharacteristic(), false);
            mBluetoothLeService.readCharacteristic(dinfo.getCharacteristic());
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
        	//Log.e(TAG, "connectCurrServiceA");
            mBluetoothLeService.setCharacteristicNotification(dinfo.getCharacteristic(),true);
        }
	}
	
	/**
     * 激活模块描述对象的通知
     * 
     * @param 
     */
	private void enableModelNotification(List<BluetoothGattService> gattServices,String addr)
	{
		if (gattServices == null) return;

		DeviceInfo dinfo = this.getDeviceInfoByAddr(addr);

		if(dinfo == null)return;
	     
		String uuid = null;
		 
		for (BluetoothGattService gattService : gattServices) {
			uuid = gattService.getUuid().toString();
			//Log.e(TAG,"enableTXNotification1:"+uuid);
	            
			//模块驱动服务
			if(uuid.equals(LogitowBlueConstants.HEART_RATE_MODEL))
			{
				List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
				for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
					uuid = gattCharacteristic.getUuid().toString();
					//Log.e("charas-WRITEgattCharacteristic-uuid:",uuid);
					//找到该服务UUID匹配的蓝牙描述对象
					if(uuid.equals(LogitowBlueConstants.WRITE_CHARACTERISTIC_CONFIG))
					{
						Log.e(TAG,"enableModelNotification-WRITE_CHARACTERISTIC_CONFIG-UUID:"+uuid);
						dinfo.setModelCharacteristic(gattCharacteristic);
						connectCurrWriteService(dinfo);
	                }
	             }
	         }
	           
	     }
	}
	
	
	//连接当前写入蓝牙服务器特征对象
    public void connectCurrWriteService(DeviceInfo dinfo)
	{
        final int charaProp = dinfo.getModelCharacteristic().getProperties();
		//Log.e("AppActivity:", "connectCurrWriteService"+charaProp);
		
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
        	//Log.e("AppActivity:", "connectCurrWriteServiceB");
        	mBluetoothLeService.setCharacteristicNotification(dinfo.getModelCharacteristic(), false);
            mBluetoothLeService.readCharacteristic(dinfo.getModelCharacteristic());
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
        	//Log.e("AppActivity:", "connectCurrWriteServiceA");
            mBluetoothLeService.setCharacteristicNotification(dinfo.getModelCharacteristic(),true);
        }
	}
	
	/**
     * 通过设备唯一地址找寻设备信息对象
     * 
     * @param String addr
     */
	private DeviceInfo getDeviceInfoByAddr(String addr)
	{
		DeviceInfo dinfo = null;
		DeviceInfo tinfo = null;
		for (int i = 0; i < currFindDevices.size(); i++) {
			tinfo = currFindDevices.get(i);
			//Log.e(TAG, "connectCurrWriteService"+tinfo.getAddr());
			if(tinfo.getAddr().equals(addr))
			{
				dinfo = tinfo;
			}
		}
		return dinfo;
	}
	
	private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_ENABLEDTX_END);
        return intentFilter;
    }
}