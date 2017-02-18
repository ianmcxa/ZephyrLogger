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

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.mcxa.zephyrlogger.hxm.HrmReading;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

	private boolean isRecording=false;
	private String recordingTag;

	/*
	 *  TAG for Debugging Log
	 */
	private static final String TAG = "ZephyrLogger";

	/**
	 * CSV format for the file
	 */
	public static final String CSV_FORMAT = "timeInMs,stx,msgId,dlc,firmwareId,firmwareVersion,hardWareId," +
			"hardwareVersion,batteryIndicator,heartRate,heartBeatNumber,hbTime1,hbTime2," +
			"hbTime3,hbTime4,hbTime5,hbTime6,hbTime7,hbTime8,hbTime9,hbTime10,hbTime11," +
			"hbTime12,hbTime13,hbTime14,hbTime15,reserved1,reserved2,reserved3,distance," +
			"speed,strides,reserved4,reserved5,crc,etx,reserved5,crc,etx";

	/*
	 *  Layout Views
	 */
	@BindView(R.id.status) TextView mStatus;
	@BindView(R.id.toolbar) Toolbar toolbar;
	@BindView(R.id.main_button) AppCompatButton mButton;
    @BindView(R.id.main_activity_view) RelativeLayout view;

	//our text views for displaying data
	@BindView(R.id.heart_rate) TextView mHeartRate;
	@BindView(R.id.rri) TextView mRri;
	@BindView(R.id.battery) TextView mBattery;
	@BindView(R.id.speed) TextView mSpeed;

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
			FileWriter writer;
			if(!file.exists()){
				boolean created = file.createNewFile();
				if (!created) throw new IOException("Could not create data file");
				writer = new FileWriter(file, true);
				//if this is a new file, write the CSV format at the top
				writer.write(CSV_FORMAT + "\n");
			} else {
				writer = new FileWriter(file, true);
			}
			writer.write(System.currentTimeMillis()+","+m.toString()+"\n");
			writer.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Could not create logging file");
            e.printStackTrace();
			System.exit(1);
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Unsupported encoding exception thrown trying to write file");
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
			// Initialize the service to perform bluetooth connections
			mHxmService = new HxmService(this, mHandler);

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

    // Identifier for the permission request
    private static final int WRITE_STORAGE_PERMISSIONS_REQUEST = 7;

	public void getExternalStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Fire off an async request to actually get the permission
            // This will show the standard permission request dialog UI
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_STORAGE_PERMISSIONS_REQUEST);
        }
    }

    // Callback with the request from calling requestPermissions(...)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == WRITE_STORAGE_PERMISSIONS_REQUEST) {
            //if we didn't get the permission
            if (!(grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Snackbar.make(view,"This app cannot function without external storage permissions.",
                        Snackbar.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

	/*
	 * Our onCreate() needs to setup the main activity that we will use to        
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");

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
         * Request external storage permissions
         */
        getExternalStoragePermissions();

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
			final Snackbar snackbar = Snackbar.make(view,"Bluetooth is not available or not enabled",
					Snackbar.LENGTH_INDEFINITE);
			snackbar.setAction("Close", new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					snackbar.dismiss();
				}
			}).setActionTextColor(Color.WHITE);
			snackbar.show();
			mStatus.setText(R.string.noBluetooth);
			//disable button
			mButton.setEnabled(false);

		} else {
			/*
			 * Everything should be good to go so let's try to connect to the HxM
			 */
			if (!mBluetoothAdapter.isEnabled()) {
				mStatus.setText(R.string.btNotEnabled);
				Log.d(TAG, "onStart: Blueooth adapter detected, but it's not enabled");
			} else {
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

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		Log.e(TAG, "Destroying activity. Stopping bluetooth service");
		if (mHxmService != null) mHxmService.stop();
	}

	Handler mHandler = new MessageHandler(this);

	// The Handler that gets information back from the hrm service
	private static class MessageHandler extends Handler {
		private final WeakReference<MainActivity> activityReference;

		MessageHandler(MainActivity activity) {
			activityReference = new WeakReference<>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			MainActivity activity = activityReference.get();
			if (activity != null) {

				switch (msg.what) {
					case R.string.HXM_SERVICE_MSG_STATE:
						Log.d(TAG, "handleMessage():  MESSAGE_STATE_CHANGE: " + msg.arg1);
						switch (msg.arg1) {
							case R.string.HXM_SERVICE_CONNECTED:
								if ((activity.mStatus != null) && (activity.mHxMName != null)) {
									activity.mStatus.setText(R.string.connectedTo);
									activity.mStatus.append(activity.mHxMName);
									//set button to start recording
									activity.mButton.setText(activity.getResources()
											.getString(R.string.start_record));
									activity.mButton
											.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
								}
								break;

							case R.string.HXM_SERVICE_CONNECTING:
								activity.mStatus.setText(R.string.connecting);
								break;

							case R.string.HXM_SERVICE_RESTING:
								if (activity.mStatus != null) {
									activity.mStatus.setText(R.string.notConnected);
									//set button to connect
									activity.mButton.setText(activity.getResources()
											.getString(R.string.connect));
									activity.mButton
											.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_connect, 0, 0, 0);
									activity.mHeartRate.setText("");
									activity.mBattery.setText("");
									activity.mRri.setText("");
									activity.mSpeed.setText("");
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
						HrmReading hrm = new HrmReading(readBuf);
						activity.displayHrmReading(hrm);

						if (activity.isExternalStorageWritable() && activity.isRecording) {
							activity.exportData(hrm);
						}
						break;
					}

					case R.string.HXM_SERVICE_MSG_TOAST:
						String message = msg.getData().getString(null);
						if (message != null)
							Snackbar.make(activity.view, message, Snackbar.LENGTH_LONG).show();
						break;
				}
			}
		}
	}

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
			startStopRecording();
			return true;
		case R.id.quit:
			//stop the service
			mHxmService.stop();
			finish();
			return true;
		}

		return false;
	}

	@OnClick(R.id.main_button)
	public void onMainButtonCLicked() {
		if (mHxmService == null || mHxmService.getState() != R.string.HXM_SERVICE_CONNECTED)
			connectToHxm();
		else startStopRecording();
	}

	private void startStopRecording() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				== PackageManager.PERMISSION_GRANTED) {
			isRecording = !isRecording;
			recordingTag = (isRecording ? "" + System.currentTimeMillis() : recordingTag);
			Snackbar.make(view,
					(isRecording ? R.string.recording_on : R.string.recording_off),
					Snackbar.LENGTH_SHORT).show();

			//set button to start or stop recording
			if (isRecording) {
				mButton.setText(getResources().getString(R.string.stop_record));
				mButton.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_stop, 0, 0, 0);
			} else {
				mButton.setText(getResources().getString(R.string.start_record));
				mButton.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_play, 0, 0, 0);
			}
		} else {
			Snackbar.make(view,"Cannot record without external storage permissions.",
					Snackbar.LENGTH_LONG).show();
		}

	}

	/**
	 * Calculate the average RRi in milliseconds per packet recieved
	 * @param h HrmReading object
	 * @return the average RRi in ms from the recieved packet
     */
	private Long calcRRi(HrmReading h) {
		Long interval = 0L;
		/* Note that each heart beat is a value from 0 to 65535. The value rolls over
		 * at 65535, so when we have a large enough difference (I'm using 10000), we know that
		 * a rollover occured and we need to subtract 65535 from the result
		 */

		Long tmp = Math.abs(h.hbTime15 - h.hbTime14);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;
		//yeah this would probably be easier with a loop, but idk how to do that without
		//shoving all the hbTime things in an array and thus copying them needlessly
		tmp = Math.abs(h.hbTime14 - h.hbTime13);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;
		tmp = Math.abs(h.hbTime13 - h.hbTime12);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;
		tmp = Math.abs(h.hbTime12 - h.hbTime11);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;
		tmp = Math.abs(h.hbTime11 - h.hbTime10);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;
		tmp = Math.abs(h.hbTime10 - h.hbTime9);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;
		tmp = Math.abs(h.hbTime9 - h.hbTime8);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;
		tmp = Math.abs(h.hbTime8 - h.hbTime7);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;
		tmp = Math.abs(h.hbTime7 - h.hbTime6);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;
		tmp = Math.abs(h.hbTime6 - h.hbTime5);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;
		tmp = Math.abs(h.hbTime5 - h.hbTime4);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;
		tmp = Math.abs(h.hbTime4 - h.hbTime3);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;
		tmp = Math.abs(h.hbTime3 - h.hbTime2);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;
		tmp = Math.abs(h.hbTime2 - h.hbTime1);
		interval += (tmp > 10000L) ? Math.abs(tmp - 65535) : tmp;

		return interval/14;
	}

	/**
	 * Calculate speed which is measured in 1/256m/s blocks
	 * @param h HrmReading object
	 * @return Double containing speed
     */
	private Double calcSpeed(HrmReading h) {
		return ((double) h.speed)/256;
	}

	/****************************************************************************
	 * Some utility functions to control the formatting of HxM fields into the 
	 * activity's view
	 ****************************************************************************/	
	private void displayHrmReading(HrmReading h){
		mHeartRate.setText(String.format(Locale.US, "%d bpm", h.heartRate));
		mBattery.setText(String.format(Locale.US, "%d %%", h.batteryIndicator));
		mRri.setText(String.format(Locale.US, "%d ms", calcRRi(h)));
		mSpeed.setText(String.format(Locale.US, "%.1f m/s", calcSpeed(h)));
	}
}
