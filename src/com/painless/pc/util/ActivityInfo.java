package com.painless.pc.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.painless.pc.singleton.BitmapUtils;
import com.painless.pc.singleton.Debug;
import com.painless.pc.util.SectionAdapter.SectionItem;

public class ActivityInfo extends SectionItem implements Comparable<ActivityInfo> {

	public final Bitmap originalIcon;
	public final Intent targetIntent;

	public final String packageName;
	public final String name;
	public final boolean isReceiver;

	public ActivityInfo(String label, Drawable icon, Intent intent, Bitmap orgIcon, String packageName, String name, boolean isReceiver) {
		super(label, icon);
		targetIntent = intent;
		originalIcon = orgIcon;
		this.packageName = packageName;
		this.name = name;
		this.isReceiver = isReceiver;
	}

	@Override
	public int compareTo(ActivityInfo another) {
		return label.compareTo(another.label);
	}

	/**
	 * Returns the correct PackageManager query flags for the current Android version.
	 * On Android 11+ (API 30+) we must use MATCH_ALL to see all installed apps,
	 * not just those that have explicitly declared visibility to us.
	 */
	private static int getQueryFlags() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return PackageManager.MATCH_ALL;
		}
		return 0;
	}

	public static ArrayList<ActivityInfo> loadList(Context context, Intent targetIntent) {
		return parseResolvers(context, targetIntent,
				context.getPackageManager().queryIntentActivities(targetIntent, getQueryFlags()), false);
	}

	public static ArrayList<ActivityInfo> loadReceivers(Context context, Intent targetIntent) {
		return parseResolvers(context, targetIntent,
				context.getPackageManager().queryBroadcastReceivers(targetIntent, getQueryFlags()), true);
	}

	private static ArrayList<ActivityInfo> parseResolvers(Context context, Intent targetIntent, List<ResolveInfo> resolvers, boolean isReceiver) {
		ArrayList<ActivityInfo> result = new ArrayList<ActivityInfo>();
		PackageManager pm = context.getPackageManager();
		Resources res = context.getResources();	
		for (ResolveInfo info : resolvers) {
			// Skip disabled activities
			if (info.activityInfo != null && !info.activityInfo.enabled) {
				continue;
			}
			try {
				Bitmap icon = BitmapUtils.drawableToBitmap(info.loadIcon(pm), context);
				Intent myIntent = new Intent(targetIntent);
				myIntent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
				ActivityInfo item = new ActivityInfo(
						info.loadLabel(pm).toString(),
						new BitmapDrawable(res, icon),
						myIntent,
						icon,
						info.activityInfo.packageName,
						info.activityInfo.name,
						isReceiver);
				result.add(item);
			} catch (Exception e) {
				Debug.log(e);
			}
		}
		Collections.sort(result);
		return result;
	}
}