package com.telegram.telegram;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.util.Log;
import android.widget.ImageButton;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.content.pm.PackageManager;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CreateTelegram extends Activity {
    private static final int SELECT_FILE = 1;
    private static final int REQUEST_CAMERA = 2;

    private EditText editText;
    private Button cancelButton;
    private Button postButton;
    private ImageButton uploadImageButton;
    private ImageView imagePreview;
    private String userChosenTask;

    // Had to default this string to "" so it would break as being `null`
    private String uploadedImage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        Intent intent = getIntent();

        final Double lat = intent.getDoubleExtra("lat", 0);
        final Double lng = intent.getDoubleExtra("lng", 0);
        final String uid = intent.getStringExtra("uid");

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;

        // Arbitrarily set the popup window height and width
        getWindow().setLayout((int)(width*0.8), WindowManager.LayoutParams.WRAP_CONTENT);

        // Get the buttons and listen for them to be clicked
        cancelButton = (Button) findViewById(R.id.CancelButton);
        postButton = (Button) findViewById(R.id.PostButton);
        uploadImageButton = (ImageButton) findViewById(R.id.UploadImageButton);

        // Image preview
        imagePreview = (ImageView) findViewById(R.id.ImagePreview);

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

                // Get the editText view so we can get the data thats typed in
                editText = (EditText) findViewById(R.id.TelegramMessage);
                String message = editText.getText().toString();

                Log.d("t", "clicked post");
                final CharSequence[] items = { "Go back" };

                if (message.isEmpty() && uploadedImage.isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(CreateTelegram.this);
                    builder.setTitle("You need to add a message or image to your Telegram!");
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {

                            if (items[item].equals("Go back")) {
                                dialog.dismiss();
                            }
                        }
                    });

                    builder.show();

                }
                else {
                    Telegram telegram = new Telegram(uid, message, uploadedImage, lat, lng, false);

                    // Send data to the maps activity
                    Intent i  = new Intent();

                    // Adds the "message" property to the intent so the main activity can access it
                    i.putExtra("telegram", telegram);

                    // Sends it back to the MapsActivity.onActivityResult()
                    setResult(RESULT_OK, i);

                    finish();
                }
            }

        });


        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("t", "clicked upload image");

                final CharSequence[] items = { "Take Photo", "Choose from Library",
                        "Cancel" };

                AlertDialog.Builder builder = new AlertDialog.Builder(CreateTelegram.this);
                builder.setTitle("Add Photo!");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        boolean result=Utility.checkPermission(CreateTelegram.this);

                        if (items[item].equals("Take Photo")) {
                            userChosenTask ="Take Photo";
                            if(result)
                                cameraIntent();

                        } else if (items[item].equals("Choose from Library")) {
                            userChosenTask ="Choose from Library";
                            if(result)
                                galleryIntent();

                        } else if (items[item].equals("Cancel")) {
                            dialog.dismiss();
                        }
                    }
                });
                builder.show();
            }
        });
    }


    private void cameraIntent()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select File"),SELECT_FILE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(userChosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if(userChosenTask.equals("Choose from Library"))
                        galleryIntent();
                } else {
                    Log.d("t", "Denied permission");
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
        }
    }


    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {

        Bitmap bm=null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        int width=1000;
        int height=1000;

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int newWidth = dm.widthPixels;
        int newHeight= dm.heightPixels;

        // Arbitrarily set the popup window height and width
        getWindow().setLayout((int)(newWidth*0.8), (int)(newHeight*0.5));
        //int inWidth = editText.getWidth();
        //int inHeight = editText.getHeight();

        //Log.d("t", "Width: " + inWidth + " -- height: " + inHeight);

        Bitmap resizedbitmap=Bitmap.createScaledBitmap(bm, width, height, true);
//        imagePreview.setImageBitmap(resizedbitmap);
        imagePreview.setImageBitmap(bm);
        imagePreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imagePreview.setMaxHeight(1000);
        imagePreview.setMaxWidth(1000);

        //imagePreview.setImageBitmap(bm);
        String imageStr = getEncoded64ImageStringFromBitmap(bm);
        uploadedImage = imageStr;
    }

    public String getEncoded64ImageStringFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);

        byte[] byteFormat = stream.toByteArray();
        // get the base 64 string
        String imgString = Base64.encodeToString(byteFormat, Base64.NO_WRAP);
        return imgString;
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

        File destination = new File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis() + ".jpg");

        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        imagePreview.setImageBitmap(thumbnail);

        // Convert the image to Base64
        byte[] byteFormat = bytes.toByteArray();
        // get the base 64 string
        String imgString = Base64.encodeToString(byteFormat, Base64.NO_WRAP);
        uploadedImage = imgString;
    }





}