package com.telegram.telegram;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class TelegramAdapter extends ArrayAdapter<Telegram> {

    public TelegramAdapter(Context context, ArrayList<Telegram> telegrams) {
        super(context, 0, telegrams);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Telegram telegram = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.telegram_item, parent, false);
        }
        // Lookup view for data population
//        ImageView imagePreview = (ImageView) convertView.findViewById(R.id.ImagePreview);
        TextView message = (TextView) convertView.findViewById(R.id.message);
        TextView user = (TextView) convertView.findViewById(R.id.user);

        // Populate the data into the template view using the data object
        String imgURL = telegram.getImg();

//        new DownloadImageTask(imagePreview).execute(imgURL);
        message.setText(telegram.getMsg());
        user.setText(telegram.getUid());

        // Return the completed view to render on screen
        return convertView;
    }
}
