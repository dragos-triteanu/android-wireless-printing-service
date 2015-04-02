/*
    Copyright (C) 2014 Sergii Pylypenko.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mediasaturn.print.init;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileInputStream;

import android.util.Log;

import java.util.*;

import android.os.StatFs;

import com.mediasaturn.print.activities.InstallApplicationActivity;
import com.mediasaturn.print.activities.MainActivityRemade;
import com.mediasaturn.print.services.Cups;
import com.mediasaturn.print.services.Proc;

import org.cups.android.R;

import java.net.URL;
import java.net.URLConnection;

public class Installer{


	public static boolean unpacking = false;
	public static InstallApplicationActivity installApplicationActivity = null;
	public static TextView installingNotice = null;

	public synchronized static boolean isInstalled(Context p)
	{
		return new File(p.getFilesDir().getAbsolutePath() + Cups.IMG + Cups.CUPSD).exists();
	}

	public static void unpackData()
	{
		if (unpacking)
			return;
		unpacking = true;
		new Thread(new Runnable()
		{
			public void run()
			{
				unpackDataThread();
			}
		}).start();
	}

	public synchronized static void unpackDataThread()
	{
		if (isInstalled(installApplicationActivity))
		{
			unpacking = false;
			Cups.startCupsDaemon(installApplicationActivity);
            installApplicationActivity.finish();
			return;
		}

		Log.i(TAG, "Extracting CUPS data");

		setInstallingNotice(installApplicationActivity.getResources().getString(R.string.please_wait_unpack));

		StatFs storage = new StatFs(installApplicationActivity.getFilesDir().getPath());
		long avail = (long)storage.getAvailableBlocks() * storage.getBlockSize() / 1024 / 1024;
		long needed = 600;
		Log.i(TAG, "Available free space: " + avail + " Mb required: " + needed + " Mb");
		if (avail < needed)
		{
			setInstallingNotice(installApplicationActivity.getResources().getString(R.string.not_enough_space, needed, avail));
			return;
		}

		try
		{
            installApplicationActivity.getFilesDir().mkdirs();
			InputStream stream = installApplicationActivity.getAssets().open("busybox-" + android.os.Build.CPU_ABI);
			String busybox = new File(installApplicationActivity.getFilesDir(), "busybox").getAbsolutePath();
			OutputStream out = new FileOutputStream(busybox);

			Cups.copyStream(stream, out);

			new Proc(new String[] {"/system/bin/chmod", "0755", busybox}, installApplicationActivity.getFilesDir());

			try
			{
				InputStream archiveAssets = installApplicationActivity.getAssets().open("dist-cups-jessie.tar.xz");
				Process proc = Runtime.getRuntime().exec(new String[] {busybox, "tar", "xJ"}, null, installApplicationActivity.getFilesDir());
				copyStreamWithProgress(130000000, archiveAssets, proc.getOutputStream());
				int status = proc.waitFor();
				Log.i(TAG, "Unpacking data from assets: status: " + status);
			}
			catch(Exception e)
			{
				Log.i(TAG, "Error unpacking data from assets: " + e.toString());
				Log.i(TAG, "No data archive in assets, trying OBB data");
				try
				{
					File obbFile = new File(installApplicationActivity.getExternalFilesDir(null).getParentFile().getParentFile().getParentFile(),
												"obb/" + installApplicationActivity.getPackageName() + "/main.100." + installApplicationActivity.getPackageName() + ".obb");
					Log.i(TAG, "OBB file path: " + obbFile.getAbsolutePath() + " exists " + obbFile.exists() + " length " + obbFile.length());
					if (!obbFile.exists() || obbFile.length() < 256)
						throw new IOException("Cannot find data file: " + obbFile.getAbsolutePath());
					Process proc = Runtime.getRuntime().exec(new String[] {busybox, "tar", "xJ"}, null, installApplicationActivity.getFilesDir());
					copyStreamWithProgress(obbFile.length(), new FileInputStream(obbFile), proc.getOutputStream());
					int status = proc.waitFor();
					Log.i(TAG, "Extract data file: " + obbFile.getAbsolutePath() + " extract command status " + status);
					// Clear the .obb file, we do not need it anymore
					Proc pp = new Proc(new String[] {busybox, "sh", "-c", "echo Unpacked_and_truncated > " + obbFile.getAbsolutePath()}, installApplicationActivity.getFilesDir());
					Log.i(TAG, "Truncate data file: " + obbFile.getAbsolutePath() + " status " + pp.status + " " +  Arrays.toString(pp.out));
				}
				catch (Exception ee)
				{
					final String ARCHIVE_URL = "http://sourceforge.net/projects/libsdl-android/files/ubuntu/CUPS/dist-cups-jessie.tar.xz/download";
					Log.i(TAG, "Error unpacking data from OBB: " + ee.toString());
					Log.i(TAG, "No data archive in OBB, downloading from web: " + ARCHIVE_URL);
					setInstallingNotice(installApplicationActivity.getResources().getString(R.string.downloading_web));
					URL link = new URL(ARCHIVE_URL);
					URLConnection connection = link.openConnection();

					InputStream download = new BufferedInputStream(connection.getInputStream());
					Process proc = Runtime.getRuntime().exec(new String[] {busybox, "tar", "xJ"}, null, installApplicationActivity.getFilesDir());
					copyStreamWithProgress(connection.getContentLength(), download, proc.getOutputStream());
					int status = proc.waitFor();
					Log.i(TAG, "Downloading from web: status: " + status);
				}
			}

			new Thread(new Runnable()
			{
				public void run()
				{
					String dots = "";
					while (unpacking)
					{
						setInstallingNotice(installApplicationActivity.getResources().getString(R.string.please_wait_unpack) + dots);
						dots += ".";
						try
						{
							Thread.sleep(1000);
						}
						catch (InterruptedException e)
						{
						}
					}
				}
			}).start();
			
			new Proc(new String[] {busybox, "cp", "-af", "img-" + android.os.Build.CPU_ABI + "/.", "img/"}, installApplicationActivity.getFilesDir());
			new Proc(new String[] {busybox, "rm", "-rf", "img-armeabi-v7a", "img-x86"}, installApplicationActivity.getFilesDir());
			stream = installApplicationActivity.getAssets().open("cupsd.conf");
			out = new FileOutputStream(new File(Cups.chrootPath(installApplicationActivity), "etc/cups/cupsd.conf"));
			Cups.copyStream(stream, out);
			new Proc(new String[] {busybox, "chmod", "-R", "go+rX", "usr/share/cups"}, Cups.chrootPath(installApplicationActivity));

			Log.i(TAG, "Extracting data finished");
		}
		catch(Exception e)
		{
			Log.i(TAG, "Error extracting data: " + e.toString());
			unpacking = false;
			setInstallingNotice(installApplicationActivity.getResources().getString(R.string.error_extracting) + " " + e.toString());
			return;
		}


		unpacking = false;
        installApplicationActivity.startActivity(new Intent(installApplicationActivity, MainActivityRemade.class));
		Cups.startCupsDaemon(installApplicationActivity);
        installApplicationActivity.finish();
	}

	public static void setInstallingNotice(final String str)
	{
        installApplicationActivity.runOnUiThread(new Runnable() {
            public void run() {
                installingNotice.setText(str);
            }
        });
	}

	public static void copyStreamWithProgress(long size, InputStream stream, OutputStream out) throws java.io.IOException
	{
		byte[] buf = new byte[131072];
		if (size <= 0)
			size = 129332656;
		setInstallingNotice(installApplicationActivity.getResources().getString(R.string.please_wait_unpack_progress, 0));
		int len = stream.read(buf);
		long totalLen = 0;
		while (len >= 0)
		{
			if(len > 0)
				out.write(buf, 0, len);
			totalLen += len;
			setInstallingNotice(installApplicationActivity.getResources().getString(R.string.please_wait_unpack_progress, totalLen * 100 / size));
			len = stream.read(buf);
		}
		stream.close();
		out.close();
		setInstallingNotice(installApplicationActivity.getResources().getString(R.string.please_wait_unpack_progress, 100));
	}

	static final String TAG = "CupsInstaller";
}
