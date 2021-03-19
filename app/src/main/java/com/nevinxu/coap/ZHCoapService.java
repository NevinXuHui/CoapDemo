package com.nevinxu.coap;

import java.io.Serializable;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.config.NetworkConfig;

import com.nevinxu.coap.cmd.ICmd;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.IBinder;
import android.util.Log;

public class ZHCoapService extends Service {
	private static final String TAG="WLCoapService";
	private static final boolean DEBUG=true;
	private static final int PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
	
	private static CoapServer mCoapServer;
	private ZHDataObserve mDataObserve;
	private ZHQueryResource mQueryResource;
	
	private boolean isConnected=false;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		initCoapResource();
		
		IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(connectReceiver, intentFilter);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Serializable serializable=intent.getSerializableExtra(Const.KEY_CMD);
		Log.d(TAG, "onStartCommand1");
		if(serializable != null && serializable instanceof ICmd){
			ICmd cmd=(ICmd) serializable;
			if(mDataObserve != null){
				mDataObserve.change(cmd.getCmdString());
				Log.d(TAG, "onStartCommand2");
			}
		}
		return START_STICKY;
	}
	
	
	/**
	 * 初始化CoapResource
	 */
	private void initCoapResource() {
		mCoapServer = new CoapServer(PORT);
		mQueryResource=new ZHQueryResource(getApplicationContext(),"query");
		mDataObserve=new ZHDataObserve("notify");
		mCoapServer.add(mDataObserve);
		mCoapServer.add(mQueryResource);
	}
	
	/**
	 * 开启coap服务
	 */
	private void startCoapServer() {
		try {
			stopCoapServer();
			mCoapServer.start();
			if (DEBUG)
				Log.d(TAG, "CoapServer start");
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.d(TAG, "CoapServer start2");
	}
	
	/**
	 * 停止coap服务
	 */
	private void stopCoapServer() {
		if (mCoapServer != null) {
			mCoapServer.stop();
			if (DEBUG)
				Log.d(TAG, "CoapServer stop");
		}
	}

	private BroadcastReceiver connectReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			State state = State.DISCONNECTED;
			if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				ConnectivityManager contectivityMananger = (ConnectivityManager) context
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo mNetworkInfo = contectivityMananger.getActiveNetworkInfo();
				if (mNetworkInfo != null && mNetworkInfo.isConnected()) {
					state = mNetworkInfo.getState();
				}
			}
			if (state == State.CONNECTED &&!isConnected) {
				isConnected=true;
				startCoapServer();
			} else if (state == State.DISCONNECTED) {
				isConnected=false;
				stopCoapServer();
			}
		}
	};
	
	public void onDestroy() {
		unregisterReceiver(connectReceiver);
		super.onDestroy();
	};
}
