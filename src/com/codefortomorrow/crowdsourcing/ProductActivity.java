package com.codefortomorrow.crowdsourcing;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.Tag;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.UUID;

public class ProductActivity extends Activity
{
    private TextView productView;
    private Button cameraButton;

    private String productID;
    private String contentUUID;

    private static final String TAG = "Lee";
    private static final String PREF_NAME = "OPENEATS";
    private static final String PREF_KEY = "UUID";

	@Override
	protected void onDestroy()
	{
		// TODO Auto-generated method stub
		
		super.onDestroy();
	}

	@Override
	protected void onPause()
	{
		// TODO Auto-generated method stub
		super.onPause();
		this.unregisterReceiver(broadcast);
	}

	@Override
	protected void onResume()
	{
		// TODO Auto-generated method stub
		super.onResume();
		this.registerReceiver(broadcast, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_product);
		findView();
		setListener();
		loadData();
		
	}
	
	private void findView()
	{
		productView = (TextView) findViewById(R.id.product_ID);
		cameraButton = (Button) findViewById(R.id.button_camera);
	}
	
	private void setListener()
	{
		cameraButton.setOnClickListener(btn_camera_click);
	}
	
	private void loadData()
	{
		//獲取 Scan Barcode
		Intent intent = getIntent();
		productID = intent.getStringExtra("product_ID");
		productView.setText(productID);

        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        for (Account account: accounts)
        {
            Log.d(TAG, account.name);
        }

        SharedPreferences sp = getSharedPreferences(PREF_NAME, Context.MODE_WORLD_WRITEABLE);
        contentUUID = sp.getString(PREF_KEY, "");
        if(contentUUID.equals(""))
        {
            contentUUID = UUID.randomUUID().toString();
            sp.edit().putString(PREF_KEY, contentUUID).commit();
        }
        Log.d(TAG, "uuid: " + contentUUID);
	}
	
	private BroadcastReceiver broadcast =  new BroadcastReceiver() 
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			// TODO Auto-generated method stub
			// Detect the network
			ConnectivityManager connect = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connect.getActiveNetworkInfo();
			if(networkInfo != null)
			{
				if(networkInfo.isAvailable())
				{
					cameraButton.setClickable(true);
					cameraButton.setText(R.string.button_product_start_camera);
				}
				else
				{
					cameraButton.setClickable(false);
					cameraButton.setText(R.string.button_product_no_network);
				}
			}
			else
			{
				cameraButton.setClickable(false);
				cameraButton.setText(R.string.button_product_no_network);
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.product, menu);
		return true;
	}
	
	private Button.OnClickListener btn_camera_click = new Button.OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			// TODO Auto-generated method stub
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.setClassName(ProductActivity.this, CrowdsourcingActivity.class.getName());
            intent.putExtra("product_ID",productID);
            intent.putExtra("UUID", contentUUID);
            ProductActivity.this.startActivity(intent);
		}
		
	};
	
	

}
