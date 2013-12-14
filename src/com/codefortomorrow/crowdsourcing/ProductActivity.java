package com.codefortomorrow.crowdsourcing;

import com.example.cameratest.R;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ProductActivity extends Activity
{
	TextView productView;
	Button cameraButton;

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
		Intent intent = getIntent();
		String productID = intent.getStringExtra("product_ID");
		productView.setText(productID);
	}

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
			ProductActivity.this.startActivity(intent);
		}
		
	};

}
