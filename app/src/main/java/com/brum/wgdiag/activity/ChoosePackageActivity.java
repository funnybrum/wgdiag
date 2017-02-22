package com.brum.wgdiag.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.brum.wgdiag.R;
import com.brum.wgdiag.bluetooth.Service;
import com.brum.wgdiag.command.diag.Package;
import com.brum.wgdiag.command.diag.Packages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for selecting what diagnostic package to be executed.
 */
public class ChoosePackageActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_activity);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Service.stop();
    }
}