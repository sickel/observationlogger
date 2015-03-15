package com.mortensickel.obslogger;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.*;

/**
 * Created by morten on 3/8/15.
 */

public class itemList extends Activity {
    List<Map<String, String>> obsList = new ArrayList<Map<String, String>>();
    SimpleAdapter simpleAdpt;
    String lastdrag, lasttimestamp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.itemlist);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getString("lastdrag") != null) lastdrag = extras.getString("lastdrag");
            // Toast.makeText(getApplicationContext(),"|"+lastdrag+"|",Toast.LENGTH_LONG).show();
            if (extras.getString("lasttime") != null) lasttimestamp = extras.getString("lasttime");
            // Toast.makeText(getApplicationContext(),"|"+lasttimestamp+"|",Toast.LENGTH_LONG).show();
        }
        Button next = (Button) findViewById(R.id.ButtonBack);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(view.getContext(), MainActivity.class);
              	EditText et=(EditText)findViewById(R.id.observationText);
				i.putExtra("freetext",et.getText().toString());
				i.putExtra("lastdrag", lastdrag);
                i.putExtra("lasttime", lasttimestamp);
			//	Toast.makeText(getApplicationContext(),et.getText(),Toast.LENGTH_SHORT).show();
				setResult(Activity.RESULT_OK,i);
				finish();
            }

           // TODO: Freetext should be indicated to exist in "Last observation", but not quoted
            // TODO: Maybe first 10-15 letters in Freetext and ...
            // TODO: Remember that data are picked up from last observation - maybe time for a change
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String altlist = sharedPrefs.getString("secValues", getResources().getString(R.string.altnames));
        List<String> observations = Arrays.asList(altlist.split("\\s*,\\s*"));
	    obsList.clear();
        for (String obs : observations) {
		    obsList.add(createObservation("observation", obs));
        }
		altlist = sharedPrefs.getString("dropnames", getResources().getString(R.string.dropnames));
        observations = Arrays.asList(altlist.split("\\s*,\\s*"));
      	for (String obs : observations) {
		    obsList.add(createObservation("observation", obs));
        }
        ListView lv = (ListView) findViewById(R.id.listView);
        simpleAdpt = new SimpleAdapter(this, obsList, android.R.layout.simple_list_item_1, new String[]{"observation"}, new int[]{android.R.id.text1});
        lv.setAdapter(simpleAdpt);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {


            public void onItemClick(AdapterView<?> parentAdapter, View view, int position,long id) {
                TextView clickedView = (TextView) view;
                Intent i = new Intent(view.getContext(), MainActivity.class);
                i.putExtra("lastdrop", clickedView.getText());
                i.putExtra("lastdrag", lastdrag);
                i.putExtra("lasttime", lasttimestamp);
				EditText et=(EditText)findViewById(R.id.observationText);
				i.putExtra("freetext",et.getText().toString());
       		    setResult(Activity.RESULT_OK,i);
				finish();
            }
        });
        // See more at: http://www.survivingwithandroid.com/2012/09/listviewpart-1.html#sthash.vZYbPB7J.dpuf
    }

    private HashMap<String, String> createObservation(String key, String name) {
        HashMap<String, String> observation = new HashMap<String, String>();
        observation.put(key, name);
        return observation;
    }


}
