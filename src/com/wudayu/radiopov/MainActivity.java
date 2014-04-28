package com.wudayu.radiopov;

import java.io.DataOutputStream;
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

import Jama.Matrix;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
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

	/*

	Name	Value	Size
	Base UUID Value (Used in promoting 16-bit and 32-bit UUIDs to 128-bit UUIDs)	0x0000000000001000800000805F9B34FB	128-bit
	SDP	0x0001	16-bit
	RFCOMM	0x0003	16-bit
	OBEX	0x0008	16-bit
	HTTP	0x000C	16-bit
	L2CAP	0x0100	16-bit
	BNEP	0x000F	16-bit
	Serial Port	0x1101	16-bit
	ServiceDiscoveryServerServiceClassID	0x1000	16-bit
	BrowseGroupDescriptorServiceClassID	0x1001	16-bit
	PublicBrowseGroup	0x1002	16-bit
	OBEX Object Push Profile	0x1105	16-bit
	OBEX File Transfer Profile	0x1106	16-bit
	Personal Area Networking User	0x1115	16-bit
	Network Access Point	0x1116	16-bit
	Group Network	0x1117	16-bit

	 */
	private static final int SELECT_PICTURE_REQUEST_CODE = 0xF3CE;
	private static final UUID MY_UUID = UUID
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

	ProgressDialog progressDialog;

	Handler mHandler;

	int currentDevice = -1;

	int[][][] data;

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

	@SuppressLint("HandlerLeak")
	@Click
	void btnSendImageClicked() {
		if (currentDevice < 0 || null == selectedImagePath
				|| "".equals(selectedImagePath)) {
			Toast.makeText(MainActivity.this,
					R.string.device_or_image_required, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		/* DEBUG CODE
		compressImage();

    	for (int i = 0; i < 256; ++i) {
    		for (int j = 0; j < 16; ++j) {
    			for (int k = 0; k < 16; ++k) {
    				System.out.printf("%d", data[i][j][k]);
    				System.out.printf("%c", 0x2C);
    				// dos.writeChar(',');
    			}
    		}
			System.out.printf("%c", 0x0A);
			// dos.writeChar('\n');
    	}
		*/

		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);

				if (progressDialog != null) {
					progressDialog.dismiss();
					progressDialog = null;
				}

				switch(msg.what) {
				case 1:
					Toast.makeText(MainActivity.this, R.string.send_image_success, Toast.LENGTH_SHORT).show();
					break;
				default:
					Toast.makeText(MainActivity.this, R.string.send_image_failure, Toast.LENGTH_SHORT).show();
					break;
				}
				btnSendImage.setClickable(true);
			}
		};
		
		if (progressDialog == null) {
			progressDialog = ProgressDialog
			.show(MainActivity.this,"Sending", "Please wait...");
			btnSendImage.setClickable(false);
			new ConnectThread(devices.get(currentDevice)).start();
		}
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
	            tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
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

				compressImage();

				DataOutputStream dos = new DataOutputStream(mmOutStream);
		    	for (int i = 0; i < 256; ++i) {
		    		for (int j = 0; j < 16; ++j) {
		    			for (int k = 0; k < 16; ++k) {
		    				dos.writeShort(data[i][j][k]);
		    				dos.writeChar(0x2C);
		    				// dos.writeChar(',');
		    			}
		    		}
    				dos.writeChar(0x0A);
    				// dos.writeChar('\n');
		    	}
		    	mmOutStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				this.cancel();
			}
	    	Message message = new Message();
	    	message.arg1 = 1;
	    	mHandler.sendMessage(message);
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

	private void compressImage() {
		Bitmap originalBitmap = BitmapFactory
				.decodeFile(selectedImagePath);
		int width = originalBitmap.getWidth();
		int height = originalBitmap.getHeight();
		int halfWidth = width / 2;
		int halfHeight = height / 2;
		int[] pix = new int[width * height];
		originalBitmap.getPixels(pix, 0, width, 0, 0, width, height);
		Matrix dataR = getDataR(pix, width, height).times(1.0 / 16);
		Matrix dataG = getDataG(pix, width, height).times(1.0 / 16);
		Matrix dataB = getDataB(pix, width, height).times(1.0 / 16);
		int L = Math.min(halfWidth, halfHeight);
		
		double[][] midata = new double[256][];
		for (int i = 0; i < 256; ++i) {
			midata[i] = new double[3 * 32];
			double ang = (i + 1) * Math.PI / 128;
			for (int j = 0; j < 32; ++j) {
				double l = L * 0.2 + L * 0.8 / 32 * (j + 1);
				midata[i][j * 3 + 0] = dataB.get((int)(Math.floor(halfWidth - (l - 1) * Math.sin(ang))), (int)(Math.floor(halfHeight + (l - 1) * Math.cos(ang))));
				midata[i][j * 3 + 1] = dataG.get((int)(Math.floor(halfWidth - (l - 1) * Math.sin(ang))), (int)(Math.floor(halfHeight + (l - 1) * Math.cos(ang))));
				midata[i][j * 3 + 2] = dataR.get((int)(Math.floor(halfWidth - (l - 1) * Math.sin(ang))), (int)(Math.floor(halfHeight + (l - 1) * Math.cos(ang))));
			}
		}
		data = new int[256][][];
		for (int i = 0; i < 256; ++i) {
			data[i] = new int[16][];
			for (int j = 0; j < 16; ++j) {
				data[i][j] = new int[16];
				double[] temp = { midata[i][16 - j - 1],
						midata[i][16 - j - 1 + 16], midata[i][16 - j - 1 + 32],
						midata[i][16 - j - 1 + 48], midata[i][16 - j - 1 + 64],
						midata[i][16 - j - 1 + 80] };
				for (int k = 0; k < 16; ++k) {
					data[i][j][k] = 0;
					for (int t = 0; t < 6; ++t) {
						if (temp[t] > (k + 1))
							data[i][j][k] = (int) (data[i][j][k] + Math.pow(2, t));
					}
				}
			}
		}
	}

	private Matrix getDataR(int[] pix, int width, int height) {
		Matrix dataR = new Matrix(width, height, 0.0);
		// Apply pixel-by-pixel change
		int index = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int r = ((pix[index] >> 16) & 0xff);
				dataR.set(x, y, r);
				index++;
			} // x
		} // y
		return dataR;
	}

	private Matrix getDataG(int[] pix, int width, int height) {
		Matrix dataG = new Matrix(width, height, 0.0);
		// Apply pixel-by-pixel change
		int index = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int g = ((pix[index] >> 8) & 0xff);
				dataG.set(x, y, g);
				index++;
			} // x
		} // y
		return dataG;
	}

	private Matrix getDataB(int[] pix, int width, int height) {
		Matrix dataB = new Matrix(width, height, 0.0);
		// Apply pixel-by-pixel change
		int index = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int b = (pix[index] & 0xff);
				dataB.set(x, y, b);
				index++;
			} // x
		} // y
		return dataB;
	}

	/*
	private Matrix getDataGray(int[] pix, int width, int height) {
		Matrix dataGray = new Matrix(width, height, 0.0);
		// Apply pixel-by-pixel change
		int index = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int r = ((pix[index] >> 16) & 0xff);
				int g = ((pix[index] >> 8) & 0xff);
				int b = (pix[index] & 0xff);
				int gray = (int) (0.3 * r + 0.59 * g + 0.11 * b);
				dataGray.set(x, y, gray);
				index++;
			} // x
		} // y
		return dataGray;
	}
	*/
}