package net.freedoman.onetouch;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;

public class OneTouch extends Activity {
	
	//涉及到的组件变量
	private EditText student_id;
	private EditText passwd;
	private Button login;
	
	//用于控制无线网连接的变量
	private CheckControlWifi cc_wifi;
	private int state;
	private int wait_seconds_for_openwifi;
	private int wait_seconds_for_zjuwlan;
	public boolean cancel;
	public boolean enableZJUWLAN;
	
	//响应事件的句柄变量
	public static Handler main_handler;
	
	//提示信息变量
	private ProgressDialog pDialog;
	
	//本地存储变量
	private SharedPreferences preferences;

	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.onetouch);
		
		student_id = (EditText)findViewById(R.id.studentid);
		passwd = (EditText)findViewById(R.id.password);
		login = (Button)findViewById(R.id.login);
		
		pDialog = new ProgressDialog(this);
		preferences = getSharedPreferences("zjuwlan_personal",MODE_PRIVATE);
		
		cancel = false;
		cc_wifi = new CheckControlWifi(this);
		enableZJUWLAN = false;
		wait_seconds_for_openwifi = 0;
		wait_seconds_for_zjuwlan = 0;
		
		//定义主控制函数
		main_handler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				if(cancel){
					cancel = false;
					return;
				}
				switch(msg.what){
					case 0x2000:
						Log.v("connected","ok");
						doLogin();
	    				break;
					case 0x2001:
						checkControlWifi();
						break;
				    case 0x2002://登陆成功
				    	showDialog("您已成功登陆,即将退出");
				    	new Timer().schedule(new TimerTask(){
				    		@Override
				    		public void run(){
				    			this.cancel();
				    			exitApp();
				    		}
				    	},2000);
				    	break;
				    case 0x2003:
				    	dismissDialog();
				    	showToast("用户名错误");
				    	break;
				    case 0x2004:
				    	dismissDialog();
				    	showToast("密码错误");
				    	break;
				    case 0x2005:
				    	dismissDialog();
	    				showToast("未连接到ZJUWLAN，请检查网络信号，并手动连接");
	    				break;
				    case 0x2006:
				    	dismissDialog();
				    	showToast("账号被禁用，也许欠费了");
				    	break;
				    default:
				    	break;
				}
			}
		};
		
		//点击按钮登陆
		login.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				if(cancel){
					cancel = false;
				}
				checkControlWifi();
			}
		 });
		
		initDialog();
		//检查并读取本地存储信息
		String studentId_value = preferences.getString("studentId",null);
		String passwd_value =preferences.getString("passwd",null);
		
		
		if(studentId_value != null && studentId_value != "" && passwd_value != null && passwd_value != ""){
			student_id.setText(studentId_value);
			passwd.setText(passwd_value);
		}else{
			cancel = true;
			dismissDialog();
			showToast("请先填写学号和密码");
		}
			
		//检查无线连接并连接到ZJUWLAN
		
		if(!cancel){
			checkControlWifi();
		}
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
	}
	
	//退出程序
	private void exitApp(){
		if(cancel){
			cancel = false;
			return;
		}
		dismissDialog();//结束对话框
		finish();//退出界面
//		System.exit(0);//完全结束程序  error: Unknown binder error code. 0xfffffff7
		android.os.Process.killProcess(android.os.Process.myPid()); 
	}
	
	//检查学号或密码是否为空
	private boolean checkEmpty(){
		String student_id_value = student_id.getText().toString();
		String passwd_value = passwd.getText().toString();
		if(student_id_value == null || student_id_value.trim().equals("") || passwd_value == null || passwd_value.trim().equals("")){
			dismissDialog();
			showToast("学号或密码为空");
			return false;
		}
		return true;
	}
	
	//登录
	private void doLogin(){
		if(checkEmpty())
		{
			showDialog("已连接到ZJUWLAN,正在登陆");
			new OnSendListener(student_id,passwd,preferences).doConnect();
			return;
		}
	}
	
	//检查并控制无线登陆
	private void checkControlWifi(){
		state = cc_wifi.CheckState();
		Log.v("state", state + "");
		//WIfI为可用状态   WIFI_STATE_ENABLED 0x00000003
		if(state == 0x00000003)
		{
			Log.v("state","enable");
			if(cc_wifi.checkZJUWLAN())
			{
		    	main_handler.sendEmptyMessage(0x2000);
			}
			else
			{
				showDialog("等待连接到ZJUWLAN");
				if(!enableZJUWLAN){
					if(connectZJUWLAN())
					{
						enableZJUWLAN = true;
					}else{
						return;
					}
				}
				new Timer().schedule(new TimerTask(){
			    	@Override
			    	public void run(){
			    		this.cancel();
			    		Log.v("cycling3","yes");
			    		wait_seconds_for_zjuwlan++;
			    		Log.v("wait_seconds_for_zjuwlan",wait_seconds_for_zjuwlan + "");
			    		if(wait_seconds_for_zjuwlan > 15)
			    			main_handler.sendEmptyMessage(0x2005);
			    		else
			    			main_handler.sendEmptyMessage(0x2001);
			    	}
			    },2000);
			}
			return;
		}
		//WIFI为位置状态 WIFI_STATE_UNKNOWN 0x00000004
		else if(state == 0x00000004){
			main_handler.sendEmptyMessage(0x2005);
			return;
		}
		//WIFI为不可用状态 WIFI_STATE_DISABLED 0x00000001
		else if(state == 0x00000001){
			Log.v("state","disabled");
			showDialog("正在打开无线网络");
			cc_wifi.openWifi();
		}
		//其它状态则等待操作
		new Timer().schedule(new TimerTask(){
	    	@Override
	    	public void run(){
	    		this.cancel();
	    		Log.v("cycling","yes");
	    		wait_seconds_for_openwifi++;
	    		if(wait_seconds_for_openwifi > 30)
	    			main_handler.sendEmptyMessage(0x2005);
	    		else
	    			main_handler.sendEmptyMessage(0x2001);
	    	}
	    },1000);
		return;
	}
	
	//连接到ZJUWLAN
	private boolean connectZJUWLAN(){
		int zjuwlan_id = cc_wifi.findZJUWLAN();
		Log.v("zjuwlan_id",zjuwlan_id + "");
		if(zjuwlan_id != 0xffff){
			cc_wifi.connectZJUWLAN(zjuwlan_id);
			return true;
		}
		else{
			Log.v("nosave","zjuwlan");
			main_handler.sendEmptyMessage(0x2005);
		}
		return false;
	}
	
	//使用Toast显示相关连接信息
	private void showToast(String info){
		Toast toast = Toast.makeText(this,info,Toast.LENGTH_LONG);
		toast.show();
	}
	
	//初始化对话框
	private void initDialog(){
		pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pDialog.setTitle("提示");
		pDialog.setIndeterminate(false);
		pDialog.setCancelable(true);
		pDialog.setButton(-2,"取消",new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which){
				cancel=true;
				dialog.cancel();//TODO
			}
		});
	}
	
	//显示对话框或更改对话框中的内容
	private void showDialog(String info){
		if(!pDialog.isShowing()){
			Log.v("pDialog","no");
			pDialog.show();
		}
		pDialog.setMessage(info);
	}
	
	//结束对话框的显示
	private void dismissDialog(){
		if(pDialog.isShowing()){
			pDialog.dismiss();
		}
	}
	
}
