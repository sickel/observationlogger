package com.mortensickel.obslogger;

import java.text.*;
import java.util.Arrays;
import java.util.Date;
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
import java.util.Scanner;
import java.util.ArrayList;

import android.widget.LinearLayout.LayoutParams;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.service.notification.*;
import android.util.Log;
import android.os.IBinder;
import android.content.ServiceConnection;
import com.mortensickel.obslogger.LocationService.*;
import android.content.*;

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
    //private final String uuid="txt";
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        uuid=Installation.id(getApplicationContext());
      //  Toast.makeText(getApplicationContext(),urlString+" ",Toast.LENGTH_SHORT).show();
        setContentView(R.layout.main);
		Thread showtimeThread = null;
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
		Intent intent=new Intent(this, LocationService.class);
		intent.setAction("startListening");
		startService(intent);
	//	bindService(intent,lServiceConnection,Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onStop(){
		super.onStop();
		if(lServiceBound){
			unbindService(lServiceConnection);
			lServiceBound=false;
		}
	}
	
    @Override
    public void onResume() {
        // Rewrite the parts on drag and drop names to avoid code redundancy
        super.onResume();  // Always call the superclass method first
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        urlString=sharedPrefs.getString("uploadURL", "");
        username=sharedPrefs.getString("userName","");
        project=sharedPrefs.getString("projectName","");
        timeout=Integer.parseInt(sharedPrefs.getString("pref_timeout","20"));
        String dragnames=sharedPrefs.getString("dragNames", String.valueOf(R.string.dragnames));
        Log.w("obslog","test");
		List<String> drags= Arrays.asList(dragnames.split("\\s*,\\s*"));
        // String[] drags={"240","242","244","245","260"};
        ViewGroup ll =(ViewGroup)findViewById(R.id.dragzones);
        ll.removeAllViews();
        for (String drag : drags) {
            LinearLayout b = new LinearLayout(this);
            b.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
            TextView tv = new TextView(this);
            tv.setText(drag);
            tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            tv.setGravity(Gravity.CENTER);
            b.addView(tv);
            b.setBackgroundResource(R.drawable.shape);
            b.setOnDragListener(new MyDragListener());
            tv.setOnTouchListener(new MyTouchListener());
            ll.addView(b);
        }
        String dropnames=sharedPrefs.getString("dropNames", String.valueOf(R.string.dropnames));
        List<String> drops= Arrays.asList(dropnames.split("\\s*,\\s*"));
        //String[] drops={"Grazing","Resting","Walking","Other"};
        ll =(ViewGroup)findViewById(R.id.dropzones);
        ll.removeAllViews();
        for (String drop : drops) {
            LinearLayout b = new LinearLayout(this);
            b.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
            TextView tv = new TextView(this);
            tv.setText(drop);
            tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            tv.setGravity(Gravity.CENTER);
            b.addView(tv);
            b.setBackgroundResource(R.drawable.shape);
            b.setOnDragListener(new MyDropListener());
            ll.addView(b);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

        }

        return true;
    }

	
	public void saveObs(){
		/* Reading saved observations and reupload those that are not uploaded 
		   Rewrite file if changesaaaaaä*/
	//	Toast.makeText(getApplicationContext(),urlString+" ",Toast.LENGTH_SHORT).show();
		ArrayList<String> linelist = new ArrayList<String>();
		try{
			InputStream is=openFileInput(errorfile);
			BufferedReader rdr =new BufferedReader(new InputStreamReader(is));
			String myLine; 
			while((myLine=rdr.readLine())!=null) linelist.add(myLine);	
		}catch(Exception e){
			Toast.makeText(getApplicationContext(),"File read error",Toast.LENGTH_SHORT).show();
		}
		Toast.makeText(getApplicationContext(),"So far so good "+linelist.size(),Toast.LENGTH_SHORT).show();
		for(String line :linelist){
		//	if(line.charAt(0)=="-")
		}
	}

    public void undoAct(View v){
	
		Button btn=(Button)findViewById(R.id.btnUndo);
		btn.setEnabled(false);
		String params="undo";
		String status="";
		try{
			URL url = new URL(urlString+"?"+params);
			new PostObservation().execute(url);
		} catch (Exception e) {
			status="-";
			Toast.makeText(getApplicationContext(),"error "+e,Toast.LENGTH_LONG).show();	
		}
		try{
			String sep =";";
			FileOutputStream outputStream;

			try {
				outputStream = openFileOutput(savefile, getApplicationContext().MODE_APPEND);
				outputStream.write((status+params+"\n").getBytes());
				outputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}catch (Exception e) {
			Toast.makeText(getApplicationContext(),"error "+e,Toast.LENGTH_LONG).show();	
		}
		
	}
	public void saveAct(View v)
	{
		//Toast.makeText(getApplicationContext(), "Clicked on Button ", Toast.LENGTH_LONG).show();
		Button btn=(Button)findViewById(R.id.btnConfirm);
		btn.setEnabled(false);
		btn=(Button)findViewById(R.id.btnUndo);
		btn.setEnabled(true);
		myTimerThread.resetTime();
		Date moment = new Date();
		String tv=((TextView)findViewById(R.id.tvLastObsType)).getText().toString();
		 String drop=tv.substring(tv.lastIndexOf(" ")+1);
		 String drag=tv.substring(tv.indexOf(":")+2,tv.lastIndexOf(" "));
		 String ts=new SimpleDateFormat("yyyy-MM-dd+HH.mm.ss").format(moment);
		 String status="";
		 String params="";
		 try{
		    params="drop="+drop+"&ts="+ts+"&drag="+drag+"&uuid="+uuid+"&username="+username+"&project="+project;
		    URL url = new URL(urlString+"?"+params);
		    new PostObservation().execute(url);
			status="+";
		 } catch (Exception e) {
		    status="-";
		    Toast.makeText(getApplicationContext(),"error "+e,Toast.LENGTH_LONG).show();
		 }
		 try{
		    String sep =";";
		    FileOutputStream outputStream;

             try {
                outputStream = openFileOutput(savefile, getApplicationContext().MODE_APPEND);
                outputStream.write((status+params+"\n").getBytes());
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
			switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED:
					// do nothing
					break;
				case DragEvent.ACTION_DRAG_ENTERED:
					break;
				case DragEvent.ACTION_DRAG_EXITED:
				//	v.setBackgroundDrawable(normalShape);
					break;
				case DragEvent.ACTION_DROP:
					// Dropped, reassign View to ViewGroup
					/*View view = (View) event.getLocalState();
					ViewGroup owner = (ViewGroup) view.getParent();
					owner.removeView(view);
					LinearLayout container = (LinearLayout) v;
					container.addView(view);
					view.setVisibility(View.VISIBLE);*/
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
			switch (event.getAction()) {
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
					while((inputLine = in.readLine())!=null){}
					//	showDialog(inputLine);
					//	Toast.makeText(this, "result "+inputLine, Toast.LENGTH_LONG).show();
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


