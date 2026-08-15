package com.vamshi.ai;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class LaunchTrampolineActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String packageName = getIntent().getStringExtra("packageName");

        if (packageName != null) {
            AppLauncherUtil.launch(this, packageName);
        }

        finish();
    }
}
