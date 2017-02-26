package com.brum.wgdiag.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.brum.wgdiag.R;
import com.brum.wgdiag.bluetooth.Service;
import com.brum.wgdiag.command.diag.Package;
import com.brum.wgdiag.command.diag.Packages;
import com.brum.wgdiag.logger.DiagDataLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for selecting what diagnostic package to be executed.
 */
public class ChoosePackageActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.choose_packages_activity);

        ((TextView)findViewById(R.id.title)).setText("Select diagnostic data");

        List<String> packages = new ArrayList<>();
        for (Package pkg : Packages.PACKAGES) {
            packages.add(pkg.getName());
        }

        ListAdapter listAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.select_dialog_item,
                packages
        );

        setListAdapter(listAdapter);

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent switchIntent = new Intent(this, DiagActivity.class);
        String pkgName = l.getItemAtPosition(position).toString();

        switchIntent.putExtra(DiagActivity.ACTIVITY_EXTRA_PACKAGE_NAME, pkgName);

        startActivity(switchIntent);
    }

    public void onLogButtonClick(View view) {
        try {
            File file = DiagDataLogger.getLogFile(Environment.getExternalStorageDirectory());
            Uri fileUri = Uri.fromFile(file);

            android.util.Log.d("SS", fileUri.toString());

            Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Log file");
            sendIntent.putExtra(Intent.EXTRA_TEXT, "The generated log file should be attached.");
            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(sendIntent, "Send e-mail"));
        } catch (IOException ex) {
            Toast.makeText(
                    this,
                    "Failed to share log file: " + ex.toString(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void onBackButton(View view) {
        finish();
    }
}