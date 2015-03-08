package com.mortensickel.obslogger;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by morten on 3/8/15.
 */

public class itemList extends Activity {
    List<Map<String, String>> obsList = new ArrayList<Map<String, String>>();
    SimpleAdapter simpleAdpt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.itemlist);
        initList();
        ListView lv = (ListView) findViewById(R.id.listView);
        simpleAdpt = new SimpleAdapter(this, obsList, android.R.layout.simple_list_item_1, new String[]{"observation"}, new int[]{android.R.id.text1});
        lv.setAdapter(simpleAdpt);
        Button next = (Button) findViewById(R.id.ButtonBack);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), MainActivity.class);
                startActivityForResult(myIntent, 0);
            }

        });
    }

    private HashMap<String, String> createObservation(String key, String name) {
        HashMap<String, String> observation = new HashMap<String, String>();
        observation.put(key, name);
        return observation;
    }

    private void initList() {
        // TODO: Possible to use a text input as one of the items?
    // We populate the planets

        obsList.add(createObservation("observation", "Drinking"));
        obsList.add(createObservation("observation", "Fighting"));
        obsList.add(createObservation("observation", "Dancing"));
        obsList.add(createObservation("observation", "Singing"));
        obsList.add(createObservation("observation", "Climbing"));
        obsList.add(createObservation("observation", "?????"));

    }


}
