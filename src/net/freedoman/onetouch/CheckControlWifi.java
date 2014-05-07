package net.freedoman.onetouch;

import java.util.List;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class CheckControlWifi {
	
	private WifiManager wifiManager;
	private Context context;

	public CheckControlWifi(Context context){
		this.context = context;
		wifiManager = (WifiManager)this.context.getSystemService(Context.WIFI_SERVICE);
	}
	
	public int CheckState(){
		int state = wifiManager.getWifiState();
		return state;
	}
	
	public boolean openWifi(){
		if(wifiManager.setWifiEnabled(true)){
			return true;
		}
		return false;
	}
	public boolean checkZJUWLAN(){
		
		WifiInfo wifi_info = wifiManager.getConnectionInfo();
		String ssid = wifi_info.getSSID();
		if(ssid != null){
		    if(ssid.contains("ZJUWLAN")){
		    	if(checkConnectedZJUWLAN()){
				    return true;
		    	}
		    }
		}
		return false;
	}
	private boolean checkConnectedZJUWLAN(){
		ConnectivityManager connectManager = (ConnectivityManager)this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectManager.getActiveNetworkInfo();
		if(networkInfo != null && networkInfo.isConnected()){
			return true;
		}
		return false;
	}
	public int findZJUWLAN(){
		List<WifiConfiguration> config_list = wifiManager.getConfiguredNetworks();
		if(config_list != null){
			for(int i=0;i<config_list.size();i++){
				if(config_list.get(i).SSID.contains("ZJUWLAN")){
					return config_list.get(i).networkId;
				}
			}
		}
		return 0xffff;
	}
	public boolean connectZJUWLAN(int network_id){
		if(wifiManager.enableNetwork(network_id,true)){
			return true;
		}
		return false;
	}
}
