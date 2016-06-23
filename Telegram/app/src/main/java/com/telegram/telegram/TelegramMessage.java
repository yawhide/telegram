package com.telegram.telegram;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.util.Log;

public class TelegramMessage extends Activity {
    private EditText editText;
    private Button cancelButton;
    private Button postButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_message);

        Intent intent = getIntent();

        Double lat = intent.getDoubleExtra("lat", 0);
        Double lng = intent.getDoubleExtra("lng", 0);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        // Arbitrarily set the popup window height and width
        getWindow().setLayout((int)(width*0.8), (int)(height*0.5));

        // Get the editText view so we can get the data thats typed in
        editText = (EditText) findViewById(R.id.TelegramMessage);

        // Get the buttons and listen for them to be clicked
        cancelButton = (Button) findViewById(R.id.CancelButton);
        postButton = (Button) findViewById(R.id.PostButton);

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

                // Send data to the maps activity
                Intent i  = new Intent();

                // Adds the "message" property to the intent so the main activity can access it
                i.putExtra("message", editText.getText().toString());

                // Sends it back to the MapsActivity.onActivityResult()
                setResult(RESULT_OK, i);

                finish();
            }

        });

    }
}