package com.mortensickel.obslogger;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;


/**
 * Created by morten on 3/8/15.
 */

public class itemList extends Activity {
    List<Map<String, String>> planetsList = new ArrayList<Map<String,String>>();
    SimpleAdapter simpleAdpt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.itemlist);
        initList();
        ListView lv = (ListView) findViewById(R.id.listView);
        simpleAdpt = new SimpleAdapter(this, planetsList, android.R.layout.simple_list_item_1, new String[] {"planet"}, new int[] {android.R.id.text1});
        lv.setAdapter(simpleAdpt);
        Button next = (Button) findViewById(R.id.ButtonBack);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), MainActivity.class);
                startActivityForResult(myIntent, 0);
            }

        });
    }

    private HashMap<String, String> createPlanet(String key, String name) {
        HashMap<String, String> planet = new HashMap<String, String>();
        planet.put(key, name);
        return planet;
    }

     private void initList() {
    // We populate the planets

    planetsList.add(createPlanet("planet", "Mercury"));
    planetsList.add(createPlanet("planet", "Venus"));
    planetsList.add(createPlanet("planet", "Mars"));
    planetsList.add(createPlanet("planet", "Jupiter"));
    planetsList.add(createPlanet("planet", "Saturn"));
    planetsList.add(createPlanet("planet", "Uranus"));
    planetsList.add(createPlanet("planet", "Neptune"));


}


}
