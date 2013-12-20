package com.codefortomorrow.crowdsourcing;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ProductActivity extends Activity
{
    private TextView productView;
    private Button cameraButton;

    private String productID;

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
            ProductActivity.this.startActivity(intent);
		}
		
	};
	
	

}
