package com.painless.pc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.painless.pc.singleton.Globals;

public class PermissionDialog extends Activity {

  private Intent mTargetIntent;

  @Override
  protected void onCreate(Bundle args) {
    super.onCreate(args);

    mTargetIntent = getIntent().getParcelableExtra("target");

    View view = LayoutInflater.from(this).inflate(R.layout.permission_prompt, null);

    new AlertDialog.Builder(this)
        .setTitle(R.string.pp_title)
        .setView(view)
        .setPositiveButton(R.string.lbl_settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setData(Uri.parse("package:" + getPackageName())));
                checkNeverAndFinish(view);
            }
        })
        .setNegativeButton(R.string.pp_ignore, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mTargetIntent != null) {
                    startActivity(mTargetIntent);
                }
                checkNeverAndFinish(view);
            }
        })
        .setOnDismissListener(d -> finish())
        .show();
  }

  private void checkNeverAndFinish(View view) {
    CheckBox chk = view.findViewById(R.id.chk_never);
    if (chk != null && chk.isChecked()) {
      Globals.getAppPrefs(this).edit().putBoolean("prompt_permission", true).apply();
    }
    finish();
  }
}
