package com.brum.wgdiag.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.brum.wgdiag.R;
import com.brum.wgdiag.command.Processor;
import com.brum.wgdiag.util.Executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ChooseAdapterActivity extends ListActivity {
    private Map<String, String> bluetoothDevices = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Executor.bind(this);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage("Bluetooth adapter not found!")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finishAffinity();
                        }

                    })
                    .show();
            return;
        }

        if (!adapter.isEnabled()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage("Bluetooth adapter is disabled!")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finishAffinity();
                        }

                    })
                    .show();
            return;
        }

        setContentView(R.layout.choose_adapter_activity);

        ((TextView) findViewById(R.id.title)).setText("Select OBDII adapter");

        this.bluetoothDevices = new HashMap<>();
        for (BluetoothDevice device : adapter.getBondedDevices()) {
            this.bluetoothDevices.put(device.getName(), device.getAddress());
        }

        ListAdapter listAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.select_dialog_item,
                new ArrayList(this.bluetoothDevices.keySet())
        );

        setListAdapter(listAdapter);

    }

    @Override
    protected void onListItemClick(final ListView l, View v, int position, long id) {
        l.setEnabled(false);
        final Activity activity = this;

        super.onListItemClick(l, v, position, id);

        final Toast connectingToast = Toast.makeText(
                getApplicationContext(),
                "Connecting to " + l.getItemAtPosition(position).toString() + " ...",
                Toast.LENGTH_LONG);
        connectingToast.show();

        final String adapterAddress = this.bluetoothDevices.get(l.getItemAtPosition(position).toString());

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                boolean result = false;
                try {
                    result = Processor.verifyDevice(adapterAddress);
                } catch (Exception e) {
                    Log.d(ChooseAdapterActivity.class.getSimpleName(), "Got exception", e);
                }
                return result;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                l.setEnabled(true);

                if (success) {
                    connectingToast.cancel();
                    Toast.makeText(
                            getApplicationContext(),
                            "Connected...",
                            Toast.LENGTH_LONG).show();
                    Intent switchIntent = new Intent(activity, ChoosePackageActivity.class);
                    startActivity(switchIntent);
                } else {
                    connectingToast.cancel();
                    Toast.makeText(
                            getApplicationContext(),
                            "Failed to verify device.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }.execute();

    }
}