package com.codefortomorrow.crowdsourcing.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.ResponseHandler;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.entity.mime.HttpMultipartMode;
import ch.boye.httpclientandroidlib.entity.mime.MultipartEntity;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.util.EntityUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.openeatsCS.app.model.*;
import de.greenrobot.dao.query.QueryBuilder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

/**
 * Created by Lee on 2014/1/1.
 */
public class CrowdsourcingService extends Service
{
    private static final String TAG = "CSService";

    private static final String CS_SERVICE_ACTION      = "OpenEats.CrowdSourcingApp";
    private static final String CS_SERVICE_CONTROLTYPE = "controlType";

    private static final String EXISTBARCODELIST = "http://openeatscs.yuchuan1.cloudbees.net/api/1.0/getBarcodes";
    private static final String ACCEPTUPLOAD     = "http://openeatscs.yuchuan1.cloudbees.net/api/1.0/acceptUpload/";
    private static final String UPLOADPHOTO      = "http://openeatscs.yuchuan1.cloudbees.net/api/1.0/upload";

    // Restful Api
    private HttpClient   client;
    private HttpGet      getBarcodeList;
    private HttpGet      getAcceptUpload;
    private HttpPost     postPhoto;
    private HttpResponse response;
    private HttpEntity   resEntity;

    private boolean checkGetBarcodeList;
    private boolean checkGetAcceptUpload;
    private boolean checkPostPhoto;

    // green DAO
    private DaoMaster.DevOpenHelper devOpenHelper;
    private SQLiteDatabase          db;
    private DaoMaster               daoMaster;
    private DaoSession              daoSession;
    private BarcodeDao              barcodeDao;
    private HistoryDao              historyDao;

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

        devOpenHelper = new DaoMaster.DevOpenHelper(CrowdsourcingService.this, "openeatsCS-db", null);
        db = devOpenHelper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        barcodeDao = daoSession.getBarcodeDao();
        historyDao = daoSession.getHistoryDao();

        checkGetBarcodeList = false;
        checkGetAcceptUpload = false;
        checkPostPhoto = false;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        this.unregisterReceiver(broadcast);

        daoSession.clear();
        db.close();
        devOpenHelper.close();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        onStart(intent, startId);
        intentHandler(intent);
//        return super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT;
    }

    private class PhotoResponseHandler implements ResponseHandler<Object>
    {
        @Override
        public Object handleResponse(HttpResponse httpResponse) throws IOException
        {
            HttpEntity resEntity = httpResponse.getEntity();
            String response = EntityUtils.toString(resEntity);

            if (response.contains("Success"))
            {

            }
            else
            {
                Log.d(TAG, "Error");

            }
            Log.d(TAG, response);
            return null;
        }
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
                        // get barcode list
                        getBarcodeList();
                    }
                    break;
                case 'A':
                    if (networkIsAvailable)
                    {
                        // check server accept uploading or not
                        acceptUpload(bundle.getString("barcode"));
                    }
                    break;
                case 'U':
                    if (networkIsAvailable)
                    {
                        // upload photos
                        uploadPhotos();
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

    private void getBarcodeList()
    {
        if (!checkGetBarcodeList)
        {
            checkGetBarcodeList = true;
            Log.d(TAG, "get barcode list.");
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {

                        getBarcodeList = new HttpGet(EXISTBARCODELIST);
                        response = client.execute(getBarcodeList);
                        resEntity = response.getEntity();
                        String content = EntityUtils.toString(resEntity);

                        Log.d(TAG, "get barcode list response " + content);

                        JsonParser parser = new JsonParser();
                        JsonArray contentJson = parser.parse(content).getAsJsonArray();
                        Type listType = new TypeToken<List<String>>(){}.getType();
                        List<String> barcodeList = new Gson().fromJson(contentJson, listType);

                        Log.d(TAG, "json to barcode list size " + barcodeList.size());

                        for(int i = 0; i < barcodeList.size(); i++)
                        {
                            Barcode newBarcode = new Barcode();
                            newBarcode.setBarcode(barcodeList.get(i));
                            newBarcode.setName("");
                            newBarcode.setUpload(true);
                            newBarcode.setUpdate(true);
                            newBarcode.setFinish(true);            // product all done
                            barcodeDao.insertOrReplace(newBarcode);

                            Date currentDate = new Date();

                            History history = new History();
                            history.setBarcode(newBarcode);
                            history.setUpdated_at(currentDate);
                            historyDao.insert(history);

                        }
                        checkGetBarcodeList = false;
                    }
                    catch (Exception e)
                    {
                        checkGetBarcodeList = false;
                        Log.d(TAG,"get barcode list error: "+ e.toString());
                    }
                }
            }).start();
        }
    }

    private void uploadPhotos()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    postPhoto = new HttpPost(UPLOADPHOTO);
                    MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);


                    client.execute(postPhoto, new PhotoResponseHandler());
                }
                catch (Exception e)
                {
                    Log.d(TAG, "upload photo error: " + e.toString());
                }
            }
        }).start();
    }

    private void acceptUpload(final String barcode)
    {
        if (!checkGetAcceptUpload)
        {
            checkGetAcceptUpload = true;
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        getAcceptUpload = new HttpGet(ACCEPTUPLOAD + barcode);
                        response = client.execute(getAcceptUpload);
                        resEntity = response.getEntity();
                        String content = EntityUtils.toString(resEntity);

                        JsonParser parser = new JsonParser();
                        JsonObject contentJson = parser.parse(content).getAsJsonObject();
                        boolean result = contentJson.get("UploadAllowed").getAsBoolean();

                        QueryBuilder qb = barcodeDao.queryBuilder();
                        qb.where(BarcodeDao.Properties.Barcode.eq(barcode));
                        List barcodeList = qb.list();
                        Barcode barcodetmp;
                        if (barcodeList.size() > 0)
                        {
                            barcodetmp = (Barcode) barcodeList.get(0);
                        }
                        else
                        {
                            barcodetmp = new Barcode();
                            barcodetmp.setBarcode(barcode);
                        }

                        barcodetmp.setFinish(!result);             // set UploadAllowed ? false:true
                        barcodetmp.setUpdate(true);

                        if(barcodeList.size() > 0)
                        {
                            barcodeDao.update(barcodetmp);
                        }
                        else
                        {
                            barcodeDao.insert(barcodetmp);
                        }

                        Date currentDate = new Date();

                        History history = new History();
                        history.setBarcode(barcodetmp);
                        history.setUpdated_at(currentDate);
                        historyDao.insert(history);

                        checkGetAcceptUpload = false;
                    }
                    catch (Exception e)
                    {
                        checkGetAcceptUpload = false;
                        Log.d(TAG, "accept Upload error: "+ e.toString());
                    }
                }
            }).start();
        }
    }

    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
