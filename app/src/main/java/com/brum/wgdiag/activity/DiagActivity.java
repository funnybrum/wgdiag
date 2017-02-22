package com.brum.wgdiag.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.brum.wgdiag.activity.utils.UIDiagDataHandler;
import com.brum.wgdiag.activity.utils.ExecutionInterrupter;
import com.brum.wgdiag.command.Processor;
import com.brum.wgdiag.command.diag.Field;
import com.brum.wgdiag.command.diag.Package;
import com.brum.wgdiag.command.diag.Packages;
import com.brum.wgdiag.command.diag.impl.CompositeDataHandler;
import com.brum.wgdiag.logger.LoggingDiagDataHandler;

import java.util.HashMap;
import java.util.Map;

public class DiagActivity extends Activity {

    public static final String ACTIVITY_EXTRA_PACKAGE_NAME = "com.brum.diag.package_name";
    private ExecutionInterrupter interrupter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String pkgName = (String)this.getIntent().getExtras().get(ACTIVITY_EXTRA_PACKAGE_NAME);
        Package pkg = null;

        for (Package p : Packages.PACKAGES) {
            if (p.getName().equals(pkgName)) {
                pkg = p;
                break;
            }
        }

        ScrollView sv = new ScrollView(this);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        sv.addView(ll);

        Map<String, TextView> handlers = new HashMap<>();

        if (pkg == null) {
            finish();
            return;
        }

        for (Field field : pkg.getFields()) {
            TextView fieldTextView = addControl(ll, field.getKey());
            handlers.put(field.getKey(), fieldTextView);
        }

        CompositeDataHandler handler = new CompositeDataHandler();
        handler.registerHandler("ui", new UIDiagDataHandler(handlers));
        handler.registerHandler("log", new LoggingDiagDataHandler());

        handler.switchPackage(pkg);

        final ExecutionInterrupter interrupter =
                Processor.executeDiagPackage(pkg, handler, new Handler(), this);

        synchronized (this) {
            if (this.interrupter != null) {
                this.interrupter.interrupt(true);
            }
            this.interrupter = interrupter;
        }

        Button b = new Button(this);
        b.setText("Back");
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                interrupter.interrupt(false); finish();
            }
        });
        ll.addView(b);

        this.setContentView(sv);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private TextView addControl(LinearLayout container, String title) {
        LinearLayout subContainer = new LinearLayout(this);
        subContainer.setOrientation(LinearLayout.HORIZONTAL);

        TextView tv_s = new TextView(this);
        tv_s.setText(title + " : ");
//        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)tv_s.getLayoutParams();
//        lp.width = subContainer.getWidth()/2;
//        tv_s.setLayoutParams(lp);

        TextView tv_v = new TextView(this);
        tv_v.setText("");

        subContainer.addView(tv_s);
        subContainer.addView(tv_v);

        container.addView(subContainer);

        return tv_v;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        synchronized (this) {
            if (this.interrupter != null) {
                interrupter.interrupt(false);
            }
        }
    }
}