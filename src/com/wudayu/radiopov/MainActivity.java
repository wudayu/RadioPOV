package com.wudayu.radiopov;

import java.util.ArrayList;
import java.util.List;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import android.app.Activity;
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


	private static final int SELECT_PICTURE_REQUEST_CODE = 0xF3CE;

	private String selectedImagePath;

	List<String> devices = new ArrayList<String>();

	int currentDevice = -1;

	@Override
	protected void onResume() {
		initializeDevices();
		super.onResume();
	}

	@Background
	void initializeDevices() {
		devices.clear();
		devices.add("01");
		devices.add("02");
		devices.add("03");

		setAdapter();
	}

	@UiThread
	void setAdapter() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				MainActivity.this, android.R.layout.simple_spinner_item,
				devices);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerSelectDevice.setAdapter(adapter);
		spinnerSelectDevice.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
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
        intentBluetooth.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
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
			Picasso.with(MainActivity.this).load("file://" + selectedImagePath).into(imgSelectedPhoto);
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
		if (currentDevice < 0 || null == selectedImagePath || "".equals(selectedImagePath)) {
			Toast.makeText(MainActivity.this, R.string.device_or_image_required, Toast.LENGTH_SHORT).show();
			return;
		}

		// TODO Complete the send event
		// Toast.makeText(MainActivity.this, R.string.send_image_success, Toast.LENGTH_SHORT).show();
		// Toast.makeText(MainActivity.this, R.string.send_image_failure, Toast.LENGTH_SHORT).show();
	}
}