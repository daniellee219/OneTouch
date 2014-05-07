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
	
	//�漰�����������
	private EditText student_id;
	private EditText passwd;
	private Button login;
	
	//���ڿ������������ӵı���
	private CheckControlWifi cc_wifi;
	private int state;
	private int wait_seconds_for_openwifi;
	private int wait_seconds_for_zjuwlan;
	public boolean cancel;
	public boolean enableZJUWLAN;
	
	//��Ӧ�¼��ľ������
	public static Handler main_handler;
	
	//��ʾ��Ϣ����
	private ProgressDialog pDialog;
	
	//���ش洢����
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
		
		//���������ƺ���
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
				    case 0x2002://��½�ɹ�
				    	showDialog("���ѳɹ���½,�����˳�");
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
				    	showToast("�û�������");
				    	break;
				    case 0x2004:
				    	dismissDialog();
				    	showToast("�������");
				    	break;
				    case 0x2005:
				    	dismissDialog();
	    				showToast("δ���ӵ�ZJUWLAN�����������źţ����ֶ�����");
	    				break;
				    case 0x2006:
				    	dismissDialog();
				    	showToast("�˺ű����ã�Ҳ��Ƿ����");
				    	break;
				    default:
				    	break;
				}
			}
		};
		
		//�����ť��½
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
		//��鲢��ȡ���ش洢��Ϣ
		String studentId_value = preferences.getString("studentId",null);
		String passwd_value =preferences.getString("passwd",null);
		
		
		if(studentId_value != null && studentId_value != "" && passwd_value != null && passwd_value != ""){
			student_id.setText(studentId_value);
			passwd.setText(passwd_value);
		}else{
			cancel = true;
			dismissDialog();
			showToast("������дѧ�ź�����");
		}
			
		//����������Ӳ����ӵ�ZJUWLAN
		
		if(!cancel){
			checkControlWifi();
		}
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
	}
	
	//�˳�����
	private void exitApp(){
		if(cancel){
			cancel = false;
			return;
		}
		dismissDialog();//�����Ի���
		finish();//�˳�����
//		System.exit(0);//��ȫ��������  error: Unknown binder error code. 0xfffffff7
		android.os.Process.killProcess(android.os.Process.myPid()); 
	}
	
	//���ѧ�Ż������Ƿ�Ϊ��
	private boolean checkEmpty(){
		String student_id_value = student_id.getText().toString();
		String passwd_value = passwd.getText().toString();
		if(student_id_value == null || student_id_value.trim().equals("") || passwd_value == null || passwd_value.trim().equals("")){
			dismissDialog();
			showToast("ѧ�Ż�����Ϊ��");
			return false;
		}
		return true;
	}
	
	//��¼
	private void doLogin(){
		if(checkEmpty())
		{
			showDialog("�����ӵ�ZJUWLAN,���ڵ�½");
			new OnSendListener(student_id,passwd,preferences).doConnect();
			return;
		}
	}
	
	//��鲢�������ߵ�½
	private void checkControlWifi(){
		state = cc_wifi.CheckState();
		Log.v("state", state + "");
		//WIfIΪ����״̬   WIFI_STATE_ENABLED 0x00000003
		if(state == 0x00000003)
		{
			Log.v("state","enable");
			if(cc_wifi.checkZJUWLAN())
			{
		    	main_handler.sendEmptyMessage(0x2000);
			}
			else
			{
				showDialog("�ȴ����ӵ�ZJUWLAN");
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
		//WIFIΪλ��״̬ WIFI_STATE_UNKNOWN 0x00000004
		else if(state == 0x00000004){
			main_handler.sendEmptyMessage(0x2005);
			return;
		}
		//WIFIΪ������״̬ WIFI_STATE_DISABLED 0x00000001
		else if(state == 0x00000001){
			Log.v("state","disabled");
			showDialog("���ڴ���������");
			cc_wifi.openWifi();
		}
		//����״̬��ȴ�����
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
	
	//���ӵ�ZJUWLAN
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
	
	//ʹ��Toast��ʾ���������Ϣ
	private void showToast(String info){
		Toast toast = Toast.makeText(this,info,Toast.LENGTH_LONG);
		toast.show();
	}
	
	//��ʼ���Ի���
	private void initDialog(){
		pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pDialog.setTitle("��ʾ");
		pDialog.setIndeterminate(false);
		pDialog.setCancelable(true);
		pDialog.setButton(-2,"ȡ��",new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which){
				cancel=true;
				dialog.cancel();//TODO
			}
		});
	}
	
	//��ʾ�Ի������ĶԻ����е�����
	private void showDialog(String info){
		if(!pDialog.isShowing()){
			Log.v("pDialog","no");
			pDialog.show();
		}
		pDialog.setMessage(info);
	}
	
	//�����Ի������ʾ
	private void dismissDialog(){
		if(pDialog.isShowing()){
			pDialog.dismiss();
		}
	}
	
}
