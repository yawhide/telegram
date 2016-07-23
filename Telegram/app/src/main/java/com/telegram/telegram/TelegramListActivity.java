package com.telegram.telegram;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class TelegramListActivity extends ListActivity {

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        String[] values = new String[] { "Android", "iPhone", "WindowsMobile",
                "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
                "Linux", "OS/2", "Android", "iPhone", "WindowsMobile",
                "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
                "Linux", "OS/2"  };

        ArrayList<Telegram> telegrams = (ArrayList<Telegram>) getIntent().getExtras().get("telegrams");

//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
//                android.R.layout.simple_list_item_1, values);


        TelegramAdapter adapter = new TelegramAdapter(this, telegrams);
        setListAdapter(adapter);
        // Attach the adapter to a ListView
//        ListView listView = (ListView) findViewById(R.layout.telegram_list);
//        listView.setAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // TODO implement some logic
        Telegram telegram = (Telegram) getListAdapter().getItem(position);
        Intent i = new Intent(this, ViewTelegram.class);
        i.putExtra("telegram", telegram);
        startActivity(i);
    }
}
