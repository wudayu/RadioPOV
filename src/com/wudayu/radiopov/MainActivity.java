package com.wudayu.radiopov;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

@EActivity(R.layout.activity_main)
public class MainActivity extends Activity {

	private static final int SELECT_PICTURE_REQUEST_CODE = 0xF3CE;
	private static final UUID MY_UUID_SECURE = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	@ViewById(R.id.top_layout)
	RelativeLayout topLayout;
	@ViewById(R.id.spinner_select_device)
	Spinner spinnerSelectDevice;
	@ViewById(R.id.tv_bluetooth_entrance)
	TextView tvBluetoothEntrance;
	@ViewById(R.id.tv_hint_device)
	TextView tvHintDevice;
	@ViewById(R.id.btn_select_image)
	Button btnSelectImage;
	@ViewById(R.id.relative_activity_main)
	RelativeLayout relativeActivityMain;
	@ViewById(R.id.btn_send_image)
	Button btnSendImage;
	@ViewById(R.id.bottom_layout)
	RelativeLayout bottomLayout;
	@ViewById(R.id.img_selected_photo)
	ImageView imgSelectedPhoto;


	private String selectedImagePath;

	ArrayAdapter<String> mArrayAdapter;

	List<BluetoothDevice> devices;

	Set<BluetoothDevice> pairedDevices;

	BluetoothAdapter mBluetoothAdapter;

	int currentDevice = -1;

	@Override
	protected void onResume() {
		initializeDevices();
		super.onResume();
	}

	@Background
	void initializeDevices() {
		mBluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Device does not support Bluetooth
		}

		if (!mBluetoothAdapter.isEnabled()) {
			// Toast please enable
		}

		mArrayAdapter = new ArrayAdapter<String>(MainActivity.this,
				android.R.layout.simple_spinner_item);
		devices = new ArrayList<BluetoothDevice>();
		pairedDevices = mBluetoothAdapter.getBondedDevices();
		// If there are paired devices
		if (null != pairedDevices && pairedDevices.size() > 0) {
			// Loop through paired devices
			for (BluetoothDevice device : pairedDevices) {
				// Add the name and address to an array adapter
				mArrayAdapter
						.add(device.getName() + ", " + device.getAddress());
				devices.add(device);
			}
		}

		setAdapter();
	}

	@UiThread
	void setAdapter() {
		mArrayAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerSelectDevice.setAdapter(mArrayAdapter);
		spinnerSelectDevice
				.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						currentDevice = position;
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
						currentDevice = -1;
					}
				});
	}

	@Click
	void tvBluetoothEntranceClicked() {
		Intent intentBluetooth = new Intent();
		intentBluetooth
				.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
		startActivity(intentBluetooth);
	}

	@Click
	void btnSelectImageClicked() {
		Intent intent = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(Intent.createChooser(intent, "Select Picture"),
				SELECT_PICTURE_REQUEST_CODE);
	}

	@OnActivityResult(SELECT_PICTURE_REQUEST_CODE)
	void onResult(int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			Uri selectedImageUri = data.getData();

			selectedImagePath = getPath(selectedImageUri);
			Picasso.with(MainActivity.this).load("file://" + selectedImagePath)
					.into(imgSelectedPhoto);
		}
	}

	@SuppressWarnings("deprecation")
	private String getPath(Uri uri) {
		if (uri == null) {
			Toast.makeText(MainActivity.this, R.string.toast_illegal_image,
					Toast.LENGTH_SHORT).show();
			return null;
		}

		// try to retrieve the image from the media store first
		// this will only work for images selected from gallery
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		if (cursor != null) {
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		}

		return uri.getPath();
	}

	@Click
	void btnSendImageClicked() {
		if (currentDevice < 0 || null == selectedImagePath
				|| "".equals(selectedImagePath)) {
			Toast.makeText(MainActivity.this,
					R.string.device_or_image_required, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// TODO Complete the send event
		// Toast.makeText(MainActivity.this, R.string.send_image_success, Toast.LENGTH_SHORT).show();
		// Toast.makeText(MainActivity.this, R.string.send_image_failure, Toast.LENGTH_SHORT).show();

		new ConnectThread(devices.get(currentDevice)).start();
		// sendImage(BluetoothDevice device);
	}

	private class ConnectThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	 
	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
	        } catch (IOException e) { }
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	        // Cancel discovery because it will slow down the connection
	        mBluetoothAdapter.cancelDiscovery();
	 
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return;
	        }
	 
	        // Do work to manage the connection (in a separate thread)
	        manageConnectedSocket(mmSocket);
	    }
	 
	    private void manageConnectedSocket(BluetoothSocket socket) {
	    	try {
				OutputStream mmOutStream = socket.getOutputStream();
				// procedure write into stream
				mmOutStream.write(null);
			} catch (IOException e) {
				e.printStackTrace();
				this.cancel();
			}

	    	// mHandler
			this.cancel();
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}
	/*handler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);

				bindAppsListIntoListView();

				if (progressDialog != null) {
					progressDialog.dismiss();
					progressDialog = null;
				}
				chkAllItem.setChecked(false);
			}
		};
		
		if (progressDialog == null) {
			initializeExceptionList();

			progressDialog = ProgressDialog
			.show(AppsActivity.this,
					getString(R.string.str_title_refresh_apps_list_activity_apps),
					getString(R.string.str_content_refresh_apps_list_activity_apps));

			new Thread() {
				public void run() {
					getAppsListWithoutOrder();
	
					handler.sendEmptyMessage(0);
				}
			}.start();
		}*/

}