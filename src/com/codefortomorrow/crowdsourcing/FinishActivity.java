package com.codefortomorrow.crowdsourcing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.google.zxing.client.android.CaptureActivity;


/**
 * Created by Lee on 2013/12/17.
 */
public class FinishActivity extends Activity
{
    private Button finishButton;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_finish);
        findView();
        setListener();

    }

    private void findView()
    {
        finishButton = (Button) findViewById(R.id.button_finish);
    }

    private void setListener()
    {
        finishButton.setOnClickListener(btn_finish_click);
    }

    private void loadData()
    {

    }

    private Button.OnClickListener btn_finish_click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setClassName(FinishActivity.this, CaptureActivity.class.getName());
            FinishActivity.this.startActivity(intent);
            FinishActivity.this.finish();
        }
    };
}