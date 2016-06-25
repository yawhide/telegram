package com.telegram.telegram;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by shayanmasood on 16-06-24.
 */
public class ViewTelegram extends Activity {

    private TextView msg, uid, tid;
    private Button okayButton;
    private ImageView imagePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.info_window);

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

        // Arbitrarily set the popup window height and width
        getWindow().setLayout((int)(width*0.8), (int)(height*0.3));

        uid.setText(telegram.getUid());
        msg.setText(telegram.getMsg());

        // Only put add the image preview if there is an image string
        if (!telegram.getImg().isEmpty()) {
            imagePreview.setImageBitmap(base64ToBitmap(telegram.getImg()));
        }


        okayButton = (Button) findViewById(R.id.OkayButton);

        okayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("t", "clicked okay");

                Intent i = new Intent();
                i.putExtra("telegram", telegram);
                setResult(124, i);

                finish();
            }
        });

    }

    private Bitmap base64ToBitmap(String b64) {
        byte[] imageAsBytes = Base64.decode(b64.getBytes(), Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
    }
}
