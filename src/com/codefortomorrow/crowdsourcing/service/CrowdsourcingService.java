package com.codefortomorrow.crowdsourcing.service;

import android.app.Service;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
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
import ch.boye.httpclientandroidlib.entity.mime.content.ByteArrayBody;
import ch.boye.httpclientandroidlib.entity.mime.content.StringBody;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.util.EntityUtils;
import com.codefortomorrow.crowdsourcing.R;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.openeatsCS.app.model.*;
import de.greenrobot.dao.query.QueryBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by Lee on 2014/1/1.
 */
public class CrowdsourcingService extends Service
{
    private static final String TAG = "CSService";

    private static final String CS_INTENT_CONNECTIVITY_CHANGE        = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String CS_INTENT_OPENEATS_CROWDSOURCING_APP = "OpenEats.CrowdSourcingApp";
    private static final String CS_SERVICE_CONTROLTYPE               = "controlType";

    private static final String EXISTBARCODELIST = "http://openeatscs.yuchuan1.cloudbees.net/api/1.0/getBarcodes";
    private static final String ACCEPTUPLOAD     = "http://openeatscs.yuchuan1.cloudbees.net/api/1.0/acceptUpload/";
    private static final String UPLOADPHOTO      = "http://openeatscs.yuchuan1.cloudbees.net/api/1.0/upload";

    private static final String DB_OPENEATS         = "openeatsCS-db";
    private static final String LOG_UPLOADALLOWED   = "Update Barcode: %s, UploadAllowed: %b";
    private static final String LOG_UPLOADPHOTO     = "Upload Barcode: %s, Upload Photo: %s";
    private static final String LOG_UPLOADPHOTODONE = "Upload Barcode: %s, Upload Photo %s";

    private static final String SD_STORAGE = "/openEats";

    private static final String PREF_NAME = "OPENEATS";
    private static final String PREF_KEY  = "UUID";

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

    private List<String> photoUploadList;
    private String uploadBarcode;
    private String contentUUID;

    // green DAO
    private DaoMaster.DevOpenHelper devOpenHelper;
    private SQLiteDatabase          db;
    private DaoMaster               daoMaster;
    private DaoSession              daoSession;
    private BarcodeDao              barcodeDao;
    private HistoryDao              historyDao;

    // check SD
    File sdCardDirectory = Environment.getExternalStorageDirectory();

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
        photoUploadList = new ArrayList<String>();

        this.registerReceiver(broadcast, new IntentFilter(CS_INTENT_CONNECTIVITY_CHANGE));
        this.registerReceiver(broadcast, new IntentFilter(CS_INTENT_OPENEATS_CROWDSOURCING_APP));

        devOpenHelper = new DaoMaster.DevOpenHelper(CrowdsourcingService.this, DB_OPENEATS, null);
        db = devOpenHelper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        barcodeDao = daoSession.getBarcodeDao();
        historyDao = daoSession.getHistoryDao();

        checkGetBarcodeList = false;
        checkGetAcceptUpload = false;
        checkPostPhoto = false;

        // load preferences
        SharedPreferences sp = getSharedPreferences(PREF_NAME, Context.MODE_WORLD_WRITEABLE);
        contentUUID = sp.getString(PREF_KEY, "");
        if(contentUUID.equals(""))
        {
            contentUUID = UUID.randomUUID().toString();
            sp.edit().putString(PREF_KEY, contentUUID).commit();
        }
        Log.d(TAG, "uuid: " + contentUUID);

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

            QueryBuilder queryBuilder = barcodeDao.queryBuilder();
            queryBuilder.where(BarcodeDao.Properties.Barcode.eq(uploadBarcode));
            Barcode barcodetmp = (Barcode) queryBuilder.list().get(0);

            Date currentDate = new Date();

            History history = new History();
            history.setBarcode(barcodetmp);
            history.setTime(currentDate);

            if (response.contains("Success"))
            {
                barcodetmp.setUpload(true);
                history.setLog(String.format(LOG_UPLOADPHOTODONE, barcodetmp, "Success"));

                Log.d(TAG, "Success");
                photoUploadList.remove(photoUploadList.indexOf(uploadBarcode));
                deletePhoto(uploadBarcode);

            }
            else
            {
                photoUploadList.remove(photoUploadList.indexOf(uploadBarcode));
                photoUploadList.add(uploadBarcode);
                Log.d(TAG, "Error");
                barcodetmp.setUpload(false);
                history.setLog(String.format(LOG_UPLOADPHOTODONE, barcodetmp, "Fail"));
            }
            barcodeDao.update(barcodetmp);
            historyDao.insert(history);

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

        if (action.equals(CS_INTENT_OPENEATS_CROWDSOURCING_APP))
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
                    if (networkIsAvailable)
                    {
                        photoUploadList.add(bundle.getString("barcode"));
                        getStoragePhotos();
                    }
                    break;
                default:

                    break;
            }
        }
        else if (action.equals(CS_INTENT_CONNECTIVITY_CHANGE))
        {
            Log.d(TAG, "Network connect. photo size:" + photoUploadList.size());
            if (photoUploadList.size() > 0)
            {
                getStoragePhotos();
            }
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
                            QueryBuilder queryBuilder = barcodeDao.queryBuilder();
                            queryBuilder.where(BarcodeDao.Properties.Barcode.eq(barcodeList.get(i)));
                            List dbBarcodeList = queryBuilder.list();
                            Barcode newBarcode;
                            if (dbBarcodeList.size() > 0)           // check barcode exist or not
                            {
                                newBarcode = (Barcode) dbBarcodeList.get(0);
                            }
                            else
                            {
                                newBarcode = new Barcode();
                                newBarcode.setBarcode(barcodeList.get(i));
                            }

//                            newBarcode.setName("");
                            newBarcode.setUpload(true);
                            newBarcode.setUpdate(true);
                            newBarcode.setFinish(true);            // product all done

                            if (dbBarcodeList.size() > 0)
                            {
                                barcodeDao.update(newBarcode);
                            }
                            else
                            {
                                barcodeDao.insert(newBarcode);
                            }

                            Date currentDate = new Date();

                            History history = new History();
                            history.setBarcode(newBarcode);
                            history.setTime(currentDate);
                            history.setLog(String.format(LOG_UPLOADALLOWED, barcodeList.get(i), false));
                            historyDao.insert(history);

                            Log.d(TAG, String.format(LOG_UPLOADALLOWED, barcodeList.get(i), false) + " Time: " + history.getTime());
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
        if (!checkPostPhoto)
        {
            checkPostPhoto = true;
            Log.d(TAG, "Uploading photo.");
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        uploadBarcode = photoUploadList.get(0);

                        getAcceptUpload = new HttpGet(ACCEPTUPLOAD + uploadBarcode);
                        response = client.execute(getAcceptUpload);
                        resEntity = response.getEntity();
                        String content = EntityUtils.toString(resEntity);

                        JsonParser parser = new JsonParser();
                        JsonObject contentJson = parser.parse(content).getAsJsonObject();
                        boolean result = contentJson.get("UploadAllowed").getAsBoolean();

                        Log.d(TAG, "Uploding Check database. " + uploadBarcode);

                        QueryBuilder queryBuilder = barcodeDao.queryBuilder();
                        queryBuilder.where(BarcodeDao.Properties.Barcode.eq(uploadBarcode));
                        List barcodeList = queryBuilder.list();

                        Barcode barcodetmp;

                        if (barcodeList.size() > 0)
                        {
                            barcodetmp = (Barcode) barcodeList.get(0);
                        }
                        else
                        {
                            barcodetmp = new Barcode();
                        }
                        barcodetmp.setFinish(!result);
                        barcodeDao.update(barcodetmp);

                        Log.d(TAG, "Store History");
                        Date currentDate = new Date();

                        History history = new History();
                        history.setBarcode(barcodetmp);
                        history.setTime(currentDate);
                        history.setLog(String.format(LOG_UPLOADALLOWED, barcodetmp, result));
                        historyDao.insert(history);

                        if(result)
                        {
                            ByteArrayOutputStream out1 = new ByteArrayOutputStream();
                            Bitmap bitmap1 = BitmapFactory.decodeFile(String.format("%s%s/%s_1.jpg", sdCardDirectory.getAbsolutePath(), SD_STORAGE, uploadBarcode));
                            bitmap1.compress(Bitmap.CompressFormat.JPEG, 100, out1);
                            bitmap1.recycle();

                            ByteArrayOutputStream out2 = new ByteArrayOutputStream();
                            Bitmap bitmap2 = BitmapFactory.decodeFile(String.format("%s%s/%s_2.jpg", sdCardDirectory.getAbsolutePath(), SD_STORAGE, uploadBarcode));
                            bitmap2.compress(Bitmap.CompressFormat.JPEG, 100, out2);
                            bitmap2.recycle();

                            ByteArrayOutputStream out3 = new ByteArrayOutputStream();
                            Bitmap bitmap3 = BitmapFactory.decodeFile(String.format("%s%s/%s_3.jpg", sdCardDirectory.getAbsolutePath(), SD_STORAGE, uploadBarcode));
                            bitmap3.compress(Bitmap.CompressFormat.JPEG, 100, out3);
                            bitmap3.recycle();

                            Log.d(TAG, "Connecting server. Uploding " + uploadBarcode);
                            postPhoto = new HttpPost(UPLOADPHOTO);
                            MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

                            entity.addPart("app_user_id", new StringBody(contentUUID));
                            entity.addPart("barcode", new StringBody(uploadBarcode));
                            entity.addPart("pic0", new ByteArrayBody(out1.toByteArray(), "file", "pic0.jpg"));
                            entity.addPart("pic1", new ByteArrayBody(out2.toByteArray(), "file", "pic1.jpg"));
                            entity.addPart("pic2", new ByteArrayBody(out3.toByteArray(), "file", "pic2.jpg"));
                            postPhoto.setEntity(entity);
                            client.execute(postPhoto, new PhotoResponseHandler());
                        }
                        else
                        {
                            photoUploadList.remove(photoUploadList.indexOf(uploadBarcode));
                            deletePhoto(uploadBarcode);
                        }


                        checkPostPhoto = false;
                        if (photoUploadList.size() > 0)
                        {
                            Log.d(TAG, "uploading next photo.");
                            getStoragePhotos();
                        }
                        else
                        {
                            Log.d(TAG, "upload done.");
                        }
                    }
                    catch (Exception e)
                    {
                        checkPostPhoto = false;
                        Log.d(TAG, "upload photo error: " + e.toString());
                    }
                }
            }).start();
        }
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
                        if (barcodeList.size() > 0)                 // check barcode exist or not
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
                        history.setTime(currentDate);
                        history.setLog(String.format(LOG_UPLOADALLOWED, barcode, result));
                        historyDao.insert(history);

                        checkGetAcceptUpload = false;
                        List historyList = barcodetmp.getHistoryList();
                        for (int i = 0; i < historyList.size(); i++)
                        {
                            History historytmp = (History) historyList.get(i);
                            Log.d(TAG, historytmp.getLog() + " Time: " + historytmp.getTime());
                        }
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

    private void getStoragePhotos()
    {

        if (!checkPostPhoto && (photoUploadList.size() > 0))
        {
            uploadPhotos();
        }

    }

    private void deletePhoto(String barcode)
    {
        for(int i = 1; i <= 3; i++)
        {
            File file = new File(sdCardDirectory.getAbsolutePath() + SD_STORAGE, barcode + "_" + i + ".jpg");
            file.delete();
        }
        Log.d(TAG, "delete photos sucess " + barcode);
    }

    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
