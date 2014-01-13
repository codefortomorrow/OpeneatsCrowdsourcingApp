package com.codefortomorrow.crowdsourcing;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
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
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import com.google.zxing.client.android.CaptureActivity;
import com.openeatsCS.app.model.*;
import de.greenrobot.dao.query.QueryBuilder;

import java.util.List;
import java.util.UUID;

public class ProductActivity extends Activity
{
    private TextView productView;
    private Button   cameraButton;

    private String productID;
    private String contentUUID;
    private boolean productUploadAllowed;

    // green DAO
    private DaoMaster.DevOpenHelper devOpenHelper;
    private SQLiteDatabase          db;
    private DaoMaster               daoMaster;
    private DaoSession              daoSession;
    private BarcodeDao              barcodeDao;
    private HistoryDao              historyDao;

    private static final String DB_OPENEATS       = "openeatsCS-db";

    private static final String TAG       = "Lee";
    private static final String PREF_NAME = "OPENEATS";
    private static final String PREF_KEY  = "UUID";

    @Override
    protected void onDestroy()
    {
        // TODO Auto-generated method stub

        super.onDestroy();

        daoSession.clear();
        db.close();
        devOpenHelper.close();
    }

    @Override
    protected void onPause()
    {
        // TODO Auto-generated method stub
        super.onPause();
//        this.unregisterReceiver(broadcast);
    }

    @Override
    protected void onResume()
    {
        // TODO Auto-generated method stub
        super.onResume();
//        this.registerReceiver(broadcast, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
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

        // load preferences
        SharedPreferences sp = getSharedPreferences(PREF_NAME, Context.MODE_WORLD_WRITEABLE);
        contentUUID = sp.getString(PREF_KEY, "");
        if(contentUUID.equals(""))
        {
            contentUUID = UUID.randomUUID().toString();
            sp.edit().putString(PREF_KEY, contentUUID).commit();
        }
        Log.d(TAG, "uuid: " + contentUUID);

        devOpenHelper = new DaoMaster.DevOpenHelper(ProductActivity.this, DB_OPENEATS, null);
        db = devOpenHelper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        barcodeDao = daoSession.getBarcodeDao();

        QueryBuilder qb = barcodeDao.queryBuilder();
        qb.where(BarcodeDao.Properties.Barcode.eq(productID));
        List barcodeList = qb.list();

        productUploadAllowed = true;
        if (barcodeList.size() > 0)
        {
            Barcode barcodetmp = (Barcode) barcodeList.get(0);
            productUploadAllowed = !barcodetmp.getFinish();
            Log.d(TAG, "barcode: " + barcodetmp.getBarcode() + " UploadAllowed: " + productUploadAllowed);
        }

        if (!productUploadAllowed)
        {
            productView.setText(String.format(getResources().getString(R.string.msg_product_finish), productID));
            cameraButton.setText(R.string.button_product_finish);
        }
        else
        {
            cameraButton.setText(R.string.button_product_start_camera);
        }
	}
	
//	private BroadcastReceiver broadcast =  new BroadcastReceiver()
//	{
//		@Override
//		public void onReceive(Context context, Intent intent)
//		{
//			// TODO Auto-generated method stub
//			// Detect the network
//			ConnectivityManager connect = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//			NetworkInfo networkInfo = connect.getActiveNetworkInfo();
//			if(networkInfo != null)
//			{
//				if(networkInfo.isAvailable())
//				{
//					cameraButton.setClickable(true);
//					cameraButton.setText(R.string.button_product_start_camera);
//				}
//				else
//				{
//					cameraButton.setClickable(false);
//					cameraButton.setText(R.string.button_product_no_network);
//				}
//			}
//			else
//			{
//				cameraButton.setClickable(false);
//				cameraButton.setText(R.string.button_product_no_network);
//			}
//		}
//	};

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
            if (productUploadAllowed)
            {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                intent.setClassName(ProductActivity.this, CrowdsourcingActivity.class.getName());
                intent.putExtra("product_ID", productID);
                intent.putExtra("UUID", contentUUID);
                ProductActivity.this.startActivity(intent);
            }
            else
            {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setClassName(ProductActivity.this, CaptureActivity.class.getName());
                ProductActivity.this.startActivity(intent);
                ProductActivity.this.finish();
            }

        }

    };


}
