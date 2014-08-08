package fr.asterope;

import android.app.Activity;
import android.app.AlertDialog;
import static android.app.AlertDialog.THEME_HOLO_DARK;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationProvider;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.TextView;
import java.util.Timer;
import java.util.TimerTask;
import android.text.format.Time;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity implements LocationListener
{

    private ExternalFileLogger logs = null;
    private LocationManager gps = null;
    private Location last_position = null;
    private boolean started = false;
    private float inst_speed = 0.0f;        // Speed in m.s-1
    private float average_speed = 0.0f;     // Average Speed in m.s-1
    private float distance = 0.0f;          // Integrated distance in m.
    private long update_count = 0;          // gps update counter.
    private float elapsed_seconds = 0;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String gpsStatus = "";
    private int satelliteNumber = 0;
    private long pauseStarted = -1;
    private String logFilename = null;
    private boolean firstGPSFixReceived = false;

    // Elevation Gain related variables.
    static final private int ALTITUDE_ARRAY_SIZE = 4;
    static final private double MAX_ELEVATION_DELTA = 4.0;
    private float[] altitudeBuffer = new float[ALTITUDE_ARRAY_SIZE];
    private int altitudeBufferIndex = 0;
    private int altitudeBufferValueNumber = 0;
    private float lastAltitude = -1.0f;     // -1.0f means non initialised.
    private float ascent = 0.0f;            // positive elevation gain in m.
    private float descent = 0.0f;           // negative elevation gain in m.

    // App constants
    static final private float requiredAccuracy = 10.0f;        // Ignore precision below this value, in  meters. 
    static final private int gps_update_interval = 7500;         // in milliseconds, 0 means as fast as possibile
    static final private float gps_min_distance = 0.0f;        // in meter, 0 means any distances.
    static final private String fileExtension = ".csv";

    private Handler myHandler = null;
    private Runnable myRunnable = null;
    private Timer myTimer = null;


    /**
     * resets all application data except log file name.
     */
    private void reset()
    {
        started = false;
        inst_speed = 0.0f;        // Speed in m.s-1
        average_speed = 0.0f;     // Average Speed in m.s-1
        distance = 0.0f;          // Integrated distance in m.
        update_count = 0;          // gps update counter.
        elapsed_seconds = 0.0f;
        gpsStatus = getString(R.string.gps_status_no_upd);
        latitude = 0.0;
        longitude = 0.0;
        satelliteNumber = 0;
        pauseStarted = -1;

        lastAltitude = -1.0f;
        ascent = 0.0f;
        descent = 0.0f;
        altitudeBufferIndex = 0;
        altitudeBufferValueNumber = 0;
        for (int i = 0; i < ALTITUDE_ARRAY_SIZE; i++)
        {
            altitudeBuffer[i] = 0.0f;
        }

        logs.safeWrite(getString(R.string.logs_reset));

    }


    /**
     * Called when the activity is first created. This is where you should do
     * all of your normal static set up: create views, bind data to lists, etc.
     * This method also provides you with a Bundle containing the activity's
     * previously frozen state, if there was one. Always followed by onStart().
     *
     * You should always call up to your superclass when implementing these
     * method.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        checkForAvailableGPS();

        gps = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, gps_update_interval, gps_min_distance, this);

        /*
         Nested runnable used to update  Duration TextView.
         Other UI fields are updated in UpdateUI called from onLocationChange.
         */
        myRunnable = new Runnable()
        {
            public void run()
            {

                TextView tv = (TextView) findViewById(R.id.duration_label);
                if (tv != null)
                {
                    long local_elapsed_seconds = 0;

                    if ((started == true) && (last_position != null))
                    {
                        local_elapsed_seconds = (long) elapsed_seconds + ((SystemClock.elapsedRealtimeNanos() - last_position.getElapsedRealtimeNanos()) / 1000000000);
                    }
                    else
                    {
                        local_elapsed_seconds = (long) elapsed_seconds;
                    }
//                    deltaTseconds = (float) ((location.getElapsedRealtimeNanos() - last_position.getElapsedRealtimeNanos()) / 1000000000.0);

                    long hour = local_elapsed_seconds / 3600;
                    long min = (local_elapsed_seconds % 3600) / 60;
                    long sec = local_elapsed_seconds % 60;

                    String result = String.format("%02d:%02d:%02d", hour, min, sec);
                    tv.setText(result);
                }
            }
        };

        myHandler = new Handler();

        if (savedInstanceState != null)
        {
            latitude = savedInstanceState.getDouble("latitude");
            longitude = savedInstanceState.getDouble("longitude");
            satelliteNumber = savedInstanceState.getInt("satelliteNumber");
            last_position = (Location) savedInstanceState.getParcelable("last_position");
            started = savedInstanceState.getBoolean("started");
            inst_speed = savedInstanceState.getFloat("inst_speed");
            average_speed = savedInstanceState.getFloat("average_speed");
            distance = savedInstanceState.getFloat("distance");
            update_count = savedInstanceState.getLong("update_count");
            elapsed_seconds = savedInstanceState.getFloat("elapsed_seconds");
            gpsStatus = savedInstanceState.getString("gpsStatus");
            logFilename = savedInstanceState.getString("logFilename");
            firstGPSFixReceived = savedInstanceState.getBoolean("firstGPSFixReceived");
            altitudeBufferIndex = savedInstanceState.getInt("altitudeBufferIndex");
            altitudeBufferValueNumber = savedInstanceState.getInt("altitudeBufferValueNumber");
            lastAltitude = savedInstanceState.getFloat("lastAltitude");
            altitudeBuffer = savedInstanceState.getFloatArray("altitudeBuffer");
            ascent = savedInstanceState.getFloat("ascent");
            descent = savedInstanceState.getFloat("descent");

            setContentView(R.layout.main);

            updateUI();

            if (firstGPSFixReceived == true)
            {
                Button myButton = (Button) findViewById(R.id.button_start_resume);
                myButton.setEnabled(true);
                if (started == true)
                {
                    myButton.setText(getString(R.string.button_stop));
                }
                else
                {
                    myButton.setText(getString(R.string.button_start_resume));
                }
            }
        }
        else
        {
            SimpleDateFormat sdf = new SimpleDateFormat("E_dd_MMM_yyyy__HH_mm_ss");
            String now = sdf.format(new Date());
            logFilename = getString(R.string.saveDirectory) + "/Run_" + now + fileExtension;
            setContentView(R.layout.main);
        }

        if (firstGPSFixReceived == false)
        {
            Toast.makeText(this, getString(R.string.gps_waiting_fix), Toast.LENGTH_LONG).show();
        }

        // Check for Sub directory.
        boolean saveSubDirExists = true;

        try
        {
            File subDir = new File(Environment.getExternalStorageDirectory().getPath() + "/" + getString(R.string.saveDirectory));
            if (subDir.exists() == false)
            {
                // Sub directory does not exists. Try to create it.
                saveSubDirExists = subDir.mkdir();
            }
        }
        catch (Exception e)
        {
            saveSubDirExists = false;
        }

        logs = new ExternalFileLogger(logFilename);

        logs.safeWrite(getString(R.string.logs_start_session));
        logs.safeWrite(getString(R.string.app_name));
        logs.safeWrite(getString(R.string.logs_gps_update_interval) + gps_update_interval + " ms,  " + getString(R.string.logs_gps_min_distance) + gps_min_distance + " m.");
        logs.safeWrite(getString(R.string.logs_required_accuracy) + requiredAccuracy + " m.");
        logs.safeWrite(getString(R.string.logs_csv_format));

        if ((ExternalFileLogger.isExternalStorageWritable() == false) || (saveSubDirExists == false))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.error_logs_ko));
            builder.setMessage(getString(R.string.error_logs_ko_details));
            builder.setPositiveButton(getString(R.string.button_OK), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    // Inform user but do nothing.

                }
            });

            builder.create().show();
        }
    }


    /**
     * This method is called when user is about to close the application (click
     * on backward button. A confirmation dialog ask user to confirm the
     * operation.
     */
    @Override
    public void onBackPressed()
    {
        //Ask the user to confirm close app.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.msg_close));
        builder.setMessage(getString(R.string.msg_close_confirm));
        builder.setPositiveButton(getString(R.string.button_YES), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // Yes, end Activity
                finish();
            }
        });

        builder.setNegativeButton(getString(R.string.button_CANCEL), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // Just ignore the event.
            }
        });
        builder.create().show();
    }


    /**
     * Called when the activity is becoming visible to the user. Followed by
     * onResume() if the activity comes to the foreground, or onStop() if it
     * becomes hidden.
     *
     * You should always call up to your superclass when implementing these
     * method.
     */
    @Override
    protected void onStart()
    {
        super.onStart();
    }


    /**
     * Called after your activity has been stopped, prior to it being started
     * again. Always followed by onStart()
     *
     * You should always call up to your superclass when implementing these
     * method.
     */
    @Override
    protected void onRestart()
    {
        super.onRestart();
    }


    /**
     * Called when the activity will start interacting with the user. At this
     * point your activity is at the top of the activity stack, with user input
     * going to it. Always followed by onPause().
     *
     * You should always call up to your superclass when implementing these
     * method.
     */
    @Override
    protected void onResume()
    {
        super.onResume();

        if ((started == true) && (pauseStarted != -1))
        {
            // Update elapsed_second with the second we sleept.
            Time now = new Time();
            now.setToNow();
            long nowSeconds = (now.toMillis(false) / 1000);
            long deltaT = nowSeconds - pauseStarted;

            logs.safeWrite(getString(R.string.logs_pause_resume) + nowSeconds + getString(R.string.logs_pause_duration) + deltaT + getString(R.string.logs_pause_duration_unit)+".");

            updateUI();
            pauseStarted = -1; // invalidate pause start time.
        }

        myTimer = new Timer();
        myTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                updateTime();
            }
        }, 0, 1000);
    }


    /**
     * Called when the system is about to start resuming a previous activity.
     * This is typically used to commit unsaved changes to persistent data, stop
     * animations and other things that may be consuming CPU, etc.
     * Implementations of this method must be very quick because the next
     * activity will not be resumed until this method returns. Followed by
     * either onResume() if the activity returns back to the front, or onStop()
     * if it becomes invisible to the user.
     *
     * You should always call up to your superclass when implementing these
     * method.
     */
    @Override
    protected void onPause()
    {
        super.onPause();

        // Stop updating timer.
        if (myTimer != null)
        {
            myTimer.cancel();
            myTimer = null;
        }

        if (started)
        {
            Time now = new Time();
            now.setToNow();
            pauseStarted = now.toMillis(false) / 1000;
            logs.safeWrite(getString(R.string.logs_entering_pause) + pauseStarted);
        }
    }


    /**
     * Called when the activity is no longer visible to the user, because
     * another activity has been resumed and is covering this one. This may
     * happen either because a new activity is being started, an existing one is
     * being brought in front of this one, or this one is being destroyed.
     * Followed by either onRestart() if this activity is coming back to
     * interact with the user, or onDestroy() if this activity is going away.
     *
     * You should always call up to your superclass when implementing these
     * method.
     */
    @Override
    protected void onStop()
    {
        super.onStop();
    }


    /**
     * Save Instance data here. Not that permanent data (disk) must be save into
     * onPause() method.
     *
     * @param savedInstanceState
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);

        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putBoolean("started", started);
        savedInstanceState.putFloat("inst_speed", inst_speed);
        savedInstanceState.putFloat("average_speed", average_speed);
        savedInstanceState.putFloat("distance", distance);
        savedInstanceState.putLong("update_count", update_count);
        savedInstanceState.putFloat("elapsed_seconds", elapsed_seconds);
        savedInstanceState.putString("gpsStatus", gpsStatus);
        savedInstanceState.putParcelable("last_position", last_position);
        savedInstanceState.putDouble("latitude", latitude);
        savedInstanceState.putDouble("longitude", longitude);
        savedInstanceState.putInt("satelliteNumber", satelliteNumber);
        savedInstanceState.putString("logFilename", logFilename);
        savedInstanceState.putBoolean("firstGPSFixReceived", firstGPSFixReceived);

        savedInstanceState.putFloat("lastAltitude", lastAltitude);
        savedInstanceState.putInt("altitudeBufferIndex", altitudeBufferIndex);
        savedInstanceState.putInt("altitudeBufferValueNumber", altitudeBufferValueNumber);
        savedInstanceState.putFloatArray("altitudeBuffer", altitudeBuffer);
        savedInstanceState.putFloat("ascent", ascent);
        savedInstanceState.putFloat("descent", descent);
    }


    /**
     * The final call you receive before your activity is destroyed. This can
     * happen either because the activity is finishing (someone called finish()
     * on it, or because the system is temporarily destroying this instance of
     * the activity to save space. You can distinguish between these two
     * scenarios with the isFinishing() method.
     *
     * You should always call up to your superclass when implementing these
     * method.
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        gps.removeUpdates(this);
        gps = null;
        if (myTimer != null)
        {
            myTimer.cancel();
            myTimer = null;
        }
        myRunnable = null;
        myHandler = null;
    }


    /**
     * Post callback in order to update time in the main thread. The runnable is
     * in charge of updating duration textview field.
     */
    private void updateTime()
    {
        myHandler.post(myRunnable);
    }


    /**
     * This method si called when (re)start / stop button is pressed. Perform
     * toggle between measurement, stop and resume measurements.
     *
     * @param view : Button that called this callback (unused here).
     */
    public void startResume(View view)
    {
        String action;
        Button myButton = (Button) findViewById(R.id.button_start_resume);
        if (started == true)
        {
            myButton.setText(getString(R.string.button_start_resume));
            started = false;
            action = getString(R.string.logs_tracking_stopped);
        }
        else
        {
            myButton.setText(getString(R.string.button_stop));
            started = true;
            action = getString(R.string.logs_tracking_started);
        }
        logs.safeWrite(getString(R.string.logs_tracking_is) + action + ".");
    }


    /**
     * This method si called when reset button is pressed. A confirmation dialog
     * is shown to the user, to confirm the operation.
     *
     * @param view : Button that called this callback (unused here).
     */
    public void reset(View view)
    {

        //Ask the user to enable GPS
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.msg_reset));
        builder.setMessage(getString(R.string.msg_reset_confirm));
        builder.setPositiveButton(getString(R.string.button_YES), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // Yes, reset Activity
                Button myButton = (Button) findViewById(R.id.button_start_resume);
                myButton.setText(R.string.button_start_resume);

                // reset things.
                reset();
            }
        });

        builder.setNegativeButton(getString(R.string.button_CANCEL), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // Just ignore the event.
            }
        });
        builder.create().show();
    }


    /**
     * This method updates User Interface. No calculation are done here, except
     * some minor volatile speed conversions.
     */
    private void updateUI()
    {
        TextView tv = (TextView) findViewById(R.id.distance_label);
        if (tv != null)
        {
            String result = String.format("%3.2f km", distance / 1000.0f);
            tv.setText(result);
        }

        tv = (TextView) findViewById(R.id.instant_speed_label);
        if (tv != null)
        {
            String result = String.format("%3.2f km/h", inst_speed * 3.6f);
            tv.setText(result);
        }

        tv = (TextView) findViewById(R.id.coordinateN);
        if (tv != null)
        {
            String msg = Location.convert(latitude, Location.FORMAT_DEGREES) + " North";
            tv.setText(msg);
        }

        tv = (TextView) findViewById(R.id.coordinateE);
        if (tv != null)
        {
            String msg = Location.convert(longitude, Location.FORMAT_DEGREES) + " East";
            tv.setText(msg);
        }

        tv = (TextView) findViewById(R.id.satellite);
        if (tv != null)
        {
            String satDiag = "";
            if (satelliteNumber <= 2)
            {
                tv.setTextColor(0xFFFF0000);
                satDiag = getString(R.string.gps_no_signal);
            }
            if ((satelliteNumber >= 3) && (satelliteNumber < 5))
            {
                tv.setTextColor(0xFFFFA500);
                satDiag = getString(R.string.gps_low_signal);
            }
            if ((satelliteNumber >= 5) && (satelliteNumber < 7))
            {
                tv.setTextColor(0xFFFFFF00);
                satDiag = getString(R.string.gps_aver_signal);
            }
            if ((satelliteNumber >= 7) && (satelliteNumber < 9))
            {
                tv.setTextColor(0xFF00FF00);
                satDiag = getString(R.string.gps_good_signal);
            }
            if (satelliteNumber >= 9)
            {
                tv.setTextColor(0xFF00FF00);
                satDiag = getString(R.string.gps_excel_signal);
            }

            String msg = String.format("%s (%d sat.)", satDiag, satelliteNumber);
            tv.setText(msg);
        }

        tv = (TextView) findViewById(R.id.elevation_label);
        if (tv != null)
        {
            String msg = String.format("+%3.0fm / %3.0fm", ascent, descent);
            tv.setText(msg);
        }

        tv = (TextView) findViewById(R.id.average_speed_label);
        if (tv != null)
        {
            String msg = String.format("%3.2f km/h av.", average_speed * 3.6f);
            tv.setText(msg);
        }
    }


    /**
     * Callback called when the GPS has updated location. This is the main
     * tracker method.
     *
     * @param location : Location containing GPS informations.
     */
    public void onLocationChanged(Location location)
    {
        if (location != null)
        {
            float accuracy = location.getAccuracy();
            double deltaD = 0.0f;                        // Delta distance between two location updates.
            float deltaTseconds = 0.0f;                 // Delta Time between two location updates, in nano seconds.
            String state = getString(R.string.logs_no_tracking);
            double altitude = 0.0;
            float bearing = 0.0f;

            // check for required precision otherwise just ignore the location.
            if (accuracy <= requiredAccuracy)
            {
                if (firstGPSFixReceived == false)
                {
                    Button myButton = (Button) findViewById(R.id.button_start_resume);
                    myButton.setEnabled(true);
                    firstGPSFixReceived = true;
                }

                update_count++;

                latitude = location.getLatitude();
                longitude = location.getLongitude();
                satelliteNumber = location.getExtras().getInt("satellites");
                inst_speed = location.getSpeed();
                altitude = location.getAltitude();
                bearing = location.getBearing();

                // Integrate distance if we already have one valid position.
                if (last_position != null)
                {
                    if (started == true)
                    {
                        WGS84Point src = new WGS84Point(last_position.getLatitude(), last_position.getLongitude());
                        WGS84Point dst = new WGS84Point(location.getLatitude(), location.getLongitude());
                        deltaD = WGS84.vincentyDistance(src, dst) * 1000.0; // Distance is returned in kilometer !

                        deltaTseconds = (float) ((location.getElapsedRealtimeNanos() - last_position.getElapsedRealtimeNanos()) / 1000000000.0);
                        elapsed_seconds += deltaTseconds;
                        distance += deltaD;
                        average_speed = (distance / (float) elapsed_seconds);
                        computeElevationGain(altitude);
                        state = getString(R.string.logs_tracking_ok);
                    }
                }

                // Always update last position
                last_position = location;
            }
            else
            {
                state = getString(R.string.logs_bad_accuracy);
            }

            updateUI();
            /* 
             CSV format : OLC; Distance; Delta Dist; Accuracy; Inst Speed; deltaTSeconds; altitude; bearing; latitude; longitude; lastAltitude; ascent; descent; SatNumber; UpdateNumber; State;
             */
            String toLog = String.format("OLC; %4.2f; %4.2f; %4.2f; %4.2f; %4.2f; %4.2f; %4.2f; %9.6f; %9.6f; %4.1f; %3.1f; %3.1f; %d; %d; %s;", distance, deltaD, accuracy, inst_speed, deltaTseconds, altitude, bearing, latitude, longitude, lastAltitude, ascent, descent, satelliteNumber, update_count, state);

            logs.safeWrite(toLog);
        }
        else
        {
            logs.safeWrite(getString(R.string.logs_location_is_null));
        }
    }


    /**
     * Compute elevation gain, based on altitude received on location update.
     *
     * @param alt : raw altitude received in Location oject.
     */
    void computeElevationGain(double alt)
    {
        // 4 satellites are the least expected to get 3D GPS fix (with altitude).
        if (satelliteNumber < 4)
        {
            return;
        }

        // Check for infinity / NaN..
        if (Double.isInfinite(alt) || Double.isNaN(alt))
        {
            return;
        }

        // New altitude received, store it in altitude buffer.
        altitudeBuffer[altitudeBufferIndex] = (float) alt;
        altitudeBufferIndex++;
        if (altitudeBufferIndex == ALTITUDE_ARRAY_SIZE)
        {
            altitudeBufferIndex = 0;
        }

        if (altitudeBufferValueNumber < ALTITUDE_ARRAY_SIZE)
        {
            altitudeBufferValueNumber++;
        }

        // If we have enough data, we can compute average altitude and get a value.
        if (altitudeBufferValueNumber == ALTITUDE_ARRAY_SIZE)
        {
            float av_alt = 0.0f;
            for (int i = 0; i < ALTITUDE_ARRAY_SIZE; i++)
            {
                av_alt += altitudeBuffer[i];
            }

            av_alt /= (float) ALTITUDE_ARRAY_SIZE;

            if (lastAltitude == -1.0f)
            {
                // First altitude value.
                lastAltitude = av_alt;
            }
            else
            {
                // We already have an averaged altitude value, we can compute elevation gain.
                float deltaH = av_alt - lastAltitude;

                // Basic check on delta elevation : No more  than a max value.
                // This should be correlated with deltaT for better test.
                if (Math.abs(deltaH) > MAX_ELEVATION_DELTA)
                {
                    return;
                }
                if (deltaH < 0)
                {
                    descent += deltaH;
                }
                else
                {
                    ascent += deltaH;
                }

                lastAltitude = av_alt;
            }
        }
    }


    /**
     * Callback called when the GPS status has changed.
     *
     * @param provider
     */
    public void onStatusChanged(String provider, int status, Bundle extras)
    {

        switch (status)
        {
            case LocationProvider.OUT_OF_SERVICE:
                gpsStatus = provider + getString(R.string.gps_status_oos);
                break;
            case LocationProvider.AVAILABLE:
                gpsStatus = provider + getString(R.string.gps_status_avail);
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                gpsStatus = provider + getString(R.string.gps_status_unavail);
                break;
            default:
                gpsStatus = provider + getString(R.string.gps_status_unknown);
                break;
        }

        logs.safeWrite("OSC:" + gpsStatus);
    }


    /**
     * Callback called when the GPS has been enabled.
     *
     * @param provider
     */
    public void onProviderEnabled(String provider)
    {
        logs.safeWrite(getString(R.string.logs_gps_enabled) + provider);
    }


    /**
     * Callback called when the GPS has been disabled.
     *
     * @param provider
     */
    public void onProviderDisabled(String provider)
    {
        logs.safeWrite(getString(R.string.logs_gps_disabled) + provider);
    }


    /**
     * This method checks for GPS availability. If GPS is disabled, a dialog is
     * shown proposing to open GPS panel to enable it. Cancelling this
     * operation, will end the activity. If the GPS is enabled, nothing happend.
     */
    void checkForAvailableGPS()
    {
        boolean exitApp = false;

        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            //Ask the user to enable GPS
            AlertDialog.Builder builder = new AlertDialog.Builder(this, THEME_HOLO_DARK);
            builder.setTitle(getString(R.string.msg_gps_disabled));
            builder.setMessage(getString(R.string.msg_gps_disabled_confirm));
            builder.setPositiveButton(getString(R.string.button_YES), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //Launch settings, allowing user to make a change
                    Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(i);

                }
            });

            builder.setNegativeButton(getString(R.string.button_CANCEL), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //No location service, end Activity
                    finish();
                }
            });
            builder.create().show();
        }
    }
}
