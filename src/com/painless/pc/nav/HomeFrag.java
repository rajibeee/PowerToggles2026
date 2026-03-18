package com.painless.pc.nav;

import java.io.File;
import java.io.FileOutputStream;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.painless.pc.PCWidgetActivity;
import com.painless.pc.R;
import com.painless.pc.singleton.BackupUtil;
import com.painless.pc.singleton.Debug;
import com.painless.pc.singleton.Globals;
import com.painless.pc.singleton.SettingStorage;
import com.painless.pc.util.Thunk;
import com.painless.pc.util.WidgetSetting;

public class HomeFrag extends AbsListFrag {

  private static final int REQUEST_BACKUP  = 10;
  private static final int REQUEST_RESTORE = 11;
  private static final String SHARE_FILE_NAME = "widget.zip";

  @Thunk Context mContext;
  @Thunk LayoutInflater mLf;
  @Thunk SharedPreferences mExtraPrefs;
  private MyAdapter mAdapter;

  private ListItem mItemUnderAction;

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mContext = view.getContext();
    mLf = LayoutInflater.from(mContext);
    mExtraPrefs = mContext.getSharedPreferences(Globals.EXTRA_PREFS_NAME, Context.MODE_PRIVATE);
    setEmptyMsg(R.string.wp_no_widget, R.string.wp_add_help);

    mAdapter = new MyAdapter(mContext);
    setListAdapter(mAdapter);
    getListView().setDividerHeight(0);
    getListView().setSelector(android.R.color.transparent);
  }

  @Override
  public void onResume() {
    super.onResume();
    if (mAdapter != null) {
      mAdapter.clear();
      final AppWidgetManager awm = AppWidgetManager.getInstance(mContext);
      for (final int widgetId : awm.getAppWidgetIds(new ComponentName(mContext, PCWidgetActivity.class))) {
        Log.e("PC", "WidgetID " + widgetId);
        mAdapter.add(new ListItem(widgetId, this));
      }
    }
  }

  /** Returns the app-specific backup folder (no permission needed on any Android version). */
  private File getBackupDir() {
    // Use external files dir if available (shows up in Files app), otherwise internal cache
    File extDir = mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
    if (extDir == null) extDir = mContext.getFilesDir();
    File dir = new File(extDir, "PowerToggles_Backups");
    if (!dir.exists()) dir.mkdirs();
    return dir;
  }

  @Thunk void handleMenuClicked(View target, MenuItem item) {
    mItemUnderAction = (ListItem) target.getTag(R.id.btn_menu);
    int id = item.getItemId();

    if (id == R.id.mnu_share) {
      try {
        BackupUtil.createBackup(
            mContext.openFileOutput(SHARE_FILE_NAME, Context.MODE_WORLD_READABLE),
            mItemUnderAction.widgetId, mContext);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mContext.getFileStreamPath(SHARE_FILE_NAME)));
        shareIntent.setType("application/zip");
        startActivity(Intent.createChooser(shareIntent, getString(R.string.wp_share)));
      } catch (Throwable e) {
        Debug.log(e);
        Toast.makeText(mContext, R.string.msg_unknown_error, Toast.LENGTH_LONG).show();
      }

    } else if (id == R.id.mnu_backup) {
      // Write to app-specific external folder — no storage permission needed
      File backupDir = getBackupDir();
      String fileName = "widget_" + mItemUnderAction.widgetId + "_"
          + System.currentTimeMillis() + ".zip";
      File backupFile = new File(backupDir, fileName);
      try {
        BackupUtil.createBackup(new FileOutputStream(backupFile), mItemUnderAction.widgetId, mContext);
        // Persist backup path so it shows permanently under the widget card
        String backupPath = backupFile.getAbsolutePath();
        mExtraPrefs.edit()
            .putString("last_backup" + mItemUnderAction.widgetId, backupPath)
            .apply();
        // Refresh the card to show the new backup path immediately
        mItemUnderAction.clearDisplayView();
        mAdapter.notifyDataSetChanged();
        Toast.makeText(mContext,
            "Backup saved to:\n" + backupPath,
            Toast.LENGTH_LONG).show();
      } catch (Throwable e) {
        Debug.log(e);
        Toast.makeText(mContext, getResources().getStringArray(R.array.wc_export_msg)[2], Toast.LENGTH_LONG).show();
      }

    } else if (id == R.id.mnu_restore) {
      // Use system file picker (SAF) — works on all Android versions without permission
      Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      picker.addCategory(Intent.CATEGORY_OPENABLE);
      picker.setType("application/zip");
      // Hint the picker to start in our backup folder
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        picker.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,
            Uri.fromFile(getBackupDir()));
      }
      startActivityForResult(picker, REQUEST_RESTORE);

    } else if (id == R.id.mnu_delete) {
      new AlertDialog.Builder(mContext)
          .setTitle(R.string.wp_delete)
          .setMessage(Html.fromHtml(getString(R.string.wp_delete_msg)))
          .setNegativeButton(R.string.act_cancel, null)
          .setPositiveButton(R.string.act_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.dismiss();
              doDelete();
            }
          }).create().show();
    }
  }

  @Thunk void doDelete() {
    try {
      AppWidgetHost host = new AppWidgetHost(mContext.getApplicationContext(), 0);
      host.deleteAppWidgetId(mItemUnderAction.widgetId);
      mAdapter.remove(mItemUnderAction);
    } catch (Throwable e) {
      Debug.log(e);
      Toast.makeText(mContext, R.string.msg_unknown_error, Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != Activity.RESULT_OK || data == null) {
      return;
    }
    if (requestCode == REQUEST_RESTORE) {
      Uri uri = data.getData();
      if (uri == null) return;
      // Persist permission so we can read the file
      try {
        mContext.getContentResolver().takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      } catch (Exception ignored) { }

      if (BackupUtil.importBackupFromUri(uri, mContext, mItemUnderAction.widgetId)) {
        Toast.makeText(mContext, R.string.wp_restore, Toast.LENGTH_SHORT).show();
        onResume();
      } else {
        Toast.makeText(mContext, getResources().getStringArray(R.array.wc_import_msg)[1], Toast.LENGTH_LONG).show();
      }
    }
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    Globals.showWidgetConfig(mAdapter.getItem(position).widgetId, mContext, false);
  }

  private static class MyAdapter extends ArrayAdapter<ListItem> {

    public MyAdapter(Context context) {
      super(context, 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      return getItem(position).getDisplayView();
    }
  }

  /**
   * Just a holder class so that adapter.remove does not remove the first item in the list.
   */
  private static class ListItem {
    @Thunk final int widgetId;
    private final WidgetSetting settings;
    private final HomeFrag parent;

    private View displayView;

    @Thunk ListItem(int widgetId, HomeFrag parent) {
      this.widgetId = widgetId;
      this.settings = SettingStorage.getSettingForWidget(parent.mContext, widgetId);
      this.parent = parent;
    }

    /** Clears the cached view so it is rebuilt on next getDisplayView() call. */
    @Thunk void clearDisplayView() {
      displayView = null;
    }

    @Thunk View getDisplayView() {
      if (displayView == null) {
        displayView = parent.mLf.inflate(R.layout.widget_picker_card, null);
        RemoteViews widgetPreview = PCWidgetActivity.getRemoteView(parent.mContext, settings, false, widgetId);

        MyPopupMenu menu = new MyPopupMenu(parent, displayView.findViewById(R.id.btn_menu));
        menu.getMenuInflater().inflate(R.menu.widget_menu, menu.getMenu());

        displayView.findViewById(R.id.btn_menu).setTag(R.id.btn_menu, this);

        LinearLayout wrapper = (LinearLayout) displayView.findViewById(R.id.layout_wrapper);
        wrapper.addView(widgetPreview.apply(parent.mContext, wrapper));
        wrapper.setEnabled(false);

        for (int id : Globals.BUTTONS) {
          View btn = wrapper.findViewById(id);
          btn.setClickable(false);
          btn.setFocusable(false);
          btn.setBackgroundColor(Color.TRANSPARENT);
        }

        ((TextView) displayView.findViewById(R.id.txt_time)).setText(
            Globals.getTimeDiff(parent.mExtraPrefs.getLong("last_edit" + widgetId, 0), parent.mContext));

        // Show last backup path permanently if available
        TextView backupPathView = (TextView) displayView.findViewById(R.id.txt_backup_path);
        String lastBackup = parent.mExtraPrefs.getString("last_backup" + widgetId, null);
        if (lastBackup != null) {
          backupPathView.setText(parent.mContext.getString(R.string.wp_last_backup, lastBackup));
          backupPathView.setVisibility(View.VISIBLE);
        } else {
          backupPathView.setVisibility(View.GONE);
        }
      }
      return displayView;
    }
  }

  private static class MyPopupMenu extends PopupMenu implements OnMenuItemClickListener, OnClickListener {

    private final View mAnchor;
    private final HomeFrag mParent;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public MyPopupMenu(HomeFrag parent, View anchor) {
      super(parent.mContext, anchor);
      mAnchor = anchor;
      mParent = parent;
      mAnchor.setOnClickListener(this);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        mAnchor.setOnTouchListener(getDragToOpenListener());
      }
      setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
      mParent.handleMenuClicked(mAnchor, item);
      return true;
    }

    @Override
    public void onClick(View v) {
      show();
    }
  }
}
