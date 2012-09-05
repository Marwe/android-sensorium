package at.univie.seattlesensors;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import at.univie.seattlesensors.sensors.BatterySensor;
import at.univie.seattlesensors.sensors.BluetoothSensor;
import at.univie.seattlesensors.sensors.DummySensor;
import at.univie.seattlesensors.sensors.GPSLocationSensor;
import at.univie.seattlesensors.sensors.NetworkLocationSensor;
import at.univie.seattlesensors.sensors.RadioSensor;
import at.univie.seattlesensors.sensors.WifiSensor;

public class SensorService extends Service {
	private SensorRegistry registry;

	private static final int NOTIFICATION = 42;
	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		Log.d("LocalService", "Received bind intent");
		startSensors();
		return mBinder;
	}
	public class LocalBinder extends Binder {
		SensorService getService() {
			return SensorService.this;
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("LocalService", "Received start id " + startId + ": " + intent);
		startSensors();
		return START_STICKY;
	}
	
	private void startSensors(){
		registry = SensorRegistry.getInstance();
		registry.registerSensor(new RadioSensor(this));
		registry.registerSensor(new NetworkLocationSensor(this));
		registry.registerSensor(new GPSLocationSensor(this));
		registry.registerSensor(new BatterySensor(this));
		registry.registerSensor(new DummySensor(this));
		registry.registerSensor(new WifiSensor(this));
		registry.registerSensor(new BluetoothSensor(this));
		registry.startup();

		// start the XMLRPC server
		if (!XMLRPCSensorServerThread.running)
			(new Thread(new XMLRPCSensorServerThread())).start();
		else
			Log.d("SeattleSensors",
					"Thread already running, not spawning another one.");

	}
	
	@Override
	public void onCreate() {
		Notification sensorsNot = new Notification();
		sensorsNot.icon = R.drawable.ic_launcher;
		sensorsNot.when = System.currentTimeMillis();
		Intent notificationIntent = new Intent(this, SeattleSensorsActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		sensorsNot.setLatestEventInfo(this, "SeattleSensors", "running", contentIntent);
		((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION, sensorsNot);
	}



	@Override
	public void onDestroy() {
		((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION);
		Log.d("SeattleSensors", "SeattleSensors stopped");
	}

	public SensorRegistry getSensorRegistry() {
		return registry;
	}
}