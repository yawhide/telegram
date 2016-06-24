package com.telegram.telegram;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.util.Log;
import android.widget.ImageButton;

public class CreateTelegram extends Activity {
    private EditText editText;
    private Button cancelButton;
    private Button postButton;
    private ImageButton uploadImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create);

        Intent intent = getIntent();

        final Double lat = intent.getDoubleExtra("lat", 0);
        final Double lng = intent.getDoubleExtra("lng", 0);
        final String uid = intent.getStringExtra("uid");

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        // Arbitrarily set the popup window height and width
        getWindow().setLayout((int)(width*0.8), (int)(height*0.5));

        // Get the editText view so we can get the data thats typed in
        editText = (EditText) findViewById(R.id.TelegramMessage);
        final String message = editText.getText().toString();

        // Get the buttons and listen for them to be clicked
        cancelButton = (Button) findViewById(R.id.CancelButton);
        postButton = (Button) findViewById(R.id.PostButton);
        uploadImageButton = (ImageButton) findViewById(R.id.UploadImageButton);

        // TODO: 6/23/2016 have some error checking avie!!!!!!!!!!!!!!!!!!! 

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("t", "clicked cancel");

                // finish activity and close the bitch
                finish();
            }

        });

        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("t", "clicked post");

                Telegram telegram = new Telegram(uid, message, "TODO FOR AVIE", lat, lng, false);

                // Send data to the maps activity
                Intent i  = new Intent();

                // Adds the "message" property to the intent so the main activity can access it
                i.putExtra("telegram", telegram);

                // Sends it back to the MapsActivity.onActivityResult()
                setResult(RESULT_OK, i);

                finish();
            }

        });


        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("t", "clicked upload image");

                // Send data to the maps activity
                Intent i = new Intent();
            }
        });



    }
}