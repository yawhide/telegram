package com.telegram.telegram;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
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

import java.io.InputStream;
import java.net.URL;

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

            new DownLoadImageTask(imagePreview).execute(imgURL);

            getWindow().setLayout((int)(width), (int)(height));
        }
        else {
            getWindow().setLayout((int)(width*0.8), (int)(height*0.3));
        }

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

    private class DownLoadImageTask extends AsyncTask<String,Void,Bitmap> {
        ImageView imageView;

        public DownLoadImageTask(ImageView imageView){
            this.imageView = imageView;
        }

        /*
            doInBackground(Params... params)
                Override this method to perform a computation on a background thread.
         */
        protected Bitmap doInBackground(String...urls){
            String urlOfImage = urls[0];
            Bitmap image = null;
            try{
                InputStream is = new URL(urlOfImage).openStream();
                /*
                    decodeStream(InputStream is)
                        Decode an input stream into a bitmap.
                 */
                image = BitmapFactory.decodeStream(is);
            }catch(Exception e){ // Catch the download exception
                e.printStackTrace();
            }
            return image;
        }

        /*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
        protected void onPostExecute(Bitmap result){
            int nh = (int) ( result.getHeight() * (512.0 / result.getWidth()) );
            Bitmap scaled = Bitmap.createScaledBitmap(result, 512, nh, true);
            imageView.setImageBitmap(scaled);
        }
    }
}
