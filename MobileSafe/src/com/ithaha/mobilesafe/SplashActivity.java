package com.ithaha.mobilesafe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import net.tsz.afinal.FinalHttp;
import net.tsz.afinal.http.AjaxCallBack;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import android.widget.Toast;

import com.ithaha.mobilesafe.utils.StreamTools;

/**
 * ��������ҳ��
 * ���ܣ�
 * 		1.���������ݿ�ļ���
 * 		2.�汾���µĲ�ѯ
 * 		3.���ͼ��Ľ���
 * @author hello
 *
 */
public class SplashActivity extends ActionBarActivity {

	protected static final int ENTER_HOME = 0;
	protected static final int SHOW_UPDATE_DIALOG = 1;
	protected static final int NETWORK_ERROR = 2;
	protected static final int JSON_ERROR = 3;
	private TextView tv_splash_version;
	protected String TAG = "SplashActivity";
	private TextView tv_update_info;
	
	private String description;
	private String apkurl;
	private SharedPreferences sp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		
		// ������ʾ�汾��Ϣ
		tv_splash_version = (TextView) findViewById(R.id.tv_splash_version);
		tv_splash_version.setText("�汾��:" + getVersion());
		tv_update_info = (TextView) findViewById(R.id.tv_update_info);
		
		sp = getSharedPreferences("config", MODE_PRIVATE);
		boolean update = sp.getBoolean("update", false);
		
		// ���ͼ��Ĵ���
		installShortCut();
		
		// �������ݿ�
		copyDB("address.db");
		copyDB("antivirus.db");
		
		if(update) {
			// �������
			checkUpdate(); 
		} else {
			// �Զ������ر�
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					enterHome();
				}
			}, 2000);
		}
		
		// ������ҳ�Ķ���Ч��
		AlphaAnimation aa = new AlphaAnimation(0.2f, 1.0f);
		aa.setDuration(500);
		
		findViewById(R.id.rl_root_splash).startAnimation(aa);
	}

	/**
	 * �������ͼ��
	 */
	private void installShortCut() {
		boolean shortcut = sp.getBoolean("shortcut", false);
		if(shortcut) {
			return ;
		}
		
		Intent intent = new Intent();
		intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		// ��ݷ�ʽ��Ҫ����3����Ҫ��Ϣ  1������ 2��ͼ�� 3����ʲô����
		// 1.����
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "�ֻ�С��ʿ");
		// 2.ͼ��
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
		// 3.ָ����ͼ
		Intent shortcutIntent = new Intent();
		// actionֻ��ָ��һ���� category����ָ�����
		shortcutIntent.setAction("android.intent.action.MAIN");
		// Add a new category to the intent.
		shortcutIntent.addCategory("android.intent.category.LAUNCHER");
		// 
		shortcutIntent.setClassName(getPackageName(), "com.ithaha.mobilesafe.SplashActivity");
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		
		// �ù㲥�ķ�ʽ������ͼ
		sendBroadcast(intent);
		
		Editor edit = sp.edit();
		edit.putBoolean("shortcut", true);
		edit.commit();
	}

	/**
	 * �������ݿ�
	 */
	private void copyDB(String filename) {
		// ֻ�ÿ���һ�Σ��Ͳ��ظ�������
		
		// path ��address.db������ݿ⿽����data/data/<����>/files/address.db
		try {
			File file = new File(getFilesDir(),filename);
			if(file.exists() && file.length() > 0) {
				// �Ѿ�������
				
			} else {
				// ��������
				InputStream is = getAssets().open(filename);
				FileOutputStream fos = new FileOutputStream(file);
				byte[] buffer = new byte[1024];
				int len = 0;
				while((len = is.read(buffer)) != -1) {
					fos.write(buffer, 0, len);
				}
				fos.close();
				is.close();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case SHOW_UPDATE_DIALOG:		// ��ʾ�����Ի���
				Log.i(TAG, "��ʾ�����ĶԻ���");
				showUpdateDialog();
				break;

			case ENTER_HOME:				// ������ҳ��
				enterHome();
				break;
				
			case NETWORK_ERROR:				// �������
				enterHome();
				Toast.makeText(getApplicationContext(), "�����쳣", 0).show();
				break;
				
			case JSON_ERROR:				// JSON��������
				enterHome();
				Toast.makeText(SplashActivity.this, "JSON��������", 0).show();
				break;
			default:
				break;
			}
		}
	};
	
	/**
	 * ���������Ի���
	 */
	private void showUpdateDialog() {
		// this = SplashActivity.this
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle("��������");
		builder.setMessage(description);
		
//		builder.setCancelable(false);		// ǿ������
		// ���������ط�����ֱ�ӽ�����ҳ
		builder.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				enterHome();
				dialog.dismiss();
			}
		});
		
		builder.setPositiveButton("��������", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// ����apk�������滻��װ
				if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					// sdcard ����
					
					// afinal
					FinalHttp finalhttp = new FinalHttp();
					finalhttp.download(apkurl, Environment.getExternalStorageDirectory().getAbsolutePath()+"/mobilesafe2.0.apk", new AjaxCallBack<File>() {

						@Override
						public void onFailure(Throwable t, int errorNo,
								String strMsg) {
							t.printStackTrace();
							Toast.makeText(getApplicationContext(), "����ʧ��", 1).show();
							super.onFailure(t, errorNo, strMsg);
						}

						@Override
						public void onLoading(long count, long current) {
							super.onLoading(count, current);
							tv_update_info.setVisibility(View.VISIBLE);
							// ��ǰ���ذٷֱ�
							
							tv_update_info.setText("���ؽ���:" + current + "/" + count);
						}

						@Override
						public void onSuccess(File t) {
							super.onSuccess(t);
							installAPK(t);
						}
						
					});
				} else {
					Toast.makeText(getApplicationContext(), "û��SD�����޷���ȷ����", 0).show();
				}
			}
		});
		
		builder.setNegativeButton("�´���˵", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// �Ի�����ʧ��������ҳ��
				dialog.dismiss();
				enterHome();
			}
		});
		
		builder.show();
	}
	
	/**
	 * ������ҳ��
	 */
	private void enterHome() {
		Intent intent = new Intent(this,HomeActivity.class);
		startActivity(intent);
		// �رյ�ǰҳ��
		finish();
	}
	
	/**
	 * ����Ƿ� ���°汾
	 */
	private void checkUpdate() {
		
		new Thread(){

			public void run() {
				// Return a new Message instance from the global pool.
				Message msg = Message.obtain();
//				Message msg = new Message();
				long startTime = System.currentTimeMillis();
				try {
					// URL
					URL url = new URL(getString(R.string.serverurl));
					
					// ����
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setReadTimeout(400);
					conn.setConnectTimeout(400);
					int responseCode = conn.getResponseCode();
					if(responseCode == 200) {
						// �����ɹ�
						InputStream is = conn.getInputStream();
						// ����ת�����ַ���
						String result = StreamTools.readFromStream(is);
						Log.i(TAG , "�����ɹ�: " + result);
						
						// json����
						JSONObject obj = new JSONObject(result);
						String version = (String) obj.get("version");
						description = (String) obj.get("description");
						apkurl = (String) obj.get("apkurl");
						
						// У���Ƿ����°汾
						if(getVersion().equals(version)) {
							// �汾һ�£�û���°汾,������ҳ��
							msg.what = ENTER_HOME;
							
						} else {
							// ���°汾�����������Ի���
							msg.what = SHOW_UPDATE_DIALOG;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					msg.what = NETWORK_ERROR;
				} catch (JSONException e) {
					e.printStackTrace();
					msg.what = JSON_ERROR;
				} finally {
					long endTime = System.currentTimeMillis();
					// ���˶���ʱ����
					long dTime = endTime - startTime;
					// ��Ҫͣ��2s
					if(dTime < 2000) {
						try {
							Thread.sleep(2000 - dTime);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					handler.sendMessage(msg);
				}
			};
		}.start();
		
	}
	
	/**
	 * �õ�Ӧ�ó���İ汾��Ϣ
	 */
	private String getVersion() {
		
		// ���������ֻ���apk Return PackageManager instance to find global package information. 
		PackageManager pm = getPackageManager();
		
		try {
			// �õ�ָ��apk�Ĺ����嵥�ļ�
			PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
			String versionName = packageInfo.versionName;
			return versionName;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * ��װapk
	 * @param t
	 */
	private void installAPK(File t) {
		Intent intent = new Intent();
		intent.setAction("android.intent.action.VIEW");
		intent.addCategory("android.intent.category.DEFAULT");
		intent.setDataAndType(Uri.fromFile(t), "application/vnd.android.package-archive");
		startActivity(intent);
	}
}