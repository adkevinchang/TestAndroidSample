//
//  BluetoothLeService
//	蓝牙的服务类
//
//  Created by paracrakevin on 2018/1/24.
//  Copyright © 2018年 paracra. All rights reserved.
//

package com.logitow.plugin.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import com.logitow.plugin.utils.ParserUtils;

/**
 * 蓝牙服务处理类
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    public final static String ACTION_GATT_CONNECTED    ="com.logitow.plugin.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED ="com.logitow.plugin.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED ="com.logitow.plugin.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE    ="com.logitow.plugin.ACTION_DATA_AVAILABLE";
    public final static String ACTION_ENABLEDTX_END     ="com.logitow.plugin.ENABLEDTX_END";
    
    public final static String EXTRA_DATA         ="com.logitow.plugin.EXTRA_DATA";
    public final static String EXTRA_DEVICE_LEVEL ="com.logitow.plugin.EXTRA_DEVICE_LEVEL";
    public final static String EXTRA_DEVICE_ADDR  ="com.logitow.plugin.EXTRA_DEVICE_ADDR";

    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(LogitowBlueConstants.HEART_RATE_MEASUREMENT);
    public final static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString(LogitowBlueConstants.CLIENT_CHARACTERISTIC_CONFIG);
    
    public final static UUID UUID_HEART_RATE_MODEL = UUID.fromString(LogitowBlueConstants.HEART_RATE_MODEL);
    public final static UUID WRITE_CHARACTERISTIC_CONFIG = UUID.fromString(LogitowBlueConstants.WRITE_CHARACTERISTIC_CONFIG);
    
    public final static UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // 蓝牙透传的回调处理
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
    	//蓝牙设备连接状态的改变，并广播连接通知
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Attempting to start service discovery:" +mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                final Intent intent = new Intent(intentAction);
                intent.putExtra(EXTRA_DEVICE_ADDR,gatt.getDevice().getAddress());
                sendBroadcast(intent);
            }
        }
        
        //蓝牙服务被发现状态
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final Intent intent = new Intent(ACTION_GATT_SERVICES_DISCOVERED);
                intent.putExtra(EXTRA_DEVICE_ADDR,gatt.getDevice().getAddress());
                sendBroadcast(intent);
            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }
        
        //蓝牙某个服务的特征被读取状态
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
        	Log.e(TAG, "onCharacteristicRead: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
        
        //蓝牙某个服务的特征变量改变状态
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	//Log.e(TAG, "onCharacteristicChanged: ");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
        
        //蓝牙特征写入
        public void onCharacteristicWrite(BluetoothGatt paramBluetoothGatt, BluetoothGattCharacteristic paramBluetoothGattCharacteristic, int paramInt)
        {
          Log.i("TAG", "onCharacteristicWrite" + new String(paramBluetoothGattCharacteristic.getValue()));
        }
        
        //蓝牙特征的描述对象被写入
        public void onDescriptorWrite(BluetoothGatt paramBluetoothGatt, BluetoothGattDescriptor paramBluetoothGattDescriptor, int paramInt)
        {
        	String str = paramBluetoothGattDescriptor.getCharacteristic().getUuid().toString();
            super.onDescriptorWrite(paramBluetoothGatt, paramBluetoothGattDescriptor, paramInt);
            if (!(CLIENT_CHARACTERISTIC_CONFIG.equals(paramBluetoothGattDescriptor.getCharacteristic().getUuid())))
            {
              Log.i("TAG", "描述对象写入，模块通知已激活"+paramBluetoothGattDescriptor.getCharacteristic().getUuid());
              return;
            }
            Log.i("TAG", "描述对象写入，功能通知已激活"+paramBluetoothGattDescriptor.getCharacteristic().getUuid());
            final Intent intent = new Intent(ACTION_ENABLEDTX_END);
            intent.putExtra(EXTRA_DEVICE_ADDR,paramBluetoothGatt.getDevice().getAddress());
            sendBroadcast(intent);
        }
        
    };

    //更新广播
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    
    //蓝牙硬件广播数据，并解析数据
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        //Log.e(TAG,"broadcastUpdate:"+action+"uuid:"+characteristic.getUuid());
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.e(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        }else if(UUID_HEART_RATE_MODEL.equals(characteristic.getUuid()))
        {
        	int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.e(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DEVICE_LEVEL, String.valueOf(heartRate));   
        }else {
        	//Log.e(TAG,"service 数据 uuid"+characteristic.getUuid());
            final byte[] data = characteristic.getValue();
            boolean chackednum = false;
            if(data.length == 2)
            {
                  int i = ParserUtils.unsignedByteToInt(data[0]);
                  int j = ParserUtils.unsignedByteToInt(data[1]);
                  String str2 = i + "." + j;
                //  Log.e("service 数据2：",""+str2);
                  intent.putExtra(EXTRA_DEVICE_LEVEL,str2);
            }else
            {
            	if (data != null && data.length > 0) {
                	//Log.e("service 数据 length",""+data.length);
                	String currDataStr = "";
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for(int i=0;i<data.length;i++)
                    {
                    	byte byteChar = data[i];
                    	String str = String.format("%02X ", byteChar);
                    	stringBuilder.append(str);
                    	str = str.trim(); 
                    	currDataStr  += str;
                    	
                        if(i==3)
                        {
                        	int curnum = Integer.parseInt(str);
                        	if(curnum>0&&curnum<7)
                        	{
                        		chackednum = true;
                        	}
                        	//Log.e(TAG,"service  num"+i+"//"+curnum);
                        }
                    }
                    if(chackednum)
                    {
                    	intent.putExtra(EXTRA_DATA,currDataStr);
                    }
                    
                }
            }
            
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * 蓝牙初始化返回成功
     */
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    /**
     * 连接指定设备地址
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * 关闭
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * 读取特征描述对象
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }
    
    /**
     * 向蓝牙中写入数据。
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        
        boolean isWrite = mBluetoothGatt.writeCharacteristic(characteristic);
        Log.e(TAG, "mBluetoothGatt writeCharacteristic="+isWrite);
    }

    /**
     * 开启描述特征对象的通知功能
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        characteristic.setWriteType(2);
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        
        if (enabled&&CLIENT_CHARACTERISTIC_CONFIG.equals(characteristic.getUuid())) {
        	//Log.e(TAG, "BluetoothAdapter initialized"+characteristic);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
            if(descriptor!=null)
            {
        	  // Log.e(TAG, "BluetoothAdapter initialized");
        	   descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
               this.mBluetoothGatt.writeDescriptor(descriptor);
            }
       }
        
        if (enabled&&WRITE_CHARACTERISTIC_CONFIG.equals(characteristic.getUuid())) {
        	//Log.e(TAG, "WRITE initialized"+characteristic);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
            if(descriptor!=null)
            {
           	   descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
               this.mBluetoothGatt.writeDescriptor(descriptor);
            }
          }
    }

    /**
     *获取蓝牙硬件的所有服务
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }
}
