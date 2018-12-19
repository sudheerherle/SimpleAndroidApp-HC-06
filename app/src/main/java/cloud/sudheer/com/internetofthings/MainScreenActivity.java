package cloud.sudheer.com.internetofthings;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import org.acra.*;
import org.acra.annotation.*;

import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.Parse;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.PushService;
import com.parse.SaveCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Ref;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class MainScreenActivity extends Activity{

    public TextView status;
    public ImageButton speakbtn;
    private TextToSpeech ttobj;
    Switch bedroom_light = null;//(Switch)  findViewById(R.id.switch1);
    Switch bedroom_fan = null;//(Switch)  findViewById(R.id.switch2);
    Button BtnRefresh   = null;//(Button) findViewById(R.id.button);
//    ParseObject Home_Auto = null;
    ProgressDialog dialog = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    public static final int MESSAGE_STATE_CHANGE = 1;
    private static final int RX_DATA_RECIEVED = 0;
    public static final int MESSAGE_TOAST = 5;
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 3;//REQUEST_OK
    private static final int REQUEST_OK = 6;
    private AcceptThread mAcceptThread;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private ConnectThread mConnectThread;
    private SharedPreferences sharedPref;
    public static final String TOAST = "toast";
    private ConnectedThread mConnectedThread;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private int mState = 0;
    String mDeviceName = "Device";
//    ParsePush push =null;
    private SharedData sharedata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedata = SharedData.getSingletonObject();

        sharedata.SetAppStatus(true);
        setContentView(R.layout.activity_main_screen);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        boolean bedroom_notification = sharedPref.getBoolean(getString(R.string.Bedroom), true);

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(SharedPreferences sharedPref, String key) {
//                        updateSubscription();
                    }
                };
        sharedPref.registerOnSharedPreferenceChangeListener(listener);
        bedroom_light = (Switch)  findViewById(R.id.switch1);
        bedroom_fan = (Switch)  findViewById(R.id.switch2);
        status = (TextView) findViewById(R.id.status2);
        speakbtn = (ImageButton) findViewById(R.id.imageButton);

        ttobj=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        }
        );
        openBTActivity();
        new AsyncTaskExample().execute();
        ttobj.setLanguage(Locale.US);
        if(sharedata.isAppRunning()== false) {
            sharedata.SetAppStatus(true);
            connectDevice("20:14:05:19:22:17");
        }

        bedroom_light.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(mConnectedThread!=null) {
                    String data = "a";
                    if(isChecked == false){
                        data ="b";
                    }
                    mConnectedThread.write(data.getBytes());
                }
                UpdateLight(isChecked);
            }
        });

        bedroom_fan.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(mConnectedThread!=null) {
                    String data = "c";
                    if(isChecked==false){
                        data = "d";
                    }
                    mConnectedThread.write(data.getBytes());
                }
//                UpdateFan(isChecked);
            }
        });

        speakbtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
                try {
                    startActivityForResult(i, REQUEST_OK);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Error initializing speech to text engine.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //MNEU
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
//        NetworkUtilTask netTask = new NetworkUtilTask(this);
//        try {
//            try {
//                if(netTask.execute().get(2000, TimeUnit.SECONDS) == Boolean.TRUE) {
////                    Initialise_Parse();
//                    RefreshStatus();
//                }
//                else{
//                    Toast.makeText(this, "Internet connectivity not available.", Toast.LENGTH_SHORT).show();
//                }
//            } catch (TimeoutException e) {
//                e.printStackTrace();
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
    }

    private void UpdateLight(boolean isChecked){
        if(mConnectedThread!=null) {
            String data = "a";
            if(isChecked == false){
                data ="b";
            }
            mConnectedThread.write(data.getBytes());
        }
        if(isNetworkAvailable()==false){
            Toast.makeText(this, "Internet connectivity not available.", Toast.LENGTH_SHORT).show();
            return;
        }
//        if(Home_Auto==null) return;
//        Home_Auto.put("BedRoomLight", isChecked);
//        Home_Auto.saveInBackground();

//        push.setChannel("Bedroom");
//        if(isChecked)push.setMessage("The bedroom Light is turned on");
//        else push.setMessage("The bedroom Light is turned off");
//        push.sendInBackground();
    }

//    private void UpdateFan(boolean isChecked){
//        if(mConnectedThread!=null) {
//            String data = "c";
//            if(isChecked==false){
//                data = "d";
//            }
//            mConnectedThread.write(data.getBytes());
//        }
//        if(isNetworkAvailable()==false){
//            Toast.makeText(this, "Internet connectivity not available.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if(Home_Auto==null) return;
//        Home_Auto.put("BedRoomFan", isChecked);
//        Home_Auto.saveInBackground();

//        push.setChannel("Bedroom");
//        if(isChecked)push.setMessage("The Bedroom fan is turned on");
//        else push.setMessage("The Bedroom fan is turned off");
//        push.sendInBackground();
//    }
//    private void RefreshStatus(){
//        if(isNetworkAvailable()==false){
//            Toast.makeText(this, "Internet connectivity not available.", Toast.LENGTH_SHORT).show();
//            return;
//        }
////        if(Home_Auto==null) return;
////        try {
////            Home_Auto = ParseQuery.getQuery("TestObject").get("dksmXfZB2Z");
////        } catch (ParseException e) {
////            e.printStackTrace();
////        }
////        boolean o = (Boolean)Home_Auto.get("BedRoomFan");
//        boolean o = false;
//        bedroom_fan.setChecked(o);
//        if(mConnectedThread!=null) {
//            String data = "c";
//            if(o==false){
//                data = "d";
//            }
//            mConnectedThread.write(data.getBytes());
//        }
//        o = false;//(Boolean)Home_Auto.get("BedRoomLight");
//        bedroom_light.setChecked(o);
//        if(mConnectedThread!=null) {
//            String data = "a";
//            if(o == false){
//                data ="b";
//            }
//            mConnectedThread.write(data.getBytes());
//        }
//    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {

            case R.id.secure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, BTList.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
//            case R.id.Refresh:
//                RefreshStatus();
//                return true;
//            case R.id.Settings:
//                Intent intent = new Intent(this, SettingsActivity.class);
//                startActivityForResult(intent,9);
        }
        return false;
    }

    private void ensureDiscoverable() { //make BT Discoverable
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    @ReportsCrashes(formUri = "")
    public class MyApplication extends Application {
        @Override
        public void onCreate() {
            // The following line triggers the initialization of ACRA
            super.onCreate();
            ACRA.init(this);
            ACRA.getErrorReporter().setReportSender(new LocalReportSender(this));
        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When BTListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupInterface();
                }
//                else {
//                    // User did not enable Bluetooth or an error occured
//                    Toast.makeText(this, "Bluetooth not enabled, accessing through cloud...", Toast.LENGTH_SHORT).show();
////                    NetworkUtilTask netTask = new NetworkUtilTask(this);
//                    try {
//                        if(netTask.execute().get() == Boolean.TRUE) {
////                            RefreshStatus();
//                        }
//                        else{
//                            Toast.makeText(this, "Neither bluetooth not internet is available, leaving...", Toast.LENGTH_SHORT).show();
//                            finish();
//                        }
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    } catch (ExecutionException e) {
//                        e.printStackTrace();
//                    }
//                }
            case REQUEST_OK:
                    if(data==null) return;
                    ArrayList<String> thingsYouSaid = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if(thingsYouSaid.size()==0){
                        return;
                    }
                    if((thingsYouSaid.get(0).equalsIgnoreCase("Turn on Light")))
                    {
                        bedroom_light.setChecked(true);
                        UpdateLight(true);
                        ttobj.speak("Bed room light is now turned on.", TextToSpeech.QUEUE_FLUSH, null);
                    }
                    if((thingsYouSaid.get(0).equalsIgnoreCase("Turn off Light")))
                    {
                        bedroom_light.setChecked(false);
                        UpdateLight(false);
                        ttobj.speak("Okay. Bed room light is tuned off.", TextToSpeech.QUEUE_FLUSH, null);
                    }
                    if((thingsYouSaid.get(0).equalsIgnoreCase("Turn on Fan")))
                    {
                        bedroom_fan.setChecked(true);
//                        UpdateFan(true);
                        ttobj.speak("Okay, Bedroom fan is turned on.", TextToSpeech.QUEUE_FLUSH, null);
                    }
                    if((thingsYouSaid.get(0).equalsIgnoreCase("Turn off Fan")))
                    {
                        bedroom_fan.setChecked(false);
//                        UpdateFan(false);
                        ttobj.speak("Affirmative! Bedroom fan is turned off.", TextToSpeech.QUEUE_FLUSH, null);
                    }
        }
    }

    private void setupInterface() { //Setting up interface
        status = (TextView) findViewById(R.id.status2);
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
        setState(STATE_LISTEN);
    }


//    private void Initialise_Parse(){
//        if(isNetworkAvailable()==false){
////            Toast.makeText(this, "Ineternet connectivity not available.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        try {
//            Parse.initialize(MainScreenActivity.this, "lPNb2mvKPqnhtNh8UnuDggqS3MUSqpdkQBCTzoGc", "OqERB79X82Yc6UcCLIOuUpvJE8bsLw4nLVWbPSdz");
//            PushService.setDefaultPushCallback(this, MainScreenActivity.class);
//            Home_Auto = ParseQuery.getQuery("TestObject").get("dksmXfZB2Z");
//            ParseInstallation installation = ParseInstallation.getCurrentInstallation();
//            installation.saveInBackground(new SaveCallback() {
//                @Override
//                public void done(ParseException e) {
//
//                }
//            });
//            push = new ParsePush();
//            push.setPushToAndroid(true);
//            updateSubscription();
//
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//    }


//    public boolean hasInternetAccess(Context context) {
//        if (isNetworkAvailable()) {
//            try {
//                HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
//                urlc.setRequestProperty("User-Agent", "Test");
//                urlc.setRequestProperty("Connection", "close");
//                urlc.setConnectTimeout(1500);
//                urlc.connect();
//                return (urlc.getResponseCode() == 200);
//            } catch (IOException e) {
//                Log.e("com.parse.push", "Error checking internet connection", e);
//            }
//        } else {
//            Log.d("com.parse.push", "No network available!");
//        }
//        return false;
//    }

//    private class NetworkUtilTask extends AsyncTask<Void, Void, Boolean>{
//        Context context;
//
//        public NetworkUtilTask(Context context){
//            this.context = context;
//        }
//
//        protected Boolean doInBackground(Void... params) {
//            return hasInternetAccess(this.context);
//        }
//        protected void onPostExecute(Boolean hasActiveConnection) {
//            Log.d("com.parse.push","Success=" + hasActiveConnection);
//        }
//    }

//    public void updateSubscription(){
//        List<String> subscribedChannels = ParseInstallation.getCurrentInstallation().getList("channels");
//        if(subscribedChannels!=null){
//        for(int p=0;p<subscribedChannels.size();p++){
//            ParsePush.unsubscribeInBackground(subscribedChannels.get(p), new SaveCallback() {
//                @Override
//                public void done(ParseException e) {
//                    if (e != null) {
//                        Log.d("com.parse.push", "successfully subscribed to the broadcast channel.");
//                    } else {
//                        Log.e("com.parse.push", "failed to subscribe for push", e);
//                    }
//                }
//            });
//        }}
//        if(sharedPref.getBoolean(getString(R.string.Bedroom), true)) {
//            ParsePush.subscribeInBackground("Bedroom", new SaveCallback() {
//                @Override
//                public void done(ParseException e) {
//                    if (e != null) {
//                        Log.d("com.parse.push", "successfully subscribed to the broadcast channel.");
//                    } else {
//                        Log.e("com.parse.push", "failed to subscribe for push", e);
//                    }
//                }
//            });
//
//        }else{
//            ParsePush.unsubscribeInBackground("Bedroom", new SaveCallback() {
//                @Override
//                public void done(ParseException e) {
//                    if (e != null) {
//                        Log.d("com.parse.push", "successfully subscribed to the broadcast channel.");
//                    } else {
//                        Log.e("com.parse.push", "failed to subscribe for push", e);
//                    }
//                }
//            });
//
//        }
//    }
    class AsyncTaskExample extends AsyncTask<Void, String, String> {

        protected void onPreExecute(){
            dialog = new ProgressDialog(MainScreenActivity.this);
            dialog.setMessage("Please wait. Connecting to server...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }
        protected String doInBackground(Void...arg0) {
            return "You are at PostExecute";
        }
        protected void onProgressUpdate(String...a){
//            Initialise_Parse();
        }
        protected void onPostExecute(String result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    RefreshStatus();
                    dialog.dismiss();
                }
            });

        }
    }
    protected void openBTActivity() {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {//BT is enabled, so setup the interface
            setupInterface();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    private synchronized void connectDevice(Intent data) {
        // Get the device MAC address //20:14:05:19:22:17
        String address = data.getExtras().getString(BTList.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mDeviceName = device.getName();

        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    private synchronized void connectDevice(String addrs) {
        // Get the device MAC address //20:14:05:19:22:17

        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(addrs);
        mDeviceName = device.getName();

        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connect(BluetoothDevice device) {
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    private String processForCommands() {
        return "";

    }
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE: //Updating status
                    switch (msg.arg1) {
                        case STATE_CONNECTED:
                            status.setText("Connected to- ");
                            status.append(mDeviceName);
                            FetchStatus();
                            break;
                        case STATE_CONNECTING:
                            status.setText("Connecting...");
                            break;
                        case STATE_LISTEN:
                            status.setText("Listening...");
                            break;
                        case STATE_NONE:
                            status.setText("Not Connected.");
                            break;
                    }
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }

    };

    private synchronized void setState(int state) {
        mState = state;
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        // Give the new state to the Handler so the UI Activity can update
    }

    protected void FetchStatus() {

    }
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        mDeviceName = device.getName();
        setState(STATE_CONNECTED);
    }

    public void connectionFailed() {
        Message msg = mHandler.obtainMessage(MainScreenActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainScreenActivity.TOAST, "Device connection failed.");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Go back to Listening
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
        setState(STATE_LISTEN);
    }

    public void connectionLost() {
        Message msg = mHandler.obtainMessage(MainScreenActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainScreenActivity.TOAST, "Device connection was lost.");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Go back to Listening
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
        setState(STATE_LISTEN);
    }


    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            // Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("MainScreenActivity", MY_UUID);
            } catch (IOException e) {

            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a successful connection or an exception
                    if(mmServerSocket!=null)
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                if (socket != null) {
                    synchronized (this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {

                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {

            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        sharedPref.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onPause(){
        super.onPause();
        sharedPref.unregisterOnSharedPreferenceChangeListener(listener);
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {

            }
            mmSocket = tmp;
        }

        public void run() {
            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {

                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {

            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            byte[] buffer = new byte[1024]; //default 1024
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // processByte(buffer, bytes);
                    String h = new String(buffer);
                    System.out.print(h);
                    byte[] actualBuffer = new byte[bytes];
                    System.arraycopy(buffer, 0, actualBuffer, 0, bytes);
                    if(actualBuffer.length>0){

                        // TODO: Data is recieved on to "actualBuffer"

                        Message msg = Message.obtain();
                        msg.what = RX_DATA_RECIEVED;
                        msg.obj = h;
                        myHandler.sendMessage(msg);//(h);

                    }

                } catch (IOException e) {
                    connectionLost();
                    break;
                }
            }
        }


        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {

            }
        }

        public void cancel() {
            try {
                mmInStream.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }

            try {
                mmOutStream.flush();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }
    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case RX_DATA_RECIEVED:
                    ProcessRecievedData();
                    break;
                default:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
            }
        }

    };

    private void ProcessRecievedData() {

    }
}
