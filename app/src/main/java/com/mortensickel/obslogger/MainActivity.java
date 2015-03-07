package com.mortensickel.obslogger;

import java.text.*;
import java.util.Arrays;
import java.util.Date;
import android.app.ActionBar;
import android.location.Location;
import android.util.TypedValue;
import android.os.*;
import android.view.Gravity;
import android.widget.*;
import android.app.Activity;
import android.content.ClipData;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
// import android.widget.LinearLayout;
import java.io.*;
import java.net.*;
import java.util.List;
//import java.util.Scanner;
import java.util.ArrayList;

import android.widget.LinearLayout.LayoutParams;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
// import android.util.Log;
import android.os.IBinder;
import android.content.ServiceConnection;
import android.content.*;
import com.mortensickel.obslogger.LocationService.*;
import android.os.SystemClock;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class MainActivity extends Activity {
	LocationService lService;
	boolean lServiceBound=false;
	private String urlString="http://hhv3.sickel.net/beite/storeobs.php";
    private boolean doUpload=true;
    private String savefile="observations.dat";
	private String errorfile="errors.dat";
	private String project;
    private final ShowTimeRunner myTimerThread = new ShowTimeRunner();
	/** Called when the activity is first created. */
    private static final int RESULT_SETTINGS = 1;
    private String uuid;
    private String username;
    private int timeout=10;
    // Messenger lService = null;
    //private final String uuid="txt";





	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	//	setContentView(R.layout.activity_main);
	
		uuid=Installation.id(getApplicationContext());
		//  Toast.makeText(getApplicationContext(),urlString+" ",Toast.LENGTH_SHORT).show();
        setContentView(R.layout.main);
		
		ActionBar actionBar = getActionBar();
		// add the custom view to the action bar
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
       // Toast.makeText(getApplicationContext(),"SFSG",Toast.LENGTH_SHORT).show();
        Integer lnum=0;
        try {
            lnum=linenumbers(new File(getFilesDir(), errorfile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        showStatus(lnum.toString());
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
			//	Toast.makeText(getApplicationContext(),"toggler 1",Toast.LENGTH_SHORT).show();
				toggleGPS();	
				break;

        }

        return true;
    }

	
	public void toggleGPS(){
	//	Toast.makeText(getApplicationContext(),"Toggler",Toast.LENGTH_SHORT).show();
		
		if(lServiceBound){	
			Toast.makeText(getApplicationContext(),getResources().getString( R.string.stopping_gps),Toast.LENGTH_SHORT).show();
			stopGPS();
		}else{
			Toast.makeText(getApplicationContext(),getResources().getString( R.string.starting_gps),Toast.LENGTH_SHORT).show();
			startGPS();
		}
		
	}
	
	
	public void saveObs(){
		/* Reading saved observations and reupload those that are not uploaded 
		   Rewrite file if changes*/
	//	Toast.makeText(getApplicationContext(),urlString+" ",Toast.LENGTH_SHORT).show();
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
					URL url = new URL(line);
					new PostObservation().execute(url);
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
        TextView tvstatus =(TextView)findViewById(R.id.acbar_status);
        tvstatus.setText(lnum.toString());
	}
/*

// Why does this not work when I pull it out in a function?

    public String hashMapToString(HashMap paramset){
        String params="";
        for(Map.Entry<String, String> entry : paramset.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (params.equals("")) params = params + "&";
            params=params+key+"="+(String)value;
        }
        return(params);
    }
*/


    public void undoAct(View v){
	
		Button btn=(Button)findViewById(R.id.btnUndo);
		btn.setEnabled(false);
        HashMap<String, String> paramset = new HashMap<String, String>();
        paramset.put("undo","undo");
        paramset.put("uuid",uuid);
        Date moment = new Date();
        String ts=new SimpleDateFormat("yyyy-MM-dd+HH.mm.ss").format(moment);
		paramset.put("ts",ts.toString());
       	//String params=hashMapToString(paramset);
        String params="";
        for(Map.Entry<String, String> entry : paramset.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!(params.equals(""))) params = params + "&";
            params=params+key+"="+value;
        }
        try{
			URL url = new URL(urlString+"?"+params);
			new PostObservation().execute(url);
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(),"error "+e,Toast.LENGTH_LONG).show();
		}
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
        Integer lnum=0;
        try {
            lnum=linenumbers(new File(getFilesDir(), errorfile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        showStatus(lnum.toString());
	}


    public void showStatus(String status){
        TextView tvstatus =(TextView)findViewById(R.id.acbar_status);
        tvstatus.setText(status);
    }


	public void saveAct(View v)
	{
	
		//Toast.makeText(getApplicationContext(), urlString, Toast.LENGTH_LONG).show();
		Button btn=(Button)findViewById(R.id.btnConfirm);
		btn.setEnabled(false);
		btn=(Button)findViewById(R.id.btnUndo);
		btn.setEnabled(true);
		myTimerThread.resetTime();
		Date moment = new Date();
        String params="";
		HashMap<String, String> paramset = new HashMap<String, String>();
		try{
            if(lServiceBound){
                Location loc=lService.getLocation();
				Double age =(loc.getElapsedRealtimeNanos()-SystemClock.elapsedRealtimeNanos())/1e9;
				if(age/60>5){
				   throw new Exception("Stale gps");
			  	}
			//	Toast.makeText(getApplicationContext(),loc.toString(),Toast.LENGTH_SHORT).show();
				paramset.put("age",age.toString());
				paramset.put("lat",String.valueOf(loc.getLatitude()));
				paramset.put("lon",String.valueOf(loc.getLongitude()));
				paramset.put("alt",String.valueOf(loc.getAltitude()));
				paramset.put("acc",String.valueOf(loc.getAccuracy()));
				paramset.put("gpstime",String.valueOf(loc.getTime()));
				//params="&lat="+String.valueOf(loc.getLatitude())+"&lon="+String.valueOf(loc.getLongitude())+"&alt="+String.valueOf(loc.getAltitude())+"&acc="+String.valueOf(loc.getAccuracy())+"&gpstime="+String.valueOf(loc.getTime());
            }else{
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.GPSServiceNotAvailable),Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.GPSLocationNotAvailable),Toast.LENGTH_LONG);
        }
        String tv=((TextView)findViewById(R.id.tvLastObsType)).getText().toString();
		String drop=tv.substring(tv.lastIndexOf(" ")+1);
		String drag=tv.substring(tv.indexOf(":")+2,tv.lastIndexOf(" "));
		String ts=new SimpleDateFormat("yyyy-MM-dd+HH.mm.ss").format(moment);

		try{
			paramset.put("drop",drop);
			paramset.put("ts",ts.toString());
			paramset.put("drag",drag);
			paramset.put("uuid",uuid);
			paramset.put("username",username);
			paramset.put("project",project);
            params="";
            for(Map.Entry<String, String> entry : paramset.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!(params.equals(""))) params = params + "&";
                params=params+key+"="+value;
            }

			//params="drop="+drop+"&ts="+ts+"&drag="+drag+"&uuid="+uuid+"&username="+username+"&project="+project+params;
		    URL url = new URL(urlString+"?"+params);
		    new PostObservation().execute(url);
		} catch (Exception e) {
		    Toast.makeText(getApplicationContext(),"error "+e,Toast.LENGTH_LONG).show();
		}
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
		TextView txtLast = (TextView)findViewById(R.id.tvLastObsType);
		String otime=new SimpleDateFormat("HH.mm.ss").format(moment);
		txtLast.setText(otime+": "+drag+" "+drop);
        Integer lnum=0;
        try {
            lnum=linenumbers(new File(getFilesDir(), errorfile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        TextView tvstatus =(TextView)findViewById(R.id.acbar_status);
        tvstatus.setText(lnum.toString());
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
					v.setBackground(normalShape);
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
					v.setBackground(enterShape);
					break;
				case DragEvent.ACTION_DRAG_EXITED:
					v.setBackground(normalShape);
					break;
				case DragEvent.ACTION_DROP:
					// Dropped, reassign View to ViewGroup
					myTimerThread.resetTime();
					Date moment = new Date();		
					TextView tv = (TextView)event.getLocalState();
					String t=tv.getText().toString();
					Object sv = ((ViewGroup)v).getChildAt(0);
					String st = ((TextView)sv).getText().toString();	
				//	Toast.makeText(getApplicationContext()," Dragged "+t+" to "+st, Toast.LENGTH_SHORT).show();
					TextView txtLast = (TextView)findViewById(R.id.tvLastObsType);
					String otime=new SimpleDateFormat("HH.mm.ss").format(moment);
					Button bt=(Button)findViewById(R.id.btnConfirm);
					bt.setEnabled(true);
					bt=(Button)findViewById(R.id.btnUndo);
					bt.setEnabled(false);
					txtLast.setText(otime+": "+t+" "+st);
					break;
				case DragEvent.ACTION_DRAG_ENDED:
					v.setBackground(normalShape);
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
								long min=sec/60;
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
				// TODO: Implement this method
			}
		}
		
		private class PostObservation extends AsyncTask<URL, Void,Integer>{

			private Exception exception;
			private Integer status;
			protected Integer doInBackground(URL... urls){	
				try{
					URLConnection conn = urls[0].openConnection();
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
						outputStream.write((urls[0]+"\n").getBytes());
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


