package com.telegram.telegram;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewTelegram extends Activity {

    private TextView msg, uid, tid;
    private Button okayButton;
    private ImageView imagePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_view);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        Intent intent = getIntent();

        final Telegram telegram = (Telegram) intent.getExtras().get("telegram");

        uid = (TextView) findViewById(R.id.user);
        msg = (TextView) findViewById(R.id.msg);

        // Image view here to get images that are uploaded with a telegram
        imagePreview = (ImageView) findViewById(R.id.ImagePreview);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        uid.setText(telegram.getUid());
        msg.setText(telegram.getMsg());

        // Only put add the image preview if there is an image string
        if (!telegram.getImg().isEmpty()) {
            String imgURL = telegram.getImg();

            new DownloadImageTask(imagePreview).execute(imgURL);
        }

        getWindow().setLayout((int)(width*0.8), WindowManager.LayoutParams.WRAP_CONTENT);

        okayButton = (Button) findViewById(R.id.OkayButton);

        okayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("t", "clicked okay");
                finish();
            }
        });

        Intent i = new Intent();
        i.putExtra("telegram", telegram);
        setResult(124, i);

    }

}
