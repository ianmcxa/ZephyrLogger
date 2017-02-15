/*   
 * Copyright 2013 (C) Christian Orthmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (C) 2010 Pye Brook Company, Inc.
 *               http://www.pyebrook.com
 *               info@pyebrook.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * This software uses information from the document
 *
 *     'Bluetooth HXM API Guide 2010-07-22'
 *
 * which is Copyright (C) Zephyr Technology, and used with the permission
 * of the company. Information on Zephyr Technology products and how to 
 * obtain the Bluetooth HXM API Guide can be found on the Zephyr
 * Technology Corporation website at
 * 
 *      http://www.zephyr-technology.com
 * 
 *
 */

package org.mcxa.zephyrlogger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.mcxa.zephyrlogger.hxm.HrmReading;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

	private Context mContext;
	private boolean isRecording=false;
	private String recordingTag;

	/*
	 *  TAG for Debugging Log
	 */
	private static final String TAG = "ZephyrLogger";

	/*
	 *  Layout Views
	 */
	@BindView(R.id.status) TextView mStatus;
	@BindView(R.id.toolbar) Toolbar toolbar;

	/*
	 * Name of the connected device, and it's address
	 */
	private String mHxMName = null;
	private String mHxMAddress = null;

	/*
	 * Local Bluetooth adapter
	 */
	private BluetoothAdapter mBluetoothAdapter = null;

	/*
	 * Member object for the chat services
	 */
	private HxmService mHxmService = null;

	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}

	public File exportData(HrmReading m) {
		// Create a ZephyrLogs folder in external storage if it doesn't already exist
		File zephyrlogFolder = new File(Environment.getExternalStorageDirectory(), "ZephyrLogs");

        boolean dirExists = zephyrlogFolder.exists();
        //if the directory doesn't exist, create it
        if (!dirExists) {
            dirExists = zephyrlogFolder.mkdirs();
            //if it still doesn't exist, give up and exit
            if (!dirExists) {
                Log.e(TAG, "Could not create ZephyrLogs directory.");
                System.exit(1);
            }
        }

        //create a data file and write into it
		File file = new File(zephyrlogFolder, "Zephyr_"+recordingTag+"_data.txt");
		try {
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter writer = new FileWriter(file, true);
			writer.write(System.currentTimeMillis()+","+m.toString()+"\n");
			writer.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Could not create logging file");
            e.printStackTrace();
			System.exit(1);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            System.exit(1);
		} catch (IOException e) {
            Log.e(TAG, "IO Exception trying to write to data file");
            e.printStackTrace();
            System.exit(1);
		}
		return file;
	}

	/*
	 * connectToHxm() sets up our service loops and starts the connection
	 * logic to manage the HxM device data stream 
	 */
	private void connectToHxm() {
		/*
		 * Update the status to connecting so the user can tell what's happening
		 */
		mStatus.setText(R.string.connecting);

		/*
		 * Setup the service that will talk with the Hxm
		 */
		if (mHxmService == null) 
			setupHrm();

		/*
		 * Look for an Hxm to connect to, if none is found tell the user
		 * about it
		 */
		if ( getFirstConnectedHxm() ) {
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mHxMAddress);
			mHxmService.connect(device);    // Attempt to connect to the device
		} else {
			mStatus.setText(R.string.nonePaired);           
		}

	}


	/*
	 * Loop through all the connected bluetooth devices, the first one that 
	 * starts with HXM will be assumed to be our Zephyr HxM Heart Rate Monitor,
	 * and this is the device we will connect to
	 * 
	 * returns true if a HxM is found and the global device address has been set 
	 */
	private boolean getFirstConnectedHxm() {

		/*
		 * Initialize the global device address to null, that means we haven't 
		 * found a HxM to connect to yet        
		 */
		mHxMAddress = null;     
		mHxMName = null;


		/*
		 * Get the local Bluetooth adapter
		 */
		BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		/*
		 *  Get a set of currently paired devices to cycle through, the Zephyr HxM must
		 *  be paired to this Android device, and the bluetooth adapter must be enabled
		 */
		Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();

		/*
		 * For each device check to see if it starts with HXM, if it does assume it
		 * is the Zephyr HxM device we want to pair with      
		 */
		if (bondedDevices.size() > 0) {
			for (BluetoothDevice device : bondedDevices) {
				String deviceName = device.getName();
				if ( deviceName.startsWith("HXM") ) {
					/*
					 * we found an HxM to try to talk to!, let's remember its name and 
					 * stop looking for more
					 */
					mHxMAddress = device.getAddress();
					mHxMName = device.getName();
					Log.d(TAG,"getFirstConnectedHxm() found a device whose name starts with 'HXM', its name is "+mHxMName+" and its address is ++mHxMAddress");
					break;
				}
			}
		}

		/*
		 * return true if we found an HxM and set the global device address
		 */
		return (mHxMAddress != null);
	}

	/*
	 * Our onCreate() needs to setup the main activity that we will use to        
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");

		mContext = getApplicationContext();

		/*
		 * Set our content view
		 */
		setContentView(R.layout.activity_main);
        //setup butterknife
        ButterKnife.bind(this);

        // Sets the Toolbar to act as the ActionBar for this Activity window.
		// Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.app_name);

		/*
		 *  Set up the status text view, if we can't do it something is wrong with
		 *  how this application package was built, in that case display a message
		 *  and give up.        
		 */
		if ( mStatus == null ) {
			Toast.makeText(this, "Something went very wrong, missing resource, rebuild the application", Toast.LENGTH_LONG).show();
			finish();
		}            

		/*
		 * Put some initial information into our display until we have 
		 * something more interesting to tell the user about 
		 */
		mStatus.setText(R.string.initializing);


		/*
		 *  Get the default bluetooth adapter, if it fails there is not much we can do
		 *  so show the user a message and then close the application
		 */
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		/*
		 *  If the adapter is null, then Bluetooth is not supported
		 */
		if (mBluetoothAdapter == null) {
			/*
			 * Blutoooth needs to be available on this device, and also enabled.  
			 */
			Toast.makeText(this, "Bluetooth is not available or not enabled", Toast.LENGTH_LONG).show();
			mStatus.setText(R.string.noBluetooth);

		} else {
			/*
			 * Everything should be good to go so let's try to connect to the HxM
			 */
			if (!mBluetoothAdapter.isEnabled()) {
				mStatus.setText(R.string.btNotEnabled);
				Log.d(TAG, "onStart: Blueooth adapter detected, but it's not enabled");
			} else {
				mStatus.setText(R.string.connecting);
				connectToHxm();
			}
		}        
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");

		/*
		 * Check if there is a bluetooth adapter and if it's enabled,
		 * error messages and status updates as appropriate
		 */
		if (mBluetoothAdapter != null ) {
			// If BT is not on, request that it be enabled.
			// setupChat() will then be called during onActivityResult     
			if (!mBluetoothAdapter.isEnabled()) {
				mStatus.setText(R.string.btNotEnabled);
				Log.d(TAG, "onStart: Blueooth adapter detected, but it's not enabled");
			}
		} else {
			mStatus.setText(R.string.noBluetooth);
			Log.d(TAG, "onStart: No blueooth adapter detected, it needs to be present and enabled");
		}

	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
		if (mHxmService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't started already
			if (mHxmService.getState() == R.string.HXM_SERVICE_RESTING) {
				// Start the Bluetooth scale services
				mHxmService.start();
			}
		}
	}

	private void setupHrm() {
		Log.d(TAG, "setupScale:");

		// Initialize the service to perform bluetooth connections
		mHxmService = new HxmService(this, mHandler);
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mHxmService != null) mHxmService.stop();
		Log.e(TAG, "--- ON DESTROY ---");
	}

	/**
	 * Sends a message.
	 * @param message  A string of text to send.
	 */
	// The Handler that gets information back from the hrm service
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case R.string.HXM_SERVICE_MSG_STATE: 
				Log.d(TAG, "handleMessage():  MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case R.string.HXM_SERVICE_CONNECTED:
					if ((mStatus != null) && (mHxMName != null)) {
						mStatus.setText(R.string.connectedTo);
						mStatus.append(mHxMName);
					}
					break;

				case R.string.HXM_SERVICE_CONNECTING:
					mStatus.setText(R.string.connecting);
					break;

				case R.string.HXM_SERVICE_RESTING:
					if (mStatus != null ) {
						mStatus.setText(R.string.notConnected);
					}
					break;
				}
				break;

			case R.string.HXM_SERVICE_MSG_READ: {
				/*
				 * MESSAGE_READ will have the byte buffer in tow, we take it, build an instance
				 * of a HrmReading object from the bytes, and then display it into our view
				 */
				byte[] readBuf = (byte[]) msg.obj;
				HrmReading hrm = new HrmReading( readBuf );
				displayHrmReading(hrm);

				if(isExternalStorageWritable() && isRecording){
					exportData(hrm);
				}
				break;
			}

			case R.string.HXM_SERVICE_MSG_TOAST:
				Toast.makeText(getApplicationContext(), msg.getData().getString(null),Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.scan:
			connectToHxm();
			return true;

		case R.id.record:
			isRecording=(isRecording ? false : true);
			recordingTag=(isRecording ? ""+System.currentTimeMillis() : recordingTag);
			Toast.makeText(mContext,
					(isRecording ? R.string.recording_on :  R.string.recording_off),
					Toast.LENGTH_LONG).show();
			return true;

		case R.id.quit:
			finish();
			return true;
		}

		return false;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + requestCode);

		switch(requestCode)
		{  

		case 1:
			break;

		}             
	}
	
	/****************************************************************************
	 * Some utility functions to control the formatting of HxM fields into the 
	 * activity's view
	 ****************************************************************************/	
	private void displayHrmReading(HrmReading h){
		display ( R.id.stx,  h.stx );
		display ( R.id.msgId,  h.msgId );
		display ( R.id.dlc,  h.dlc );
		display ( R.id.firmwareId,   h.firmwareId );
		display ( R.id.firmwareVersion,   h.firmwareVersion );
		display ( R.id.hardwareId,   h.hardWareId );
		display ( R.id.hardwareVersion,   h.hardwareVersion );
		display ( R.id.batteryChargeIndicator,  h.batteryIndicator );
		display ( R.id.heartRate, h.heartRate );
		display ( R.id.heartBeatNumber,  h.heartBeatNumber );
		display ( R.id.hbTimestamp1,   h.hbTime1 );
		display ( R.id.hbTimestamp2,   h.hbTime2 );
		display ( R.id.hbTimestamp3,   h.hbTime3 );
		display ( R.id.hbTimestamp4,   h.hbTime4 );
		display ( R.id.hbTimestamp5,   h.hbTime5 );
		display ( R.id.hbTimestamp6,   h.hbTime6 );
		display ( R.id.hbTimestamp7,   h.hbTime7 );
		display ( R.id.hbTimestamp8,   h.hbTime8 );
		display ( R.id.hbTimestamp9,   h.hbTime9 );
		display ( R.id.hbTimestamp10,   h.hbTime10 );
		display ( R.id.hbTimestamp11,   h.hbTime11 );
		display ( R.id.hbTimestamp12,   h.hbTime12 );
		display ( R.id.hbTimestamp13,   h.hbTime13 );
		display ( R.id.hbTimestamp14,   h.hbTime14 );
		display ( R.id.hbTimestamp15,   h.hbTime15 );
		display ( R.id.reserved1,   h.reserved1 );
		display ( R.id.reserved2,   h.reserved2 );
		display ( R.id.reserved3,   h.reserved3 );
		display ( R.id.distance,   h.distance );
		display ( R.id.speed,   h.speed );
		display ( R.id.strides,  (int)h.strides );
		display ( R.id.reserved4,  h.reserved4 );
		display ( R.id.reserved5,  h.reserved5 );
		display ( R.id.crc,  h.crc );
		display ( R.id.etx,  h.etx );   
	}
	
	/*
	 * display a byte value
	 */
	private void display  ( int nField, byte d ) {   
		String INT_FORMAT = "%x";
		String s = String.format(INT_FORMAT, d);
		display( nField, s  );
	}

	/*
	 * display an integer value
	 */
	private void display  ( int nField, int d ) {   
		String INT_FORMAT = "%d";
		String s = String.format(INT_FORMAT, d);
		display( nField, s  );
	}

	/*
	 * display a long integer value
	 */
	private void display  ( int nField, long d ) {   
		String INT_FORMAT = "%d";
		String s = String.format(INT_FORMAT, d);
		display( nField, s  );
	}

	/*
	 * display a character string
	 */
	private void display ( int nField, CharSequence  str  ) {
		TextView tvw = (TextView) findViewById(nField);
		if ( tvw != null )
			tvw.setText(str);
	}
	
}
