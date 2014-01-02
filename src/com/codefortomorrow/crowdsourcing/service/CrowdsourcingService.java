package com.codefortomorrow.crowdsourcing.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;

/**
 * Created by Lee on 2014/1/1.
 */
public class CrowdsourcingService extends Service
{
    private static final String TAG = "CSService";

    private static final String CS_SERVICE_ACTION = "OpenEats.CrowdSourcingApp";
    private static final String CS_SERVICE_CONTROLTYPE = "controlType";

    private static final String EXISTBARCODELIST = "http://openeatscs.yuchuan1.cloudbees.net/api/1.0/getBarcodes";
    private static final String ACCEPTUPLOAD     = "http://openeatscs.yuchuan1.cloudbees.net/api/1.0/acceptUpload/";
    private static final String UPLOADPHOTO      = "http://openeatscs.yuchuan1.cloudbees.net/api/1.0/upload";

    private HttpClient   client;
    private HttpGet      getBarcodeList;
    private HttpGet      getAcceptUpload;
    private HttpPost     postPhoto;
    private HttpResponse response;
    private HttpEntity   resEntity;

    private BroadcastReceiver broadcast = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            intentHandler(intent);
        }
    };

    @Override
    public void onCreate()
    {
        super.onCreate();
        client = new DefaultHttpClient();
        getBarcodeList = new HttpGet(EXISTBARCODELIST);
        postPhoto = new HttpPost(UPLOADPHOTO);
        this.registerReceiver(broadcast, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        this.registerReceiver(broadcast, new IntentFilter("OpenEats.CrowdSourcingApp"));
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        this.unregisterReceiver(broadcast);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        onStart(intent, startId);
        intentHandler(intent);
//        return super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT;
    }

    private void intentHandler(Intent intent)
    {
        String action = intent.getAction();
        Bundle bundle = intent.getExtras();

        ConnectivityManager connect = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connect.getActiveNetworkInfo();

        boolean networkIsAvailable = false;
        if (networkInfo != null)
        {
            networkIsAvailable = networkInfo.isAvailable();
        }
        Log.d(TAG, "Detect Network" + networkIsAvailable);

        if (action.equals(CS_SERVICE_ACTION))
        {
            char controlType = bundle.getChar(CS_SERVICE_CONTROLTYPE);
            switch (controlType)
            {
                case 'B':
                    if (networkIsAvailable)
                    {
                        try
                        {
                            getBarcodeList = new HttpGet(EXISTBARCODELIST);
                            response = client.execute(getBarcodeList);
                            resEntity = response.getEntity();
                        }
                        catch (Exception e)
                        {
                            Log.d(TAG, controlType + " error: "+ e.toString());
                        }
                    }
                    break;
                case 'A':
                    if (networkIsAvailable)
                    {
                        try
                        {
                            String barcode = bundle.getString("barcode");
                            getAcceptUpload = new HttpGet(ACCEPTUPLOAD + barcode);
                            response = client.execute(getAcceptUpload);
                            resEntity = response.getEntity();
                        }
                        catch (Exception e)
                        {
                            Log.d(TAG, controlType + " error: "+ e.toString());
                        }
                    }
                    break;
                case 'U':
                    if (networkIsAvailable)
                    {

                    }
                    break;
                case 'S':

                    break;
                default:

                    break;
            }
        }
        else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE"))
        {

        }
    }

    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
