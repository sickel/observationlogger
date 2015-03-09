package com.mortensickel.obslogger;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// TODO: View log of stored data, export them
// TODO: Fetch settings data from server
// TODO: Activity to list more alternatives
// TODO: Possibility to type in observations
// TODO: Photo

public class MainActivity extends Activity {
	LocationService lService;

    private static String LOGTAG="Obslogger";
    private String lastdrag = "";
    private String lastdrop = "";
    private String lasttimestamp = "";
    boolean lServiceBound=false;
	private String urlString="http://hhv3.sickel.net/beite/storeobs.php";
    private boolean doUpload=true;
    private String savefile="observations.dat";
	private String errorfile="errors.dat";
	private String project;
    private final ShowTimeRunner myTimerThread = new ShowTimeRunner();
    private static final int RESULT_SETTINGS = 1;
    private static final int APILEVEL= Build.VERSION.SDK_INT;
    private String uuid;
    private String username, freetext;
    private int timeout=10;
	private static final int ACTIVITY_ITEMLIST=0;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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


    private ServiceConnection lServiceConnection;

    // www.trution.com/2014/11/bound-service-example-android/
	@Override
	protected void onStart(){
		super.onStart();
		startGPS();
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
/*		freetext="";
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getString("lastdrop") != null) lastdrop = extras.getString("lastdrop");
            if (extras.getString("lastdrag") != null) lastdrag = extras.getString("lastdrag");
            if (extras.getString("lasttime") != null) lasttimestamp = extras.getString("lasttime");
				//	Toast.makeText(getApplicationContext(),extras.getString("freetext"),Toast.LENGTH_SHORT).show();
            Button bt = (Button) findViewById(R.id.btnConfirm);
            bt.setEnabled(true);
            myTimerThread.resetTime();
        }*/
        showStatus(lnum.toString());
        TextView txtLast = (TextView) findViewById(R.id.tvLastObsType);
        txtLast.setText(lasttimestamp + ": " + lastdrag + " " + lastdrop);

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
		switch(requestCode) { 
			case (ACTIVITY_ITEMLIST) : { 
				if (resultCode == Activity.RESULT_OK) { 
						Bundle extras = i.getExtras();
						
						if (extras.getString("lastdrop") != null) lastdrop = extras.getString("lastdrop");
						if (extras.getString("lastdrag") != null) lastdrag = extras.getString("lastdrag");
						if (extras.getString("lasttime") != null) lasttimestamp = extras.getString("lasttime");
						if (extras.getString("freetext") != null) freetext = extras.getString("freetext");	
						Button bt = (Button) findViewById(R.id.btnConfirm);
						bt.setEnabled(true);
						myTimerThread.resetTime();
					
					
				//	String newText = data.getStringExtra(PUBLIC_STATIC_STRING_IDENTIFIER);
						// TODO Update your TextView.
					} 
					break; 
				} 
		} 
	}
	
    public void setZones(ViewGroup ll, String names){
       // rewrite to objects to avoid the if
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
            case R.id.menu_testlist:
                // TODO: Remove this when this is displayed through a drop
                Intent myIntent = new Intent(getApplicationContext(), itemList.class);
                startActivityForResult(myIntent, 0);
                break;

        }

        return true;
    }

	
	public void toggleGPS(){
		if(lServiceBound){
			Toast.makeText(getApplicationContext(),getResources().getString( R.string.stopping_gps),Toast.LENGTH_SHORT).show();
			stopGPS();
		}else{
			Toast.makeText(getApplicationContext(),getResources().getString( R.string.starting_gps),Toast.LENGTH_SHORT).show();
			startGPS();
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
			if(from.exists())
				from.renameTo(to);
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
				Toast.makeText(getApplicationContext(),"error "+e,Toast.LENGTH_LONG).show();
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
        String ts = new SimpleDateFormat("yyyy-MM-dd+HH.mm.ss+z").format(moment);
        paramset.put("ts",ts);
       	try{
			new PostObservation().execute(paramset);
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(),"error "+e,Toast.LENGTH_LONG).show();
		}
		try{
			FileOutputStream outputStream;

			try {
				outputStream = openFileOutput(savefile, getApplicationContext().MODE_APPEND);
				// outputStream.write((params+"\n").getBytes());
                // TODO: Find out what to do here - should all calls be logged?
                outputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}catch (Exception e) {
			Toast.makeText(getApplicationContext(),"error "+e,Toast.LENGTH_LONG).show();	
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
    }


	public void saveAct(View v)
	{
	    Button btn=(Button)findViewById(R.id.btnConfirm);
		btn.setEnabled(false);
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
				if (APILEVEL > 16){  age=(loc.getElapsedRealtimeNanos()-SystemClock.elapsedRealtimeNanos())/1e9;}
				if(age/60>5){
				   throw new Exception("Stale gps");
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
		String drop=tv.substring(tv.lastIndexOf(" ")+1);
        lastdrop = drop;
        String drag=tv.substring(tv.indexOf(":")+2,tv.lastIndexOf(" "));
        lastdrag = drag;
        String ts=new SimpleDateFormat("yyyy-MM-dd+HH.mm.ss").format(moment);
		try{
			paramset.put("drop",drop);
            paramset.put("ts", ts);
            paramset.put("drag",drag);
			paramset.put("uuid",uuid);
			paramset.put("username",username);
			paramset.put("project",project);
			paramset.put("freetext",freetext);
	
            params="";
            for(Map.Entry<String, String> entry : paramset.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!(params.equals(""))) params = params + "&";
                params=params+key+"="+value;
            }
    	    new PostObservation().execute(paramset);
		} catch (Exception e) {
		    Toast.makeText(getApplicationContext(),"error "+e,Toast.LENGTH_LONG).show();
		}
		freetext="";
		try{

		    FileOutputStream outputStream;

             try {
                outputStream = openFileOutput(savefile, getApplicationContext().MODE_APPEND);
                outputStream.write((params+"\n").getBytes());
                outputStream.close();
             } catch (Exception e) {
                e.printStackTrace();
             }
		}catch (Exception e) {
		    Toast.makeText(getApplicationContext(),"error "+e,Toast.LENGTH_LONG).show();
		}
		String otime=new SimpleDateFormat("HH.mm.ss").format(moment);
        lasttimestamp = otime;
        TextView txtLast = (TextView) findViewById(R.id.tvLastObsType);
        txtLast.setText(otime + ": " + drag + " " + drop);
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
                    TextView txtLast = (TextView) findViewById(R.id.tvLastObsType);
                    String otime=new SimpleDateFormat("HH.mm.ss").format(moment);
                    if (ll.getChildAt(n - 1) == v) {
                        // TODO: Check if last child - if so open activity itemlist
                        //   Toast.makeText(getApplicationContext(),"Last child",Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(getApplicationContext(), itemList.class);
                        i.putExtra("lastdrag", t);
                        i.putExtra("lasttime", otime);
                        startActivityForResult(i, 0);
                    } else {
                        Object sv = ((ViewGroup) v).getChildAt(0);
                        st = ((TextView) sv).getText().toString();
                    }
                    Button bt = (Button) findViewById(R.id.btnConfirm);
                    bt.setEnabled(true);
					bt=(Button)findViewById(R.id.btnUndo);
					bt.setEnabled(false);
					txtLast.setText(otime+": "+t+" "+st);
					break;
				case DragEvent.ACTION_DRAG_ENDED:
                    if (APILEVEL>15) v.setBackground(normalShape);
				default:
					break;
			}
			return true;
		}
		
		
		
		
		
		}

		void doWork(final long startTime){
			runOnUiThread(new Runnable(){
					public void run(){
						try{
							TextView txtTimer = (TextView)findViewById(R.id.tvWaitTime);
							Date dt= new Date();
							long sec=dt.getTime();
							sec=(sec-startTime)/1000;

							if(sec>timeout && timeout > 0){
								// undo timeout. to be set in settings
								Button bt=(Button)findViewById(R.id.btnUndo);
								bt.setEnabled(false);
								bt=(Button)findViewById(R.id.btnConfirm);
								bt.setEnabled(false);
								
							}
							String ct;
                            if(sec > 59){
							// TODO: hour if > 60 min
								long min=sec/60;

                                //if(min )
								sec=sec-60*min;
								if(sec < 10){
									ct=min+":0"+sec;
								}else{
									ct=min+":"+sec;
								}
							}else{
								ct= sec+ "s";}
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
						outputStream.write(("Error "+e.getMessage()+"\n").getBytes());
						outputStream.write((params+"\n").getBytes());
						outputStream.close();
					} catch (Exception fe) {
						fe.printStackTrace();
					}
				//	Toast.makeText(getApplicationContext()," upload error caught", Toast.LENGTH_SHORT).show();
					
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


