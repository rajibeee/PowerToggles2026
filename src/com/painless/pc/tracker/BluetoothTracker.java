package com.painless.pc.tracker;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import com.painless.pc.R;
import com.painless.pc.singleton.Globals;

public final class BluetoothTracker extends AbstractDoubleClickTracker  {

	private static final String CHANGE_ACTION = BluetoothAdapter.ACTION_STATE_CHANGED;

	@Override
	public String getChangeAction() {
		return CHANGE_ACTION;
	}

	public BluetoothTracker(int trackerId, SharedPreferences pref) {
		super(trackerId, pref, getTriImageConfig(R.drawable.icon_toggle_bluetooth));
	}

	@Override
	public int getActualState(Context context) {
		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null) {
			return STATE_UNKNOWN;
		}
		switch (adapter.getState()) {
			case BluetoothAdapter.STATE_ON :
				return STATE_ENABLED;
			case BluetoothAdapter.STATE_OFF :
				return STATE_DISABLED;
			case BluetoothAdapter.STATE_TURNING_ON :
				return STATE_TURNING_ON;
			case BluetoothAdapter.STATE_TURNING_OFF :
				return STATE_TURNING_OFF;
			default :
				return STATE_UNKNOWN;
		}
	}

	@Override
	protected void requestStateChange(Context context, boolean desiredState) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			// BluetoothAdapter.enable()/disable() removed in API 33; open settings instead
			Globals.startIntent(context, new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
			return;
		}
		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null) {
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			// API 31+: need BLUETOOTH_CONNECT permission
			if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
					!= PackageManager.PERMISSION_GRANTED) {
				Globals.startIntent(context, new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
				return;
			}
		}
		if (desiredState) {
			adapter.enable();
		} else {
			adapter.disable();
		}
	}

	@Override
	Intent getDCIntent(Context context) {
		return new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
	}
}
