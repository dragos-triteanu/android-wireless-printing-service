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

package com.mediasaturn.print.services;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.EditText;
import android.text.Editable;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.content.res.Configuration;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.view.View.OnKeyListener;
import android.view.MenuItem;
import android.view.Menu;
import android.view.Gravity;
import android.text.method.TextKeyListener;
import java.util.LinkedList;
import java.io.SequenceInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Set;
import android.text.SpannedString;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import android.view.inputmethod.InputMethodManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import java.util.concurrent.Semaphore;
import android.content.pm.ActivityInfo;
import android.view.Display;
import android.text.InputType;
import android.util.Log;
import android.view.Surface;
import android.app.ProgressDialog;
import java.util.ArrayList;
import java.util.Arrays;


public class Proc
{
	public String[] out = new String[0];
	public int status = 0;

	public Proc(String cmd, File dir)
	{
		Log.d(TAG, "Exec: '" + cmd + "' inside " + dir);
		status = -1;
		try
		{
			Process p = Runtime.getRuntime().exec(cmd, null, dir);
			waitFor(p);
		}
		catch(IOException e)
		{
		}
	}

	public Proc(String[] cmd, File dir)
	{
		Log.d(TAG, "Exec: '" + Arrays.toString(cmd) + "' inside " + dir);
		status = -1;
		try
		{
			Process p = Runtime.getRuntime().exec(cmd, null, dir);
			waitFor(p);
		}
		catch(IOException e)
		{
		}
	}

	void waitFor(final Process p)
	{
		final ArrayList<String> outArr = new ArrayList<String>();

		Thread tout = new Thread(new Runnable()
		{
			public void run()
			{
				BufferedReader bout = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				try
				{
					while ((line = bout.readLine()) != null)
					{
						synchronized(outArr)
						{
							outArr.add(line);
						}
						//Log.d(TAG, "Exec: out: " + line);
					}
					bout.close();
				}
				catch(IOException e)
				{
				}
			}
		});
		tout.start();
		Thread terr = new Thread(new Runnable()
		{
			public void run()
			{
				BufferedReader berr = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				try
				{
					while((line = berr.readLine()) != null)
					{
						synchronized(outArr)
						{
							outArr.add(line);
						}
						//Log.d(TAG, "Exec: err: " + line);
					}
					berr.close();
				}
				catch(IOException e)
				{
				}
			}
		});
		terr.start();
		try
		{
			status = p.waitFor();
			tout.join();
			terr.join();
		}
		catch(InterruptedException e)
		{
		}
		out = outArr.toArray(out);
		//Log.d(TAG, "Exec: exit status: " + status);
		p.destroy();
	}

	static public final String TAG = "CupsExec";
}
