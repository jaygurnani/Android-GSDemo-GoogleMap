package com.dji.GSDemo.GoogleMap;
import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import dji.sdk.AirLink.DJILBAirLink;
import dji.sdk.Battery.DJIBattery;
import dji.sdk.FlightController.DJICompass;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.FlightController.DJIFlightControllerDelegate;
import dji.sdk.MissionManager.DJIMission;
import dji.sdk.MissionManager.DJIMissionManager;
import dji.sdk.MissionManager.DJIWaypoint;
import dji.sdk.MissionManager.DJIWaypointMission;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.RemoteController.DJIRemoteController;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;

public class MainActivity extends FragmentActivity implements View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback, DJIMissionManager.MissionProgressStatusCallback, DJIBaseComponent.DJICompletionCallback {

    protected static final String TAG = "GSDemoActivity";

    private GoogleMap gMap;

    private Button locate, add, clear;
    private Button config, prepare, start, stop;
    private Button startTimer, stopTimer, exportData;
    private Button enableVirtual, disableVirtual, turnDegrees, goStraight, cancelTimer;
    public Timer timerFunc;
    public Timer GlobalTimer;

    private boolean isAdd = false;

    private double droneLocationLat = 181, droneLocationLng = 181, remoteControlLat = 181, remoteControlLong = 181;
    private float droneLocationAlt = 0;
    private float droneVelocityX, droneVelocityY, droneVelocityZ;
    private int wifi1, wifi2, wifi3, wifi4, wifi5, wifi6, wifi7, wifi8;
    private double droneHeading;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;

    private DJIWaypointMission mWaypointMission;
    private DJIMissionManager mMissionManager;
    private DJIFlightController mFlightController;
    private DJIRemoteController mRemoteController;
    private DJICompass mCompass;

    private DJIWaypointMission.DJIWaypointMissionFinishedAction mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.NoAction;
    private DJIWaypointMission.DJIWaypointMissionHeadingMode mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.Auto;

    //DbHelper
    private DbHelper mHelper;
    private SQLiteDatabase mDb;

    //Edit Texts
    private EditText editText, loopCount, speed, degreeToTurn, height;

    //Text Views
    private TextView ConnectStatusTextView;

    //Contexts
    private Context context;

    //Battery Percentages
    public int batteryPercent, batteryVoltage, batteryCurrent;

    @Override
    protected void onResume(){
        super.onResume();
        initFlightController();
        initMissionManager();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    /**
     * @Description : RETURN Button RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUI() {

        locate = (Button) findViewById(R.id.locate);
        add = (Button) findViewById(R.id.add);
        clear = (Button) findViewById(R.id.clear);
        config = (Button) findViewById(R.id.config);
        prepare = (Button) findViewById(R.id.prepare);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        startTimer = (Button) findViewById(R.id.startTimer);
        stopTimer = (Button) findViewById(R.id.stopTimer);
        exportData = (Button) findViewById(R.id.exportData);
        enableVirtual = (Button) findViewById(R.id.enableVirtual);
        turnDegrees = (Button) findViewById(R.id.turnDegrees);
        goStraight  = (Button) findViewById(R.id.goStraight);
        disableVirtual = (Button) findViewById(R.id.disableVirtual);
        cancelTimer = (Button) findViewById(R.id.cancelTimer);

        //Other content
        editText = (EditText) findViewById(R.id.editText);
        loopCount = (EditText) findViewById(R.id.loopCount);
        degreeToTurn = (EditText) findViewById(R.id.degreeToTurn);
        speed = (EditText) findViewById(R.id.speed);
        height = (EditText) findViewById(R.id.height);
        ConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);

        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        config.setOnClickListener(this);
        prepare.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        startTimer.setOnClickListener(this);
        stopTimer.setOnClickListener(this);
        exportData.setOnClickListener(this);
        enableVirtual.setOnClickListener(this);
        turnDegrees.setOnClickListener(this);
        disableVirtual.setOnClickListener(this);
        goStraight.setOnClickListener(this);
        cancelTimer.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GlobalTimer = new Timer();
        //DBHelper
        mHelper = new DbHelper(this);
        mDb = mHelper.getWritableDatabase();

        //Editable Data
        editText = (EditText) findViewById(R.id.editText);
        loopCount = (EditText) findViewById(R.id.loopCount);
        speed = (EditText) findViewById(R.id.speed);
        degreeToTurn = (EditText) findViewById(R.id.degreeToTurn);
        height = (EditText) findViewById(R.id.height);

        //TextViews
        ConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);

        //Context
        context = this;

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        initUI();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    //Start Sampler
    public void StartSampler(int timeInterval){
        final int timeIntervalFinal = timeInterval;

        final Handler handler = new Handler();
        timerFunc = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @SuppressWarnings("unchecked")
                    public void run() {
                        try {
                            //Start Logging
                            LogToDB(batteryPercent, batteryVoltage, batteryCurrent, droneLocationLng, droneLocationLat, droneLocationAlt, remoteControlLong, remoteControlLat, droneVelocityX, droneVelocityY, droneVelocityZ, droneHeading, editText.getText().toString(), wifi1, wifi2, wifi3, wifi4, wifi5, wifi6, wifi7, wifi8);
                        }
                        catch (Exception e) {
                            setResultToToast(e.getMessage().toString());
                        }
                    }
                });
            }
        };
        timerFunc.schedule(doAsynchronousTask, 0, timeIntervalFinal);
    }

    //Log to Database
    public void LogToDB(int batteryPercentage, int batteryVoltage, int batteryCurrent, double lon, double lat, double alt, double homeLong, double homeLat, float droneVelocityX, float droneVelocityY, float droneVelocityZ, double droneHeading, String method, int wifi1, int wifi2, int wifi3, int wifi4, int wifi5, int wifi6, int wifi7, int wifi8){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        ContentValues cv = new ContentValues(6);
        cv.put(mHelper.COL_BATTERY, batteryPercentage);
        cv.put(mHelper.COL_VOLT, batteryVoltage);
        cv.put(mHelper.COL_CURR, batteryCurrent);
        cv.put(mHelper.COL_LONG, lon);
        cv.put(mHelper.COL_LAT, lat);
        cv.put(mHelper.COL_ALT, alt);
        //cv.put(mHelper.COL_HOME_LAT, homeLat);
        //cv.put(mHelper.COL_HOME_LONG, homeLong);
        cv.put(mHelper.COL_WIFI1, wifi1);
        cv.put(mHelper.COL_WIFI2, wifi2);
        cv.put(mHelper.COL_WIFI3, wifi3);
        cv.put(mHelper.COL_WIFI4, wifi4);
        cv.put(mHelper.COL_WIFI5, wifi5);
        cv.put(mHelper.COL_WIFI6, wifi6);
        cv.put(mHelper.COL_WIFI7, wifi7);
        cv.put(mHelper.COL_WIFI8, wifi8);
        cv.put(mHelper.COL_VEL_X, droneVelocityX);
        cv.put(mHelper.COL_VEL_Y, droneVelocityY);
        cv.put(mHelper.COL_VEL_Z, droneVelocityZ);
        cv.put(mHelper.COL_HEADING, droneHeading);
        cv.put(mHelper.COL_METHOD, method);
        cv.put(mHelper.COL_DATE, dateFormat.format(new Date()));

        mDb.insert(mHelper.TABLE_NAME, null, cv);
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initMissionManager();
        initFlightController();
    }

    private void initMissionManager() {
        DJIBaseProduct product = DJIDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            setResultToToast("Disconnected");
            mMissionManager = null;
            return;
        } else {

            setResultToToast("Product connected");
            mMissionManager = product.getMissionManager();
            mMissionManager.setMissionProgressStatusCallback(this);
            mMissionManager.setMissionExecutionFinishedCallback(this);
        }

        mWaypointMission = new DJIWaypointMission();
    }

    private void initFlightController() {

        DJIBaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof DJIAircraft) {
                mFlightController = ((DJIAircraft) product).getFlightController();
                //mRemoteController = ((DJIAircraft) product).getRemoteController();
            }
        }

        if (mFlightController != null) {
            mCompass = mFlightController.getCompass();
            mFlightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                @Override
                public void onResult(DJIFlightControllerDataType.DJIFlightControllerCurrentState state) {
                    droneLocationLat = state.getAircraftLocation().getLatitude();
                    droneLocationLng = state.getAircraftLocation().getLongitude();
                    droneLocationAlt = state.getAircraftLocation().getAltitude();
                    droneVelocityX = state.getVelocityX();
                    droneVelocityY = state.getVelocityY();
                    droneVelocityZ = state.getVelocityZ();
                    droneHeading = mCompass.getHeading();
                    updateDroneLocation();
                }
            });
        }

//        if (mRemoteController != null){
//                mRemoteController.setGpsDataUpdateCallback(new DJIRemoteController.RCGpsDataUpdateCallback() {
//                    @Override
//                    public void onGpsDataUpdate(DJIRemoteController rc, DJIRemoteController.DJIRCGPSData gpsData){
//                        remoteControlLat = gpsData.latitude;
//                        remoteControlLong = gpsData.longitude;
//                    }
//                }
//            );
//        }
    }

    /**
     * DJIMissionManager Delegate Methods
     */
    @Override
    public void missionProgressStatus(DJIMission.DJIMissionProgressStatus progressStatus) {

    }

    /**
     * DJIMissionManager Delegate Methods
     */
    @Override
    public void onResult(DJIError error) {
        setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
    }

    private void setUpMap() {
        gMap.setOnMapClickListener(this);// add the listener for click for amap object

    }

    @Override
    public void onMapClick(LatLng point) {
        if (isAdd == true){
            markWaypoint(point);
            DJIWaypoint mWaypoint = new DJIWaypoint(point.latitude, point.longitude, altitude);
            //Add Waypoints to Waypoint arraylist;
            if (mWaypointMission != null) {
                mWaypointMission.addWaypoint(mWaypoint);
            }
        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    // Update the drone location based on states from MCU.
    private void updateDroneLocation(){

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = gMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void markWaypoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.locate:{
                updateDroneLocation();
                cameraUpdate(); // Locate the drone's place
                break;
            }
            case R.id.add:{
                enableDisableAdd();
                break;
            }
            case R.id.clear:{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gMap.clear();
                    }

                });
                if (mWaypointMission != null){
                    mWaypointMission.removeAllWaypoints(); // Remove all the waypoints added to the task
                }
                break;
            }
            case R.id.config:{
                showSettingDialog();
                break;
            }
            case R.id.prepare:{
                if(mWaypointMission != null){
                    mWaypointMission.repeatNum = Integer.parseInt(loopCount.getText().toString());
                }
                prepareWayPointMission();
                break;
            }
            case R.id.start:{
                startWaypointMission();
                break;
            }
            case R.id.stop:{
                stopWaypointMission();
                break;
            }

            case R.id.startTimer:{
                startTimerToast();
                break;
            }

            case R.id.stopTimer:{
                stopTimerToast();
                break;
            }

            case R.id.exportData:{
                exportDataToast();
                break;
            }

            case R.id.enableVirtual:{
                enableVirtual();
                break;
            }

            case R.id.turnDegrees:{
                turnDegrees();
                break;
            }

            case R.id.goStraight:{
                goStraight();
                break;
            }

            case R.id.disableVirtual:{
                disableVirtual();
                break;
            }

            case R.id.cancelTimer:{
                cancelTimer();
                break;
            }

            default:
                break;
        }
    }

    private void cancelTimer(){
        try {
            GlobalTimer.cancel();
            GlobalTimer.purge();
            setResultToToast("Timer canceled");
            GlobalTimer = new Timer();
        } catch (Exception ex) {
            setResultToToast(ex.getMessage().toString());
        }
    }

    private void enableVirtual(){
        GlobalTimer = new Timer();
        DJIBaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof DJIAircraft) {
                mFlightController = ((DJIAircraft) product).getFlightController();
                mFlightController.setHorizontalCoordinateSystem(DJIFlightControllerDataType.DJIVirtualStickFlightCoordinateSystem.Body);
                mFlightController.setRollPitchControlMode(DJIFlightControllerDataType.DJIVirtualStickRollPitchControlMode.Velocity);
                mFlightController.setVerticalControlMode(DJIFlightControllerDataType.DJIVirtualStickVerticalControlMode.Position);
                mFlightController.setYawControlMode(DJIFlightControllerDataType.DJIVirtualStickYawControlMode.AngularVelocity);
                mFlightController.enableVirtualStickControlMode(new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            setResultToToast("Enable VirtualStickControlMode successful");
                        } else {
                            setResultToToast(djiError.getDescription());
                        }
                    }
                });
            }
        }
    }

    private void disableVirtual(){
        DJIBaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof DJIAircraft) {
                mFlightController = ((DJIAircraft) product).getFlightController();
                mFlightController.disableVirtualStickControlMode(new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            setResultToToast("Disable VirtualStickControlMode successful");
                        } else {
                            setResultToToast(djiError.getDescription());
                        }
                    }
                });
            }
        }
    }

    private void goStraight() {
        DJIBaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof DJIAircraft) {
                mFlightController = ((DJIAircraft) product).getFlightController();
                final DJIFlightControllerDataType.DJIVirtualStickFlightControlData flightControlData =
                        new DJIFlightControllerDataType.DJIVirtualStickFlightControlData(
                                Float.parseFloat(speed.getText().toString()),
                                0,
                                0,
                                Float.parseFloat(height.getText().toString())
                        );

                try {
                    changeVirtualFlight(mFlightController, flightControlData);
                    setResultToToast("Straight: success");

                } catch (Exception ex) {
                    setResultToToast(ex.getMessage().toString());
                }
            }
        }
    }

    private void turnDegrees(){
        DJIBaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof DJIAircraft) {
                mFlightController = ((DJIAircraft) product).getFlightController();
                final DJIFlightControllerDataType.DJIVirtualStickFlightControlData flightControlData =
                        new DJIFlightControllerDataType.DJIVirtualStickFlightControlData(
                                Float.parseFloat(speed.getText().toString()),
                                0,
                                Float.parseFloat(degreeToTurn.getText().toString()),
                                Float.parseFloat(height.getText().toString())
                        );

                try {
                    changeVirtualFlight(mFlightController, flightControlData);
                    setResultToToast("Turn: success");

                } catch (Exception ex) {
                    setResultToToast(ex.getMessage().toString());
                }
            }
        }
    }

    private void changeVirtualFlight(final DJIFlightController mFlightController, final DJIFlightControllerDataType.DJIVirtualStickFlightControlData flightControlData){
        GlobalTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mFlightController.sendVirtualStickFlightControlData(flightControlData, new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error != null) {
                            setResultToToast(error.getDescription());
                        }
                    }
                });
            }
        }, 0, 200);
    }

    private void startTimerToast(){
        //Setup the Battery Call back method
        try {
            DJIDemoApplication.getProductInstance().getBattery().setBatteryStateUpdateCallback(
                    new DJIBattery.DJIBatteryStateUpdateCallback() {
                        @Override
                        public void onResult(DJIBattery.DJIBatteryState djiBatteryState) {
                            batteryPercent = djiBatteryState.getBatteryEnergyRemainingPercent();
                            batteryVoltage = djiBatteryState.getCurrentVoltage();
                            batteryCurrent = djiBatteryState.getCurrentCurrent();
                            ConnectStatusTextView.setText("GSDemo - Battery: " + batteryPercent);
                        }
                    }
            );
        } catch (Exception exception) {
            setResultToToast(exception.getMessage().toString());
        }

        try {
            DJIDemoApplication.getProductInstance().getAirLink().getLBAirLink().setDJILBAirLinkUpdatedAllChannelSignalStrengthsCallback(
                    new DJILBAirLink.DJILBAirLinkUpdatedAllChannelSignalStrengthsCallback() {
                        @Override
                        public void onResult(int[] rssi) {
                            wifi1 = rssi[0];
                            wifi2 = rssi[1];
                            wifi3 = rssi[2];
                            wifi4 = rssi[3];
                            wifi5 = rssi[4];
                            wifi6 = rssi[5];
                            wifi7 = rssi[6];
                            wifi8 = rssi[7];
                        }
                    }
            );
        } catch (Exception exception){
            setResultToToast(exception.getMessage().toString());
        }

        //Start the sampler - Hard code this to be once a second
        StartSampler(100);
        setResultToToast("Sampler Started");
    }

    private void stopTimerToast(){
        timerFunc.cancel();
        setResultToToast("Sampler Stopped");
    }

    private void exportDataToast(){
        mHelper.exportDB(context);
        setResultToToast("Data Exported");
    }

    private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        gMap.moveCamera(cu);

    }

    private void enableDisableAdd(){
        if (isAdd == false) {
            isAdd = true;
            add.setText("Exit");
        }else{
            isAdd = false;
            add.setText("Add");
        }
    }

    private void showSettingDialog(){
        LinearLayout wayPointSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);

        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude);
        TextView speed_RG = (TextView) wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);

        speed_RG.addTextChangedListener(new TextWatcher(){

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    mSpeed = Float.parseFloat(s.toString());
                } catch (NumberFormatException ex){
                    setResultToToast(ex.getMessage());
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after){

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count){

            }
        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone){
                    mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.NoAction;
                } else if (checkedId == R.id.finishGoHome){
                    mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.GoHome;
                } else if (checkedId == R.id.finishAutoLanding){
                    mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.AutoLand;
                } else if (checkedId == R.id.finishToFirst){
                    mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.GoFirstWaypoint;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select heading");

                if (checkedId == R.id.headingNext) {
                    mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.Auto;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.UsingInitialDirection;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.ControlByRemoteController;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.UsingWaypointHeading;
                }
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {

                        String altitudeString = wpAltitude_TV.getText().toString();
                        altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));
                        Log.e(TAG,"altitude "+altitude);
                        Log.e(TAG,"speed "+mSpeed);
                        Log.e(TAG, "mFinishedAction "+mFinishedAction);
                        Log.e(TAG, "mHeadingMode "+mHeadingMode);
                        configWayPointMission();
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                })
                .create()
                .show();
    }

    String nulltoIntegerDefalt(String value){
        if(!isIntValue(value)) value="0";
        return value;
    }

    boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }

    private void configWayPointMission(){

        if (mWaypointMission != null){
            mWaypointMission.finishedAction = mFinishedAction;
            mWaypointMission.headingMode = mHeadingMode;
            mWaypointMission.autoFlightSpeed = mSpeed;

            if (mWaypointMission.waypointsList.size() > 0){
                for (int i=0; i< mWaypointMission.waypointsList.size(); i++){
                    mWaypointMission.getWaypointAtIndex(i).altitude = altitude;
                }

                setResultToToast("Set Waypoint attitude successfully");

            }
       }
    }

    private void prepareWayPointMission(){

        if (mMissionManager != null && mWaypointMission != null) {

            DJIMission.DJIMissionProgressHandler progressHandler = new DJIMission.DJIMissionProgressHandler() {
                @Override
                public void onProgress(DJIMission.DJIProgressType type, float progress) {
                }
            };

            mMissionManager.prepareMission(mWaypointMission, progressHandler, new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    setResultToToast(error == null ? "Mission Prepare Successfully" : error.getDescription());
                }
            });
        }

    }

    private void startWaypointMission(){

        if (mMissionManager != null) {

            mMissionManager.startMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
                }
            });

        }
    }

    private void stopWaypointMission(){

        if (mMissionManager != null) {
            mMissionManager.stopMissionExecution(new DJIBaseComponent.DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
                }
            });

            if (mWaypointMission != null){
                mWaypointMission.removeAllWaypoints();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap == null) {
            gMap = googleMap;
            setUpMap();
        }

        LatLng shenzhen = new LatLng(22.5362, 113.9454);
        gMap.addMarker(new MarkerOptions().position(shenzhen).title("Marker in Shenzhen"));
        gMap.moveCamera(CameraUpdateFactory.newLatLng(shenzhen));
    }

}