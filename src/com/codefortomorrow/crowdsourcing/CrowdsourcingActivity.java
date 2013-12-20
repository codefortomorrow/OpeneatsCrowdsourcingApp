package com.codefortomorrow.crowdsourcing;

import java.io.*;
import java.util.List;
import java.util.logging.LogRecord;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;

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


public class CrowdsourcingActivity extends Activity implements SurfaceHolder.Callback {
	
	private int photoNum = 0;
    private int compressNum = 0;
    private String productID;

	private SurfaceHolder surfaceHolder;
    private SurfaceView svCameraPreview;
    private Button btnShoot, btnBack;
    private ImageView ivShoot1, ivShoot2, ivShoot3;
    private ImageView      ivTitle;
    private LinearLayout   mLinearLayout;
    private Camera         mCamera;
    private ProgressDialog progressDialog;
    private ProgressBar progressBarBitmap1;
    private ProgressBar progressBarBitmap2;
    private ProgressBar progressBarBitmap3;
    private Bitmap bmpRaw;

    //ByteArrayOutputStreams for uploading to server
    private ByteArrayOutputStream out1 = new ByteArrayOutputStream();
    private ByteArrayOutputStream out2 = new ByteArrayOutputStream();
    private ByteArrayOutputStream out3 = new ByteArrayOutputStream();

    private String TAG = "Lee";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        //use fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera_test);
        //set screen orientation
        setRequestedOrientation(0);
        //set up buttons
        btnShoot = (Button) findViewById(R.id.btn_shoot);
        btnBack = (Button) findViewById(R.id.btn_back);
        btnShoot.setOnClickListener(myOnClickListener);
        btnBack.setOnClickListener(myOnClickListener);

        ivShoot1 = (ImageView) findViewById(R.id.iv_shoot1);
        ivShoot2 = (ImageView) findViewById(R.id.iv_shoot2);
        ivShoot3 = (ImageView) findViewById(R.id.iv_shoot3);
        ivTitle = (ImageView) findViewById(R.id.iv_title);
        progressBarBitmap1 = (ProgressBar) findViewById(R.id.progressBar_bitmap1);
        progressBarBitmap2 = (ProgressBar) findViewById(R.id.progressBar_bitmap2);
        progressBarBitmap3 = (ProgressBar) findViewById(R.id.progressBar_bitmap3);

        mLinearLayout = (LinearLayout) findViewById(R.id.LinearLayout1);

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
                        mLinearLayout.setBackgroundColor(Color.parseColor("#FF95B9C7"));
                        ivTitle.setImageResource(R.drawable.title_step1);
                        compressNum--;
                        photoNum--;
                        break;
                    case 2:
                        ivShoot2.setImageResource(R.drawable.shoot2);
                        ivTitle.setBackgroundColor(Color.parseColor("#FF3090C7"));
                        mLinearLayout.setBackgroundColor(Color.parseColor("#FF3090C7"));
                        ivTitle.setImageResource(R.drawable.title_step2);
                        btnShoot.setText("");
                        compressNum--;
                        photoNum--;
                        break;
                    case 3:
                        ivShoot3.setImageResource(R.drawable.shoot3);
                        ivTitle.setBackgroundColor(Color.parseColor("#FF2B60DE"));
                        mLinearLayout.setBackgroundColor(Color.parseColor("#FF2B60DE"));
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
                startFinishActivity();
            }
            else
            {
                Log.d(TAG, "Error");
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
                    ivShoot1.setImageBitmap(BitmapFactory.decodeByteArray(out1.toByteArray(), 0, out1.size(), getBitmapOptions(2)));
                    ivShoot1.setVisibility(View.VISIBLE);
                    progressBarBitmap1.setVisibility(View.GONE);
                    compressNum++;
                    break;
                case 1:
                    ivShoot2.setImageBitmap(BitmapFactory.decodeByteArray(out2.toByteArray(), 0, out2.size(), getBitmapOptions(2)));
                    ivShoot2.setVisibility(View.VISIBLE);
                    progressBarBitmap2.setVisibility(View.GONE);
                    compressNum++;
                    break;
                case 2:
                    ivShoot3.setImageBitmap(BitmapFactory.decodeByteArray(out3.toByteArray(), 0, out3.size(), getBitmapOptions(2)));
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
                    mLinearLayout.setBackgroundColor(Color.parseColor("#FF3090C7"));
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
                    mLinearLayout.setBackgroundColor(Color.parseColor("#FF2B60DE"));
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
                    mLinearLayout.setBackgroundColor(Color.parseColor("#FF2DFF49"));
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
                    bmpRaw = resizeBitmapToSquare(bmpRaw);
                    switch (compressNum)
                    {
                        case 0:
                            bmpRaw.compress(Bitmap.CompressFormat.JPEG, 30, out1);
                            msg.what = 0;
                            break;
                        case 1:
                            bmpRaw.compress(Bitmap.CompressFormat.JPEG, 30, out2);
                            msg.what = 1;
                            break;
                        case 2:
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

    private Bitmap resizeBitmapToSquare(Bitmap bitmap)
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
        return Bitmap.createBitmap(bitmap, startWidth, startHeight, edgeLength, edgeLength);
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
            Log.d("TAG", e.toString());
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
            int minArea = previewSize.width * previewSize.height;
            for (int i = 0; i < previewSizes.size(); i++)
            {
                Log.d("mmpud", previewSizes.get(i).width + ", " + previewSizes.get(i).height);
                if (previewSizes.get(i).width == previewSizes.get(i).height)
                {
                    previewSize = previewSizes.get(i);
                }
            }
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
                MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                try
                {
                    entity.addPart("app_user_id", new StringBody("1111"));
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

    private void startFinishActivity()
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(this, FinishActivity.class.getName());
        this.startActivity(intent);
        this.finish();
    }


}
