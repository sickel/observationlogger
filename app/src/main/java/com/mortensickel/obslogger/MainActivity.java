package com.mortensickel.obslogger;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.mortensickel.obslogger.LocationService.LocalBinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// DONE 1.6: unlock by menu to confirm upload
// TODO: Set time when dragging, not when confirming
// TODO: Make use of confirm user select able
// TODO: Demand project and user name before uploading
// DONE 1.6: reminds of project and user name
// TODO: Fetch settings data from server
// TODO: Photo - take photo using normal app. Select and upload in app.
// DONE 1.6: track down error on first registration
// DONE 1.6: store and restore state if app is killed
// DONE 1.6: reset last saved 
// DONE 1.6: bigger countdown timer
// DONE 1.6: Use vibration when time for new observation - check on phone
// DONE 1.6: silent mode
// DONE 1.6: Hide countdown timer if period=0
// TODO: csv textlog of stored data
// TODO: View log of stored data, export them
// DONE 1.8: Copy log to clipboard
// TODO: Send log by mail
// DONE 1.8: Sending in body
// TODO: Send log as attachment
// TODO: create kml of observations
// TODO: logfile pr project.
// DONE: 1.6 <project>.csv
// TODO: Overview of logfiles. possibility to erase or export.
// TODO: reset logfile from menu
// TODO: ad hoc behaviour to be stored as new extra -
// DONE 1.6: ad hoc behaviour in addition to freetext
// TODO: new ad hoc pushed to other devices in same project
// TODO: set comments in settings to show actual values
// BUG: Cleardisplay does not work.
// DONE: 1.7 Asks correctly for.permissions in android 6
// DONE: 1.7 Check if this causes problems in android 5
// DONE: 1.8 added functionallity to not use secondary values


public class MainActivity extends Activity {
	LocationService lService;
    private static String LOGTAG="Obslogger";
    private String lastdrag = "";
    private String lastdrop = "";
    private String dragged = "";
    private String dropped = "";
    private String lasttimestamp = "";
    boolean lServiceBound=false;
	private String urlString="http://sickel.net/obslog/store.php";
	// view data at http://sickel.net/obslog/
    private boolean doUpload=true;
    private String savefile="observations.dat";
	private String errorfile="errors.dat";
	private String project="";
    private final ShowTimeRunner myTimerThread = new ShowTimeRunner();
    private static final int RESULT_SETTINGS = 1;
	private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION =1;
    private static final int APILEVEL= Build.VERSION.SDK_INT;
    private String uuid="";
    private String username="";
	private String freetext="";
    private int timeout=10;
	private static final int ACTIVITY_ITEMLIST=0;
	private SimpleDateFormat isoDateFormat=new SimpleDateFormat("yyyy-MM-dd+HH.mm.ss+z");
	private SimpleDateFormat timeDateFormat=new SimpleDateFormat("HH.mm.ss");
	private long waitmins;
	private long cleardisplay=24;
	private boolean quietMode = false;
	private boolean useGPS = false;
	private boolean keepUnlocked = false;
    private boolean usesec = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if( savedInstanceState != null ) {
			lastdrag=savedInstanceState.getString("lastdrag");
			lastdrop=savedInstanceState.getString("lastdrop");
			lasttimestamp=savedInstanceState.getString("lasttimestamp");
			try{
				myTimerThread.setTime(lasttimestamp);
				if(myTimerThread.getTime()>cleardisplay*3600){
                    // dont mind to display obs after cleardisplay hours
                    lastdrag="";
					lastdrop="";
				}
			}catch(java.text.ParseException e){
				debug("time format error 137");
			}
		}
		uuid=Installation.id(getApplicationContext());
	    setContentView(R.layout.main);
		ActionBar actionBar = getActionBar();
        assert actionBar != null;
        actionBar.setCustomView(R.layout.actionbar);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
		Thread showtimeThread;
		showtimeThread = new Thread(myTimerThread);
		showtimeThread.start();
	 	Button bt=(Button)findViewById(R.id.btnConfirm);
		bt.setEnabled(false);
		bt=(Button)findViewById(R.id.btnUndo);
		bt.setEnabled(false);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
					// If request is cancelled, the result arrays are empty.
					if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
							startGPS();
							useGPS=true;
					} else {
                        useGPS = false;
                    }
				}

				// other 'case' lines to check for other
				// permissions this app might request
		}
	}
	
	
	
    private ServiceConnection lServiceConnection;
    // www.trution.com/2014/11/bound-service-example-android/
	@Override
	protected void onStart(){
		super.onStart();
		int permissionCheck = ContextCompat.checkSelfPermission(this,
									Manifest.permission.ACCESS_FINE_LOCATION);
		if (permissionCheck != PackageManager.PERMISSION_GRANTED){
			ActivityCompat.requestPermissions(this,
											  new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
											  MY_PERMISSIONS_REQUEST_FINE_LOCATION);
		}else{
			startGPS();
			useGPS=true;
		}
	}
	
	protected void startGPS(){
		Intent intent=new Intent(this, LocationService.class);
		intent.setAction("startListening");
		startService(intent);
	   	lServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocalBinder binder = (LocalBinder) service;
                lService = binder.getService();
                lServiceBound=true;
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                lServiceBound = false;
            }
        };
        bindService(intent,lServiceConnection,Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("lastdrag", lastdrag);
		outState.putString("lastdrop", lastdrop);
		outState.putString("lasttimestamp", lasttimestamp);
	}
	
	@Override
	protected void onStop(){
		super.onStop();
		if(lServiceBound){		
			stopGPS();
		}
	}
	
	protected void  stopGPS(){
        // TODO: see if it is possible to turn of GPS immediately
        unbindService(lServiceConnection);
		lServiceBound=false;
	}
	
    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        urlString=sharedPrefs.getString("uploadURL", "");
        username=sharedPrefs.getString("userName","");
        project=sharedPrefs.getString("projectName","");
        usesec = sharedPrefs.getBoolean("useSecValues", true);
        // TODO: Check if <project>.csv exists if not - create it with a header line
		quietMode=sharedPrefs.getBoolean("prefQuietMode",false);
		if(username.equals("")||project.equals("")){
			debug(getResources().getString(R.string.errUsernameProject).toString());
		}
		cleardisplay=Integer.parseInt(sharedPrefs.getString("pref_cleardisplay","24"));
		waitmins=Integer.parseInt(sharedPrefs.getString("pref_logperiod","10"));
		View countdown=findViewById(R.id.llCountDown);
		if(waitmins==0){
			countdown.setVisibility(View.GONE);
		}else{
			countdown.setVisibility(View.VISIBLE);
		}
        timeout=Integer.parseInt(sharedPrefs.getString("pref_timeout","20"));
        String dragnames=sharedPrefs.getString("dragNames", getResources().getString(R.string.dragnames));
        ViewGroup ll =(ViewGroup)findViewById(R.id.dragzones);
        setZones(ll,dragnames);
        String dropnames=sharedPrefs.getString("dropNames", getResources().getString(R.string.dropnames));
        ll =(ViewGroup)findViewById(R.id.dropzones);
        setZones(ll,dropnames);
        Integer lnum = 0;
        try {
            lnum=linenumbers(new File(getFilesDir(), errorfile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        showStatus(lnum.toString());
        TextView txtLast = (TextView) findViewById(R.id.tvLastObsType);
		String otime="";
		if(!(lasttimestamp.equals(""))){
			try{
        	   otime=timeDateFormat.format(isoDateFormat.parse(lasttimestamp));  
			}
			catch(java.text.ParseException e){
				if(!lasttimestamp.equals("")){
					// will expect.empty timestamp before first registration
					debug(getResources().getString(R.string.errTimeFormat)+" 236");}
				otime=lasttimestamp;
			}
		}
		//txtLast.setText(otime + ": " + lastdrag + " " + lastdrop);
        txtLast.setText(getString(R.string.txtLast,otime,lastdrag,lastdrop));

    }

    public void debug(String t){
	    Toast.makeText(getApplicationContext(),t,Toast.LENGTH_SHORT).show();
    }

	public void debug(Integer i){
		Toast.makeText(getApplicationContext(),i.toString(),Toast.LENGTH_SHORT).show();
	}

	@Override 
	public void onActivityResult(int requestCode, int resultCode, Intent i) {     
		super.onActivityResult(requestCode, resultCode, i); 
	//	debug(requestCode);
	// DONE indicate existing freetext
		switch(requestCode) { 
			case (ACTIVITY_ITEMLIST) : { 
				if (resultCode == Activity.RESULT_OK) { 
					Bundle extras = i.getExtras();
					if (extras.getString("lastdrop") != null) {
					    lastdrop = extras.getString("lastdrop");
					    dropped = lastdrop;
                    }
					if (extras.getString("lastdrag") != null){
					    lastdrag = extras.getString("lastdrag");
					    dragged = lastdrag;
                    }
					if (extras.getString("lasttime") != null) lasttimestamp = extras.getString("lasttime");
					if (extras.getString("freetext") != null) freetext = extras.getString("freetext");	
					Button bt = (Button) findViewById(R.id.btnConfirm);
					bt.setEnabled(true);
					myTimerThread.resetTime();
				} 
				break; 
			} 
		} 
	}
	
    public void setZones(ViewGroup ll, String names){
       // rewrite to objects to avoid the if - or maybe not? Is this cleaner although less javaesqe?
        List<String> zoneNames= Arrays.asList(names.split("\\s*,\\s*"));
        ll.removeAllViews();
        for (String zone : zoneNames) {
            LinearLayout b = new LinearLayout(this);
            b.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
            TextView tv = new TextView(this);
            tv.setText(zone);
            tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            tv.setGravity(Gravity.CENTER);
            b.addView(tv);
            b.setBackgroundResource(R.drawable.shape);
            if(ll.getId()==R.id.dropzones) {
                b.setOnDragListener(new MyDropListener());
            }else{
                b.setOnDragListener(new MyDragListener());
                tv.setOnTouchListener(new MyTouchListener());
            }
            ll.addView(b);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO: Rewrite to use fragments
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_settings:
                Intent i = new Intent(this, UserSettingsActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                break;
			case R.id.menu_upload:
				saveObs();	
				break;
			case R.id.menu_togglegps:
			    toggleGPS();
				break;
			case R.id.menu_unlock:
				Button btn=(Button)findViewById(R.id.btnConfirm);
				btn.setEnabled(true);
				keepUnlocked=true;
                break;
            case R.id.copycsv:
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                String copytext = csvtostring();
                android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", copytext);
                clipboard.setPrimaryClip(clip);
                debug(getResources().getString(R.string.DataOnClipboard));
                break;
            case R.id.menu_attachdata:
                //      String filename=project+".csv";
                //      File src= new File (getFilesDir(),filename);
                //      File dst= new File (Environment.getExternalStorage().getAbsolutePath())
                Intent in = new Intent(Intent.ACTION_SEND);
                in.setType("text/plain");
                in.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
                in.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.emailsubject) + " " + project);
                in.putExtra(Intent.EXTRA_TEXT, getString(R.string.emailbody) + "\n\n" + csvtostring());
                try {
                    startActivity(Intent.createChooser(in, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return true;
    }

    public String csvtostring() {
        String result;
        String filename = project + ".csv";
        try {
            File file = new File(getFilesDir(), filename);
            long length = file.length();
            if (length < 1 || length > Integer.MAX_VALUE) {
                result = "";
                debug("File is empty or huge: " + filename);
            } else try {
                FileReader in = new FileReader(file);
                char[] content = new char[(int) length];
                int numRead = in.read(content);
                if (numRead != length) {
                    debug("Incomplete read of " + file + ". Read chars " + numRead + " of " + length);
                }
                result = new String(content, 0, numRead);

            } catch (Exception ex) {
                debug("Failure reading " + filename);
                result = "";
            }
        } catch (Exception ex) {
            debug("File not found");
            result = "";
        }
        return result;
    }

	public void toggleGPS(){
		if(lServiceBound){
			Toast.makeText(getApplicationContext(),getResources().getString( R.string.stopping_gps),Toast.LENGTH_SHORT).show();
			stopGPS();
		}else{
			if(useGPS){
			Toast.makeText(getApplicationContext(),getResources().getString( R.string.starting_gps),Toast.LENGTH_SHORT).show();
			startGPS();
			}else{
				Toast.makeText(getApplicationContext(),getResources().getString(R.string.gps_disabled),Toast.LENGTH_LONG).show();
			}
		}
	}
	
	
	public void saveObs(){
		/* Reading log of observations and reupload those that are not uploaded
		*/
			ArrayList<String> linelist = new ArrayList<>();
		try{
			InputStream is=openFileInput(errorfile);
			BufferedReader rdr =new BufferedReader(new InputStreamReader(is));
			String myLine;
			while ((myLine=rdr.readLine())!=null) 
				if (!(myLine.substring(0,5).equals("Error")))
					linelist.add(myLine);	
			File dir=getFilesDir();
			File from = new File(dir,errorfile);
			File to = new File(dir,errorfile+".bak");
			if(from.exists()) if(!(from.renameTo(to))) debug("Could not rename");
		}catch(Exception e){
			Toast.makeText(getApplicationContext(),getResources().getString(R.string.noDataFound),Toast.LENGTH_SHORT).show();
		}
		Toast.makeText(getApplicationContext(),linelist.size()+" lines read - uploading",Toast.LENGTH_SHORT).show();
		for(String line :linelist){
			try{
				if (!(line.substring(0,5).equals("Error"))){
					// URL url = new URL(line);
                    HashMap<String, String> paramset = new HashMap<String, String>();
                    paramset.put("parameters",line);
                    new PostObservation().execute(paramset);
				}
			} catch (Exception e) {
				Toast.makeText(getApplicationContext(),"error 359 "+e,Toast.LENGTH_LONG).show();
			}
		}
        Integer lnum=0;
        try {
            lnum=linenumbers(new File(getFilesDir(), errorfile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        showStatus(lnum.toString());
	}


    public void undoAct(View v){
		Button btn=(Button)findViewById(R.id.btnUndo);
		btn.setEnabled(false);
        HashMap<String, String> paramset = new HashMap<String, String>();
        paramset.put("undo","undo");
        paramset.put("uuid",uuid);
        Date moment = new Date();
        String ts = isoDateFormat.format(moment);
        paramset.put("ts",ts);
       	try{
			new PostObservation().execute(paramset);
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(),"error 385 "+e,Toast.LENGTH_LONG).show();
		}
		try{
			FileOutputStream outputStream;

			try {
				outputStream = openFileOutput(savefile, getApplicationContext().MODE_APPEND);
				// outputStream.write((params+"\n").getBytes());
                // TODO: Find out what to do here - should all calls be logged?
				// TODO: Log as csv for later export / may turn off. warning if neither file nor upload is enabled.
                outputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}catch (Exception e) {
			Toast.makeText(getApplicationContext(),"error 400"+e,Toast.LENGTH_LONG).show();	
		}
        Integer lnum=0;
        try {
            lnum=linenumbers(new File(getFilesDir(), errorfile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        showStatus(lnum.toString());
	}


    public void showStatus(String status){
        if (status.equals("0")) status = ""; else
            status = status + " " + getResources().getString(R.string.setsNotUploaded);
        TextView tvstatus =(TextView)findViewById(R.id.acbar_status);
        tvstatus.setText(status);
		tvstatus =(TextView)findViewById(R.id.acbarFreetext);
        tvstatus.setText(freetext);
    }


	public void saveAct(View v)
	{
	    Button btn=(Button)findViewById(R.id.btnConfirm);
		btn.setEnabled(false);
		keepUnlocked=false;
		btn=(Button)findViewById(R.id.btnUndo);
		btn.setEnabled(true);
        // myTimerThread.resetTime();
        Date moment = new Date();
        String params="";
		HashMap<String, String> paramset = new HashMap<String, String>();
		try{
            if(lServiceBound){
                Location loc=lService.getLocation();
                Double age=0.0;
				if (APILEVEL > 16){
                    age=(loc.getElapsedRealtimeNanos()-SystemClock.elapsedRealtimeNanos())/1e9;
                }else{
                    double currentTime = System.currentTimeMillis();
                    age=(currentTime - loc.getTime())/1000;
                }
				if(age/60>5){
				   throw new Exception(getResources().getString(R.string.errStaleGps));
			  	}
				paramset.put("age",age.toString());
				paramset.put("lat",String.valueOf(loc.getLatitude()));
				paramset.put("lon",String.valueOf(loc.getLongitude()));
				paramset.put("alt",String.valueOf(loc.getAltitude()));
				paramset.put("acc",String.valueOf(loc.getAccuracy()));
				paramset.put("gpstime",String.valueOf(loc.getTime()));
	        }else{
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.GPSServiceNotAvailable),Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.GPSLocationNotAvailable),Toast.LENGTH_LONG);
        }
        String tv=((TextView)findViewById(R.id.tvLastObsType)).getText().toString();
// TODO: Cannot read the values from the textview!
		String drop=tv.substring(tv.lastIndexOf(" ")+1);
        lastdrop = this.dropped;
        String drag=tv.substring(tv.indexOf(":")+2,tv.lastIndexOf(" "));
        lastdrag = this.dragged;
        String ts=isoDateFormat.format(moment);
		String csvline="";
        this.dragged=drag;
        this.dropped=drop;

        try{
			paramset.put("drop",URLEncoder.encode(this.lastdrop));
            paramset.put("ts", URLEncoder.encode( ts));
            paramset.put("drag",URLEncoder.encode(this.lastdrag));
			paramset.put("uuid",uuid);
			paramset.put("username",URLEncoder.encode( username));
			paramset.put("project",URLEncoder.encode(project));
			paramset.put("freetext",URLEncoder.encode(freetext));
            params="";
            for(Map.Entry<String, String> entry : paramset.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!(params.equals(""))) {
					params = params + "&";
					csvline = csvline+",";
				}
                params=params+key+"="+value;
				csvline=csvline+value;
				// TODO:.escape and quote value
            }
    	    new PostObservation().execute(paramset);
		} catch (Exception e) {
            Toast.makeText(getApplicationContext(), "error 541 " + e, Toast.LENGTH_LONG).show();
        }
		freetext="";
		try{
		    FileOutputStream outputStream;
			FileOutputStream CsvStream;
             try {
                outputStream = openFileOutput(savefile, getApplicationContext().MODE_APPEND);
                outputStream.write((params+"\n").getBytes());
                outputStream.close();
                 // debug(csvline); // REMOVE
                 CsvStream = openFileOutput(project+".csv", getApplicationContext().MODE_APPEND);
				CsvStream.write((csvline+"\n").getBytes());
				CsvStream.close();
             } catch (Exception e) {
                e.printStackTrace();
             }
		}catch (Exception e) {
		    Toast.makeText(getApplicationContext(),"error 493 "+e,Toast.LENGTH_LONG).show();
		}
		String otime=timeDateFormat.format(moment);
        lasttimestamp = isoDateFormat.format(moment);
        TextView txtLast = (TextView) findViewById(R.id.tvLastObsType);
        txtLast.setText(getString(R.string.txtLast,otime,drag,drop));

   //     txtLast.setText(otime + ": " + drag + " " + drop);
        Integer lnum=0;
        try {
            lnum=linenumbers(new File(getFilesDir(), errorfile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        showStatus(lnum.toString());
    }


    Integer linenumbers(File file) throws IOException
    {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        int lineCount = 0;
        while ((line = br.readLine()) != null)
            if (!(line.substring(0, 5).equals("Error"))) lineCount++;
        return (lineCount);
    }

	
	private final class MyTouchListener implements OnTouchListener {
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
				ClipData data = ClipData.newPlainText("", "");
				DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
				view.startDrag(data, shadowBuilder, view, 0);
			//	view.setVisibility(View.INVISIBLE);
				return true;
			} else {
				return false;
			}
		}
	}

	class MyDragListener implements OnDragListener {
		final Drawable normalShape = getResources().getDrawable(R.drawable.shape);
	    @Override
		public boolean onDrag(View v, DragEvent event) {
			int action = event.getAction();
			switch (action) {
				case DragEvent.ACTION_DRAG_STARTED:
					// do nothing
					break;
				case DragEvent.ACTION_DRAG_ENTERED:
					break;
				case DragEvent.ACTION_DRAG_EXITED:
					//	v.setBackgroundDrawable(normalShape);
					break;
				case DragEvent.ACTION_DROP:
						break;
				case DragEvent.ACTION_DRAG_ENDED:
                    if (APILEVEL>=16) v.setBackground(normalShape);
				default:
					break;
			}
			return true;
		}
	}
		
		
	class MyDropListener implements OnDragListener {
		final Drawable enterShape = getResources().getDrawable(R.drawable.shape_droptarget);
		final Drawable normalShape = getResources().getDrawable(R.drawable.shape);

	   	@Override
		public boolean onDrag(View v, DragEvent event) {
			int action = event.getAction();
			switch (action) {
				case DragEvent.ACTION_DRAG_STARTED:
					// do nothing
					break;
				case DragEvent.ACTION_DRAG_ENTERED:
                    if (APILEVEL>15)  v.setBackground(enterShape);
					break;
				case DragEvent.ACTION_DRAG_EXITED:
                    if (APILEVEL>15)  v.setBackground(normalShape);
					break;
				case DragEvent.ACTION_DROP:
                    // Dropped, reassign View to ViewGroup
					myTimerThread.resetTime();
                    Date moment = new Date();
                    LinearLayout ll = (LinearLayout) v.getParent();
                    Integer n = ll.getChildCount();
                    String st = "";
                    TextView tv = (TextView) event.getLocalState();
                    String t = tv.getText().toString();
					lastdrag=t;
					dragged=t;
                    TextView txtLast = (TextView) findViewById(R.id.tvLastObsType);
                    String otime=timeDateFormat.format(moment);
                    lasttimestamp=isoDateFormat.format(moment);
                    if (usesec && ll.getChildAt(n - 1) == v) {
                        // StartActivityForResult
                        Intent i = new Intent(getApplicationContext(), itemList.class);
                        i.putExtra("lastdrag", t);
                        i.putExtra("lasttime", lasttimestamp);
                        startActivityForResult(i, 0);
                        // TODO: Get the value from the Intent into st - onactivityresult
                    } else {
                        Object sv = ((ViewGroup) v).getChildAt(0);
                        st = ((TextView) sv).getText().toString();
                        lastdrop=st;
                        dropped=st;

                    }
					Button bt = (Button) findViewById(R.id.btnConfirm);
                    bt.setEnabled(true);
					bt=(Button)findViewById(R.id.btnUndo);
					bt.setEnabled(false);
					//txtLast.setText(otime+": "+t+" "+st);
                    txtLast.setText(getString(R.string.txtLast,otime,t,st));
					break;
				case DragEvent.ACTION_DRAG_ENDED:
                    if (APILEVEL>15) v.setBackground(normalShape);
				default:
					break;
			}
			return true;
		}
	}
	
	
	String formatminsec(long sec){
		String ct="";
		if(sec>3599){ // dont mind zero hours
			long hr=sec/3600;
			ct+=hr+":";
			sec-=hr*3600;
		}
		if(sec > 59){
			long min=sec/60;
			if(min<10){
				ct+="0";
			}
			sec=sec-60*min;
			if(sec < 10){
				ct+=min+":0"+sec;
			}else{
				ct+=min+":"+sec;
			}
		}else{
			ct+="00:";
			if(sec<10){
			   ct+="0";
			}
			ct+=sec;}
		return(ct);
	}
	
		
		void doWork(final long startTime){
			runOnUiThread(new Runnable(){
					public void run(){
						try{
							TextView txtTimer = (TextView)findViewById(R.id.tvWaitTime);
							Date dt= new Date();
							long sec=dt.getTime();
							long etime=(sec-startTime)/1000;
							sec=etime;
							if(sec>timeout && timeout > 0){
								// undo timeout. to be set in settings
								Button bt=(Button)findViewById(R.id.btnUndo);
								bt.setEnabled(false);
								bt=(Button)findViewById(R.id.btnConfirm);
								bt.setEnabled(keepUnlocked);		
							}
							String ct="-";
							if(!lastdrop.equals("")){
								ct=formatminsec(sec);
							}
							txtTimer.setText(ct);
							if(waitmins >0){
								ct="0";
								if(!lastdrop.equals("")){
									long waittime=waitmins*60-etime;
									LinearLayout ll =(LinearLayout)findViewById(R.id.llMiddle);
									if(waittime < 0){
										waittime=0;
										ll.setBackgroundColor(Color.RED);
										
									}else{
										if(waittime==0){
											if(!quietMode){	
											Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
										// Vibrate for 500 milliseconds
											if (v.hasVibrator()) {
												v.vibrate(500);
											} else {
												debug("Cannot Vibrate");
											}
											}
										}
										ll.setBackgroundColor(Color.WHITE);
									}
									ct=formatminsec(waittime);
								}
							}else{
								ct="";
							}
							txtTimer = (TextView)findViewById(R.id.tvTimeToNext);
							txtTimer.setText(ct);
						}catch(Exception e){}
					}
				});             
		}


		class ShowTimeRunner implements Runnable
		{
			private long startTime=new Date().getTime();
			public void resetTime(){
			this.startTime=new Date().getTime();
			}
			
			public void setTime(String time) throws java.text.ParseException {
					this.startTime=isoDateFormat.parse(time).getTime();
			}
			
			public long getTime(){
				long time=new Date().getTime()-this.startTime;
				return(time);
			}
			
            @Override
			public void run()
			{
				while(!Thread.currentThread().isInterrupted()){
					try{
						doWork(startTime);
						Thread.sleep(1000);
					}catch(InterruptedException e){
						Thread.currentThread().interrupt();
					}catch(Exception e){}
				}
			}
		}
		
		private class PostObservation extends AsyncTask<HashMap<String,String>, Void,Integer>{

			private Exception exception;
			private Integer status;

			protected Integer doInBackground(HashMap<String,String>... paramsets){
                HashMap<String,String> paramset = new HashMap<String, String>();
                paramset=paramsets[0];
                String params=paramset.get("parameters");
                if(params == null) {
                    params="";
                    for (Map.Entry<String, String> entry : paramset.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        if (!(params.equals(""))) params = params + "&";
                        params = params + key + "=" + value;
                    }
                }
                try{
                // TODO: Rewrite to use post / json
                   // params=URLEncoder.encode(params,"UTF-8");
					URL url = new URL(urlString+"?"+params);
					
                    URLConnection conn = url.openConnection();
					BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream()));
					String inputLine;
					while((inputLine = in.readLine())!=null) {
                       // reading the servers answer. Ignoring what we get. May check for correct return later
                       // Toast.makeText(getApplicationContext(), "result " + inputLine, Toast.LENGTH_LONG).show();
                    }
					in.close();
				}catch(Exception e){
					this.exception=e;
					status=0;
					
					FileOutputStream outputStream;

					try {
						outputStream = openFileOutput(errorfile, getApplicationContext().MODE_APPEND);
						outputStream.write(("Error 721 "+e.getMessage()+"\n").getBytes());
						outputStream.write((params+"\n").getBytes());
						outputStream.close();
					} catch (Exception fe) {
						fe.printStackTrace();
					}

					return null;
				}
                status=10;
				return status;
			}




			protected void onPostExecute(Long res){
				if(status==0){
					Toast.makeText(getApplicationContext()," upload error", Toast.LENGTH_SHORT).show();
				}
			}

		}
		
}


