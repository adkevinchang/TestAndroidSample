package com.logitow.logitowsdk;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.logitow.logtiowsdk.R;
import com.logitow.plugin.bluetooth.LogitowSdk;
import com.logitow.plugin.bluetooth.events.ConnectEvent;
import com.logitow.plugin.bluetooth.events.DevicePowerEvent;
import com.logitow.plugin.bluetooth.events.FindDeviceEvent;
import com.logitow.plugin.bluetooth.events.UpdateBleValueEvent;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
	
	private LogitowSdk ltsdk;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ltsdk = LogitowSdk.getInstance();
		ltsdk.initSdk(this);
		ltsdk.startSearchDevice();
	}
	
	@Subscribe(threadMode = ThreadMode.POSTING)
	public void onConnectEvent(ConnectEvent evt)
	{
		Log.e("MainActivity","ConnectEvent："+evt.addr);
	}
	
	@Subscribe(threadMode = ThreadMode.POSTING)
	public void onDevicePowerEvent(DevicePowerEvent evt)
	{
		Log.e("MainActivity","DevicePowerEvent："+evt.power);
	}
	
	@Subscribe(threadMode = ThreadMode.POSTING)
	public void onFindDeviceEvent(FindDeviceEvent evt)
	{
		Log.e("MainActivity","FindDeviceEvent："+evt.dinfo.getAddr());
		if(evt.dinfo.getAddr().equals("E7:4D:87:F3:21:2D"))
		{
			ltsdk.stopSearchDevice();
			ltsdk.connectLogitowDevice(evt.dinfo.getAddr());
		}
	}
	
	@Subscribe(threadMode = ThreadMode.POSTING)
	public void UpdateBleValueEvent(UpdateBleValueEvent evt)
	{
		Log.e("MainActivity","UpdateBleValueEvent："+evt.data);
		ltsdk.getDevicePower("E7:4D:87:F3:21:2D");
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		EventBus.getDefault().register(this);
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		EventBus.getDefault().unregister(this);
	}
	
}
