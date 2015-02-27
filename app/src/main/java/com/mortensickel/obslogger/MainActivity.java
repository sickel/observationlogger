package com.mortensickel.obslogger;

import java.text.*;
import java.util.Date;
import android.app.*;
import android.os.*;
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
import android.widget.LinearLayout;
import java.io.*;
import java.net.*;
public class MainActivity extends Activity {
	public String urlString="http://hhv3.sickel.net/beite/storeobs.php";
    public String savefile="observations.dat";
	ShowTimeRunner myTimerThread = new ShowTimeRunner();
	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Thread showtimeThread = null;
		showtimeThread = new Thread(myTimerThread);
		showtimeThread.start();
	    ViewGroup ll =(ViewGroup)findViewById(R.id.dragzones);
		int i;
		for(i=0;i<ll.getChildCount();i++){
			ll.getChildAt(i).setOnDragListener(new MyDragListener());
			((LinearLayout)ll.getChildAt(i)).getChildAt(0).setOnTouchListener(new MyTouchListener());
		}
		ll =(ViewGroup)findViewById(R.id.dropzones);
		for(i=0;i<ll.getChildCount();i++){
			ll.getChildAt(i).setOnDragListener(new MyDropListener());
		}
		Button bt=(Button)findViewById(R.id.btnConfirm);
		bt.setEnabled(false);
		bt=(Button)findViewById(R.id.btnUndo);
		bt.setEnabled(false);
		
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
			//	String path = Context.   .getFilesææææ.getAbsolutePath();
			String sep =";";
			///	String string = status+ts+sep+cowid+sep+bt+"\n";
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
		 String bt=tv.substring(tv.lastIndexOf(" ")+1);
		 //	Spinner cowidspinner=(Spinner)findViewById(R.id.cowidSpinner);
		 String cowid=tv.substring(tv.indexOf(":")+1,tv.lastIndexOf(" "));
		 //String.valueOf(cowidspinner.getSelectedItem());
		 //
		 String ts=new SimpleDateFormat("yyyy-MM-dd+HH.mm.ss").format(moment);
		 String status="+";
		 String params="";
		 try{
		 params="activity="+bt+"&ts="+ts+"&cowid="+cowid;
		 URL url = new URL(urlString+"?"+params);
		 new PostObservation().execute(url);
		 } catch (Exception e) {
		 status="-";
		 Toast.makeText(getApplicationContext(),"error "+e,Toast.LENGTH_LONG).show();	
		 }
		 try{
		 //	String path = Context.   .getFilesææææ.getAbsolutePath();
		 String sep =";";
		 ///	String string = status+ts+sep+cowid+sep+bt+"\n";
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

		 txtLast.setText(otime+": "+cowid+" "+bt);
		 
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
		Drawable enterShape = getResources().getDrawable(R.drawable.shape_droptarget);
		Drawable normalShape = getResources().getDrawable(R.drawable.shape);

		@Override
		public boolean onDrag(View v, DragEvent event) {
			int action = event.getAction();
			switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED:
					// do nothing
					break;
				case DragEvent.ACTION_DRAG_ENTERED:
					//v.setBackgroundDrawable(enterShape);
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
					v.setBackgroundDrawable(normalShape);
				default:
					break;
			}
			return true;
		}
		
		
		
		
		
		
		}
		
		
		class MyDropListener implements OnDragListener {
		Drawable enterShape = getResources().getDrawable(R.drawable.shape_droptarget);
		Drawable normalShape = getResources().getDrawable(R.drawable.shape);

		@Override
		public boolean onDrag(View v, DragEvent event) {
			int action = event.getAction();
			switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED:
					// do nothing
					break;
				case DragEvent.ACTION_DRAG_ENTERED:
					v.setBackgroundDrawable(enterShape);
					break;
				case DragEvent.ACTION_DRAG_EXITED:
					v.setBackgroundDrawable(normalShape);
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
					//
					/*	View view = (View) 
					ViewGroup owner = (ViewGroup) view.getParent();
					owner.removeView(view);
					LinearLayout container = (LinearLayout) v;
					container.addView(view);
					view.setVisibility(View.VISIBLE);*/
					break;
				case DragEvent.ACTION_DRAG_ENDED:
					v.setBackgroundDrawable(normalShape);
				default:
					break;
			}
			return true;
		}
		
		
		
		
		
		}

		public void doWork(final long startTime){
			runOnUiThread(new Runnable(){
					public void run(){
						try{
							TextView txtTimer = (TextView)findViewById(R.id.tvWaitTime);
							Date dt= new Date();
							long sec=dt.getTime();
							sec=(sec-startTime)/1000;
							if(sec>20){
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
		
		private class PostObservation extends AsyncTask<URL, Void,Long>{

			private Exception exception;
			protected Long doInBackground(URL... urls){	
				try{
					URLConnection conn = urls[0].openConnection();
					BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream()));
					String inputLine;
					while((inputLine = in.readLine())!=null)
					//	showDialog(inputLine);
					//	Toast.makeText(this, "result "+inputLine, Toast.LENGTH_LONG).show();
					in.close();
				}catch(Exception e){
					this.exception=e;
					return null;
				}

				long retval=10;
				return retval;
			}

			protected void onPostExecute(Long res){
				//showDialog("Ok");
			}

		}


		
			
}
