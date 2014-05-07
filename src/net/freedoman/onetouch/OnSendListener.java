package net.freedoman.onetouch;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class OnSendListener implements OnClickListener{
	
	private EditText studentId;
	private EditText passwd;
	private byte[] entitydata;
	private Handler responseHandler;
	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;
	private OneTouch onetouch;
	private boolean cancel;
	
	public OnSendListener(EditText studentId,EditText passwd,SharedPreferences preferences){
		this.studentId = studentId;
		this.passwd = passwd;
		this.preferences = preferences;
		onetouch = new OneTouch();
		this.cancel = onetouch.cancel;
	}
	@Override
	public void onClick(View v){
		doConnect();
	}
	@SuppressLint("HandlerLeak")
	public void doConnect(){
		saveInfo();
		String query = "action=login&username=" + studentId.getText().toString().trim() + "&password=" + passwd.getText().toString().trim() + "&ac_id=5&is_ldap=1&type=2&local_auth=1";
		entitydata = query.getBytes();
		
		Login login = new Login();
		responseHandler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				if(cancel){
					return;
				}
				switch(msg.what){
				  case 0x1000:
					  OneTouch.main_handler.sendEmptyMessage(0x2002);
					  break;
				  case 0x1001:
					  break;
				  case 0x1002:
					  OneTouch.main_handler.sendEmptyMessage(0x2003);
					  break;
				  case 0x1003:
					  OneTouch.main_handler.sendEmptyMessage(0x2004);
					  break;
				  case 0x1005:
					  OneTouch.main_handler.sendEmptyMessage(0x2006);
				  default:
					  break;
				}
			}
		};
		new Thread(login).start();
	}
	private void saveInfo(){
		editor = preferences.edit();
		editor.putString("studentId",studentId.getText().toString().trim());
	    editor.putString("passwd",passwd.getText().toString().trim());
		editor.commit();
	}
	private class Login implements Runnable{
		@Override
		public void run(){
			    if(cancel){
			    	return;
			    }
			    String response_info = "";
			    Log.v("login_begin","ok");
			    response_info = doLogin();
			    Log.v("res_info",response_info);
			    Log.v("login","ok");
			    
				if(response_info.contains("login_ok")){
				   responseHandler.sendEmptyMessage(0x1000);
				}
				else if(response_info.contains("online_num_error")){
				   if(!doForce()){
				    	responseHandler.sendEmptyMessage(0x1001);
				   }else{
					   responseHandler.sendEmptyMessage(0x1000);
				   }
				}
				else if(response_info.contains("username_error")){
				   responseHandler.sendEmptyMessage(0x1002);
				}
				else if(response_info.contains("password_error")){
				   responseHandler.sendEmptyMessage(0x1003);
				}
				else if(response_info.contains("status_error") || response_info.contains("available_error")){
					//TODO 账户到期被禁用
					responseHandler.sendEmptyMessage(0x1005);
			    }
				else{
					//用户名或密码错误
					   responseHandler.sendEmptyMessage(0x1004);
				}
				
		}
	}
	private String doLogin(){
		  String url = "http://net.zju.edu.cn/cgi-bin/srun_portal";
		  return sendPost(url,entitydata);
	}
    private String sendPost(String url,byte[] entitydata){
    	String result = "";
	    String line = "";
    	try{
		    HttpURLConnection link = (HttpURLConnection) (new URL(url)).openConnection();
		    
		    link.setRequestProperty("Content-Type","application/x-www-form-urlencoded;charset=utf-8");
		    link.setRequestProperty("Content-Length",String.valueOf(entitydata.length));
		    link.setRequestMethod("POST");
		    link.setDoOutput(true);
		    link.setDoInput(true);
		    link.connect();
		
		    OutputStream out = link.getOutputStream();
		    out.write(entitydata);
		    out.flush();
		    out.close();
		    BufferedReader in = new BufferedReader(new InputStreamReader(link.getInputStream()));
		    while((line = in.readLine())!=null){
			    result += line;
		    }
		    in.close();
    	 }catch(Exception e){
		    e.printStackTrace();
		 }
    	return result;
    }
	private boolean doForce(){
			String response_info;
			String url = "http://net.zju.edu.cn/rad_online.php";
			String query = "action=auto_dm&username=" + studentId.getText().toString() + "&password=" + passwd.getText().toString();
			byte[] entitydata = query.getBytes();
			response_info = sendPost(url,entitydata);
			Log.v("res_info_force",response_info);
			if(response_info.contains("ok")){
				String force_login = doLogin();
//				Log.v("force",force_login);
				if(force_login.contains("login_ok")){
				    return true;
				}
				return false;
			}
			return false;
	}
}
