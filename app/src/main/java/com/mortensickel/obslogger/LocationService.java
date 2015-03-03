package com.mortensickel.obslogger;
// http://stackoverflow.com/questions/7759504/access-locationmanager-locationlistener-from-class
import java.util.Timer;
import java.util.TimerTask;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.app.Service;
import android.os.IBinder;

public class LocationService extends Service implements LocationListener {

    private LocationManager locationManager;

    private Location location;
	private IBinder lBinder;
	private static String LOGTAG="Locservice";
    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
		Log.v(LOGTAG,"in onstart");
      //  Logging.i("CLAZZ", "onHandleIntent", "invoked");
if(intent!=null){
	Log.v(LOGTAG,"in onstart ... intent not null");
	Log.v(LOGTAG,intent.getAction());
	Log.v(LOGTAG,"in onstart ... intent not null - after");
	 if (intent.getAction().equals("startListening")) {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
        else {
            if (intent.getAction().equals("stopListening")) {
                locationManager.removeUpdates(this);
                locationManager = null;
            }
        }
}
        return START_STICKY;

    }

    @Override
    public IBinder onBind(final Intent intent) {
        return lBinder;
    }

    public void onLocationChanged(final Location location) {
        this.location = location;   
        // TODO this is where you'd do something like context.sendBroadcast()
    }

    public void onProviderDisabled(final String provider) {
    }

    public void onProviderEnabled(final String provider) {
    }

    public void onStatusChanged(final String arg0, final int arg1, final Bundle arg2) {
    }

	}
	
