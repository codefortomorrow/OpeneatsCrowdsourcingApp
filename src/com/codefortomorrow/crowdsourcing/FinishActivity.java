package com.codefortomorrow.crowdsourcing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.zxing.client.android.CaptureActivity;


/**
 * Created by Lee on 2013/12/17.
 */
public class FinishActivity extends Activity
{
    private Button finishButton;
    private TextView finishTextView;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_finish);
        findView();
        setListener();
        loadData();

    }

    private void findView()
    {
        finishButton = (Button) findViewById(R.id.button_finish);
        finishTextView = (TextView) findViewById(R.id.textView_finish);
    }

    private void setListener()
    {
        finishButton.setOnClickListener(btn_finish_click);
    }

    private void loadData()
    {
        Intent intent = getIntent();
        String updateMsg = intent.getStringExtra("updateMsg");
        if(!updateMsg.contains("Success"));
        {
            finishTextView.setText(updateMsg);
        }
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