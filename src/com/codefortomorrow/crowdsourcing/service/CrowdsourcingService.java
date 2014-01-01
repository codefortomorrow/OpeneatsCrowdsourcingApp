package com.codefortomorrow.crowdsourcing.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
    private static final String TAG                 = "CSService";

    private static final String EXISTBARCODELIST    = "http://openeatscs.yuchuan1.cloudbees.net/api/1.0/getBarcodes";
    private static final String ACCEPTUPLOAD        = "http://openeatscs.yuchuan1.cloudbees.net/api/1.0/acceptUpload/";
    private static final String UPLOADPHOTO         = "http://openeatscs.yuchuan1.cloudbees.net/api/1.0/upload";


    private HttpClient client;
    private HttpGet getBarcodeList;
    private HttpGet getAcceptUpload;
    private HttpPost postPhoto;
    private HttpResponse response;
    private HttpEntity resEntity;

    @Override
    public void onCreate()
    {
        super.onCreate();
        client = new DefaultHttpClient();
        getBarcodeList = new HttpGet(EXISTBARCODELIST);
        postPhoto = new HttpPost(UPLOADPHOTO);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Bundle bundle = intent.getExtras();
        char controlType = bundle.getChar("controlType");

        ConnectivityManager connect = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connect.getActiveNetworkInfo();

        boolean checkNetwork = false;
        if (networkInfo != null)
        {
            if (networkInfo.isAvailable())
            {
                checkNetwork = true;
            }
        }

        switch (controlType)
        {
            case 'B':
                if (checkNetwork)
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
                if (checkNetwork)
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
                if (checkNetwork)
                {

                }
                break;
            case 'S':

                break;
            default:

                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
