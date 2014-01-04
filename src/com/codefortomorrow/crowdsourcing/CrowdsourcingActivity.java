package com.codefortomorrow.crowdsourcing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.ResponseHandler;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.entity.mime.HttpMultipartMode;
import ch.boye.httpclientandroidlib.entity.mime.MultipartEntity;
import ch.boye.httpclientandroidlib.entity.mime.content.ByteArrayBody;
import ch.boye.httpclientandroidlib.entity.mime.content.StringBody;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.util.EntityUtils;
import com.openeatsCS.app.model.*;


@SuppressLint("NewApi")
public class CrowdsourcingActivity extends Activity implements SurfaceHolder.Callback {
	
	private int screenHeight;
    private int screenWidth;
	
	private int photoNum = 0;
    private int compressNum = 0;
    private String productID;
    private String contentUUID;

    private ImageView ivCoverUp, ivCoverDown;
	private SurfaceHolder surfaceHolder;
    private SurfaceView svCameraPreview;
    private Button btnShoot, btnBack;
    private ImageView ivShoot1, ivShoot2, ivShoot3;
    private ImageView      ivTitle;
    private ImageView      ivSamplePhoto;
    private Camera         mCamera;
    private ProgressDialog progressDialog;
    private ProgressBar    progressBarBitmap1;
    private ProgressBar    progressBarBitmap2;
    private ProgressBar    progressBarBitmap3;
    private Bitmap         bmpRaw;

    //ByteArrayOutputStreams for uploading to server
    private ByteArrayOutputStream out1 = new ByteArrayOutputStream();
    private ByteArrayOutputStream out2 = new ByteArrayOutputStream();
    private ByteArrayOutputStream out3 = new ByteArrayOutputStream();

    // green dao
    private DaoMaster.DevOpenHelper devOpenHelper;
    private SQLiteDatabase          db;
    private DaoMaster               daoMaster;
    private DaoSession              daoSession;
    private BarcodeDao              barcodeDao;
    private HistoryDao              historyDao;

    //'T'hh:mm:ss.SSS'Z'"); // example: 2013-11-11T12:21:48.033Z
//    final private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

    final private String TAG  = "Lee";
    final private String TAGG = "mmpud";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        //use fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_crowdsourcing);
        //set screen orientation
        setRequestedOrientation(0);
        //find screen size
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenHeight = size.y;
        screenWidth = size.x;
        Log.d(TAGG, "phone screen: " + screenWidth + ", " + screenHeight);
        //set up the upper and down cover
        ivCoverUp = (ImageView) findViewById(R.id.iv_cover_up);
        ivCoverDown = (ImageView) findViewById(R.id.iv_cover_down);
        ivCoverUp.getLayoutParams().width = (screenWidth - screenHeight) / 2;
        ivCoverDown.getLayoutParams().width = (screenWidth - screenHeight) / 2;
        //set up buttons
        btnShoot = (Button) findViewById(R.id.btn_shoot);
        btnBack = (Button) findViewById(R.id.btn_back);
        btnShoot.setOnClickListener(myOnClickListener);
        btnBack.setOnClickListener(myOnClickListener);

        ivShoot1 = (ImageView) findViewById(R.id.iv_shoot1);
        ivShoot2 = (ImageView) findViewById(R.id.iv_shoot2);
        ivShoot3 = (ImageView) findViewById(R.id.iv_shoot3);
        ivTitle = (ImageView) findViewById(R.id.iv_title);

        ivSamplePhoto = (ImageView) findViewById(R.id.iv_sample_photo);
        progressBarBitmap1 = (ProgressBar) findViewById(R.id.progressBar_bitmap1);
        progressBarBitmap2 = (ProgressBar) findViewById(R.id.progressBar_bitmap2);
        progressBarBitmap3 = (ProgressBar) findViewById(R.id.progressBar_bitmap3);

        svCameraPreview = (SurfaceView) findViewById(R.id.sv_camera_preview);
        surfaceHolder = svCameraPreview.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(this);

        loadData();

    }

    private void loadData()
    {
        Intent intent = getIntent();
        productID = intent.getStringExtra("product_ID");
        contentUUID = intent.getStringExtra("UUID");

        // setup db
        devOpenHelper = new DaoMaster.DevOpenHelper(CrowdsourcingActivity.this, "openeatsCS-db", null);
        db = devOpenHelper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        barcodeDao = daoSession.getBarcodeDao();
        historyDao = daoSession.getHistoryDao();

        setTestDataBase();
    }

    private void setTestDataBase()
    {
        Barcode barcode = new Barcode();
        barcode.setBarcode("123456");
        barcode.setName("drink");
        barcode.setLoc_photo1("123");
        barcode.setLoc_photo2("456");
        barcode.setLoc_photo3("789");
        barcode.setFinish(false);
        barcode.setUpdate(false);
        barcode.setUpdate(false);
        barcodeDao.insert(barcode);
        Log.d(TAG, "add new barcode: " + barcode.getBarcode());

        // 讀取現在日期
        Date currentDate = new Date();

        // 設定 + one hour to upload
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.HOUR, +1);

        History history = new History();
        history.setBarcode(barcode);
        history.setCreated_at(currentDate);
        history.setUpdated_at(calendar.getTime());
        historyDao.insert(history);
        Log.d(TAG, "add history: " + history.getCreated_at());

    }

    private OnClickListener myOnClickListener = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (v.getId() == R.id.btn_shoot)
            {
                if (photoNum < 3)
                {
                    mCamera.autoFocus(afcb);
                }
                else
                {
                    updatingPics();
                }
            }
            else if (v.getId() == R.id.btn_back)
            {
                switch (photoNum)
                {
                    case 1:
                        ivShoot1.setImageResource(R.drawable.shoot1);
                        ivTitle.setBackgroundColor(Color.parseColor("#FF95B9C7"));
                        ivSamplePhoto.setBackgroundResource(R.drawable.photo_sample1);
                        ivTitle.setImageResource(R.drawable.title_step1);
                        compressNum--;
                        photoNum--;
                        break;
                    case 2:
                        ivShoot2.setImageResource(R.drawable.shoot2);
                        ivTitle.setBackgroundColor(Color.parseColor("#FF3090C7"));
                        ivSamplePhoto.setBackgroundResource(R.drawable.photo_sample2);
                        ivTitle.setImageResource(R.drawable.title_step2);
                        btnShoot.setText("");
                        compressNum--;
                        photoNum--;
                        break;
                    case 3:
                        ivShoot3.setImageResource(R.drawable.shoot3);
                        ivTitle.setBackgroundColor(Color.parseColor("#FF2B60DE"));
                        ivSamplePhoto.setBackgroundResource(R.drawable.photo_sample3);
                        ivTitle.setImageResource(R.drawable.title_step3);
                        photoNum--;
                        compressNum--;
                        btnShoot.setBackgroundResource(R.drawable.btn_shoot);
                        btnShoot.setText("o");
                        break;
                    default:
                        break;
                }
            }
        }
    };

//    private Response.Listener<String> updatePicListener = new Response.Listener<String>()
//    {
//        @Override
//        public void onResponse(String s)
//        {
//            Log.d(TAG, "update success");
//            progressDialog.dismiss();
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setClassName(CrowdsourcingActivity.this, FinishActivity.class.getName());
//
//            CrowdsourcingActivity.this.startActivity(intent);
////            this.finishActivity();
//        }
//    };
//
//    private Response.ErrorListener updatePicErrorListener = new Response.ErrorListener()
//    {
//        @Override
//        public void onErrorResponse(VolleyError volleyError)
//        {
//            Log.d(TAG, "update Error: " + volleyError.toString() + " message: " + volleyError.getMessage());
//            progressDialog.dismiss();
//        }
//    };

    private class PhotoResponseHandler implements ResponseHandler<Object>
    {
        @Override
        public Object handleResponse(HttpResponse httpResponse) throws IOException
        {
            HttpEntity resEntity = httpResponse.getEntity();
            String response = EntityUtils.toString(resEntity);
            progressDialog.dismiss();

            if (response.contains("Success"))
            {
                startFinishActivity(response);
            }
            else
            {
                Log.d(TAG, "Error");
                startFinishActivity(response);
            }
            Log.d(TAG, response);
            return null;
        }
    }

    private Handler handler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case 0:
                    ivShoot1.setImageBitmap(resizeBitmapToSquare(BitmapFactory.decodeByteArray(out1.toByteArray(), 0, out1.size(), getBitmapOptions(2)), -90));
                    ivShoot1.setVisibility(View.VISIBLE);
                    progressBarBitmap1.setVisibility(View.GONE);
                    compressNum++;
                    break;
                case 1:
                    ivShoot2.setImageBitmap(resizeBitmapToSquare(BitmapFactory.decodeByteArray(out2.toByteArray(), 0, out2.size(), getBitmapOptions(2)), -90));
                    ivShoot2.setVisibility(View.VISIBLE);
                    progressBarBitmap2.setVisibility(View.GONE);
                    compressNum++;
                    break;
                case 2:
                    ivShoot3.setImageBitmap(resizeBitmapToSquare(BitmapFactory.decodeByteArray(out3.toByteArray(), 0, out3.size(), getBitmapOptions(2)), -90));
                    ivShoot3.setVisibility(View.VISIBLE);
                    progressBarBitmap3.setVisibility(View.GONE);
                    compressNum++;
                    break;
                default:

                    break;
            }

        }
    };

    private PictureCallback jpeg = new PictureCallback()
    {

        @Override
        public void onPictureTaken(byte[] data, Camera camera)
        {
//			Bitmap bmp1Raw = BitmapFactory.decodeByteArray(data,0, data.length);
            InputStream inputStream = new ByteArrayInputStream(data);
            //Use BitmapFactory options to avoid the Out of Memory issue
            bmpRaw = BitmapFactory.decodeStream(inputStream, null, getBitmapOptions(2));
//            Bitmap bmp = resizeBitmapToSquare(bmpRaw);

            FileOutputStream fop;

//            try
//            {
//                fop = new FileOutputStream("/sdcard/d" + photoNum + ".jpg");
//                bmp.compress(Bitmap.CompressFormat.JPEG, 30, fop);
//            }
//            catch (FileNotFoundException e)
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }

            switch (photoNum)
            {
                case 0:
                    ivShoot1.setVisibility(View.INVISIBLE);
                    progressBarBitmap1.setVisibility(View.VISIBLE);
                    progressBarBitmap1.setProgress(0);
//                    bmp.compress(Bitmap.CompressFormat.JPEG, 30, out1);
//                    ivShoot1.setImageBitmap(bmp);
                    ivTitle.setBackgroundColor(Color.parseColor("#FF3090C7"));
                    ivSamplePhoto.setBackgroundResource(R.drawable.photo_sample2);
                    ivTitle.setImageResource(R.drawable.title_step2);
                    photoNum++;
                    break;
                case 1:
                    ivShoot2.setVisibility(View.INVISIBLE);
                    progressBarBitmap2.setVisibility(View.VISIBLE);
                    progressBarBitmap2.setProgress(0);
//                    bmp.compress(Bitmap.CompressFormat.JPEG, 30, out2);
//                    ivShoot2.setImageBitmap(bmp);
                    ivTitle.setBackgroundColor(Color.parseColor("#FF2B60DE"));
                    ivSamplePhoto.setBackgroundResource(R.drawable.photo_sample3);
                    ivTitle.setImageResource(R.drawable.title_step3);
                    photoNum++;
                    break;
                case 2:
                    ivShoot3.setVisibility(View.INVISIBLE);
                    progressBarBitmap3.setVisibility(View.VISIBLE);
                    progressBarBitmap3.setProgress(0);
//                    bmp.compress(Bitmap.CompressFormat.JPEG, 30, out3);
//                    ivShoot3.setImageBitmap(bmp);
                    ivTitle.setBackgroundColor(Color.parseColor("#FF2DFF49"));
                    ivSamplePhoto.setBackgroundColor(Color.parseColor("#FF2DFF49"));
                    ivTitle.setImageResource(R.drawable.title_finish);
                    photoNum++;
                    btnShoot.setBackgroundResource(android.R.drawable.btn_default);
                    break;
                default:
                    break;
            }

            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    Message msg = new Message();
                    bmpRaw = resizeBitmapToSquare(bmpRaw, 90);
                    switch (compressNum)
                    {
                        case 0:
                            out1.reset();
                            bmpRaw.compress(Bitmap.CompressFormat.JPEG, 30, out1);
                            msg.what = 0;
                            break;
                        case 1:
                            out2.reset();
                            bmpRaw.compress(Bitmap.CompressFormat.JPEG, 30, out2);
                            msg.what = 1;
                            break;
                        case 2:
                            out3.reset();
                            bmpRaw.compress(Bitmap.CompressFormat.JPEG, 30, out3);
                            msg.what = 2;
                            break;
                        default:
                            break;
                    }
                    bmpRaw.recycle();
                    handler.sendMessage(msg);
                }
            }).start();
//			bmp.recycle();
//			System.gc();

            //need start preview, otherwise surfaceView will have a screen lock
            camera.stopPreview();
            camera.startPreview();
        }

    };

    private Bitmap resizeBitmapToSquare(Bitmap bitmap, float degree)
    {
        int startHeight, startWidth;
        int edgeLength;

        if (bitmap.getWidth() > bitmap.getHeight())
        {
            edgeLength = bitmap.getHeight();
            startHeight = 0;
            startWidth = (bitmap.getWidth() - bitmap.getHeight()) / 2;
        }
        else
        {
            edgeLength = bitmap.getWidth();
            startWidth = 0;
            startHeight = (bitmap.getHeight() - bitmap.getWidth()) / 2;
        }
        Matrix matrix = new Matrix();
        matrix.setRotate(degree);
        return Bitmap.createBitmap(bitmap, startWidth, startHeight, edgeLength, edgeLength, matrix, true);
    }

    private BitmapFactory.Options getBitmapOptions(int scale)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inSampleSize = scale;
        try
        {
            BitmapFactory.Options.class.getField("inNativeAlloc").setBoolean(options, true);
        }
        catch (Exception e)
        {
            Log.d(TAG, e.toString());
        }

        return options;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        mCamera = Camera.open();
        try
        {
            Camera.Parameters parameters = mCamera.getParameters();
            //in order to get square preview size
            List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
            Camera.Size previewSize = previewSizes.get(0);
            for (int i = 0; i < previewSizes.size(); i++)
            {
                Log.d(TAGG, previewSizes.get(i).width + ", " + previewSizes.get(i).height);

                previewSize = previewSizes.get(i);
                int largeEdge = (previewSize.width > previewSize.height) ? previewSize.width : previewSize.height;
                Log.d(TAGG, "largeEdge = " + largeEdge);
                if (largeEdge <= screenHeight)
                    break;
            }
            Log.d(TAGG, previewSize.width + ", " + previewSize.height);
            android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                    screenHeight * previewSize.width / previewSize.height, screenHeight, Gravity.CENTER);
            svCameraPreview.setLayoutParams(params);

            parameters.setPreviewSize(previewSize.width, previewSize.height);
            mCamera.setParameters(parameters);
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0)
    {
        System.out.println("surfaceDestroyed");
        mCamera.stopPreview();
        mCamera.release();
    }

    private AutoFocusCallback afcb = new AutoFocusCallback()
    {
        public void onAutoFocus(boolean success, Camera camera)
        {
            if (success)
            {
                camera.takePicture(null, null, jpeg);
            }
        }
    };

    private void updatingPics()
    {
        // 測試上傳 強制在主執行緒中執行網路
//        if (android.os.Build.VERSION.SDK_INT > 9) {
//            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//            StrictMode.setThreadPolicy(policy);
//        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Updating");
        progressDialog.setMessage("Updating the pictures");
        progressDialog.show();

        //設置執行緒  執行照片上傳
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Log.d(TAG, "Start updating");
                HttpClient client = new DefaultHttpClient();
                HttpPost updatePhoto = new HttpPost("http://openeatscs.yuchuan1.cloudbees.net/api/1.0/upload");
//                HttpPost updatePhoto = new HttpPost("http://192.168.155.239/api/1.0/upload");
                MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                try
                {
                    entity.addPart("app_user_id", new StringBody(contentUUID));
                    entity.addPart("barcode", new StringBody(productID));
                    entity.addPart("pic0", new ByteArrayBody(out1.toByteArray(), "file", "pic0.jpg"));
                    entity.addPart("pic1", new ByteArrayBody(out2.toByteArray(), "file", "pic1.jpg"));
                    entity.addPart("pic2", new ByteArrayBody(out3.toByteArray(), "file", "pic2.jpg"));
                    updatePhoto.setEntity(entity);
                    client.execute(updatePhoto, new PhotoResponseHandler());

                }
                catch (Exception e)
                {
                    Log.d(TAG, e.toString());
                }
            }
        }).start();


//        StringRequest picReq = new StringRequest(Request.Method.POST, "http://openeatscs.yuchuan1.cloudbees.net/api/1.0/upload", updatePicListener, updatePicErrorListener)
//        {
//            MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
//
//            @Override
//            public byte[] getBody() throws AuthFailureError
//            {
//                ByteArrayOutputStream transit = new ByteArrayOutputStream();
//                entity.addPart("pic0", new ByteArrayBody(out1.toByteArray(), "file", "pic0.jpg"));
//                entity.addPart("pic1", new ByteArrayBody(out2.toByteArray(), "file", "pic1.jpg"));
//                entity.addPart("pic2", new ByteArrayBody(out3.toByteArray(), "file", "pic2.jpg"));
//
//                try
//                {
//                    entity.addPart("app_user_id", new StringBody("1111"));
//                    entity.addPart("barcode", new StringBody("12321"));
//
//                    entity.writeTo(transit);
//                    Log.d(TAG, String.valueOf(transit.size()));
//                }
//                catch (Exception e)
//                {
//                    Log.d(TAG, e.toString());
//                }
//                return transit.toByteArray();
////                return super.getBody();
//            }
//
//            @Override
//            public String getBodyContentType()
//            {
//                Log.d(TAG, entity.getContentType().getValue());
//                return entity.getContentType().getValue();
////                return super.getBodyContentType();
//            }
//        };
//
//        RequestQueue queue = Volley.newRequestQueue(this);
//        try
//        {
//            System.out.print(picReq.getBody().toString());
//        }
//        catch (Exception e)
//        {
//            Log.d(TAG, e.toString());
//        }
//        queue.add(picReq);
    }

    private void startFinishActivity(String msg)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(this, FinishActivity.class.getName());
        intent.putExtra("updateMsg", msg);
        this.startActivity(intent);
        this.finish();
    }


}
