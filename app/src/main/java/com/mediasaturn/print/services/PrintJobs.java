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

import android.os.Handler;
import android.util.Log;

import org.cups.android.R;

import java.util.*;


public class PrintJobs
{
	private static boolean destroyed = false;
	private static HashMap<android.printservice.PrintJob, String[]> trackedJobs = new HashMap<android.printservice.PrintJob, String[]>();
	private static CupsPrintService context = null;
	private static Handler mainThread = null;


	synchronized public static void init(final CupsPrintService c)
	{
		if (context != null)
			Log.e(TAG, "Error: Already initialized with context " + context + ", new context " + c);
		destroyed = false;
		context = c;
		mainThread = new Handler(context.getMainLooper());
		Log.d(TAG, "Creating jobs tracking thread");
		new Thread(new Runnable()
		{
			public void run()
			{
				trackJobsThread();
			}
		}).start();
	}

	synchronized public static void destroy()
	{
		Log.d(TAG, "Destroying jobs tracking thread");
		destroyed = true;
	}

	synchronized public static void trackJob(final android.printservice.PrintJob job)
	{
		trackedJobs.put(job, new String[] {job.getTag(), job.getInfo().getPrinterId().getLocalId()});
		Log.d(TAG, "Started tracking job: " + job.getTag() + " printer " + job.getInfo().getPrinterId().getLocalId());
	}

	synchronized public static void stopTrackingJob(final android.printservice.PrintJob job)
	{
		trackedJobs.remove(job);
	}

	private static String getJobStatus(final String[] jobInfo)
	{
		for (String s: jobInfo)
		{
			if (s.startsWith("Status:") && s.length() > "Status:".length() + 1)
				return s.substring("Status:".length() + 1);
		}
		return "";
	}

	private static boolean isJobSucceeded(final String[] jobInfo)
	{
		for (String s: jobInfo)
		{
			if (s.equals("Alerts: job-completed-successfully") || s.equals("Alerts: processing-to-stop-point"))
				return true;
		}
		return false;
	}

	synchronized private static HashMap<android.printservice.PrintJob, String[]> copyTrackedJobsArray()
	{
		// I'm to lazy to find out how to synchronize class object from static context, so I'll just create another synchronized method
		return new HashMap<android.printservice.PrintJob, String[]>(trackedJobs);
	}

	private static void trackJobsThread()
	{
		while (!destroyed)
		{
			try
			{
				Thread.sleep(10000);
			}
			catch (Exception e)
			{
			}
			// Invoking commandline tools is slow, so we will create local copy of trackedJobs, and only lock it when changing it
			HashMap<android.printservice.PrintJob, String[]> trackedCopy = copyTrackedJobsArray();
			final HashMap<String, Map<String, String[]> > activeJobs = new HashMap<String, Map<String, String[]> >();
			final HashMap<String, Map<String, String[]> > completedJobs = new HashMap<String, Map<String, String[]> >();
			for (final android.printservice.PrintJob job: trackedCopy.keySet())
			{
				final String jobName = trackedJobs.get(job)[0];
				final String printer = trackedJobs.get(job)[1];
				if (!activeJobs.containsKey(printer))
					activeJobs.put(printer, Cups.getPrintJobs(context, printer, false));
				if (activeJobs.get(printer).containsKey(jobName))
				{
					mainThread.post(new Runnable()
					{
						public void run()
						{
							String status = getJobStatus(activeJobs.get(printer).get(jobName));
							Log.d(TAG, "Print job " + jobName + " status: " + status);
							if (status.length() > 0)
								job.block(status);
							else if (!job.isStarted())
								job.start();
						}
					});
				}
				else
				{
					if (!completedJobs.containsKey(printer))
						completedJobs.put(printer, Cups.getPrintJobs(context, printer, true));
					mainThread.post(new Runnable()
					{
						public void run()
						{
							if (!completedJobs.get(printer).containsKey(jobName))
								job.fail(context.getResources().getString(R.string.error_job_disappeared));
							else if (isJobSucceeded(completedJobs.get(printer).get(jobName)))
								job.complete();
							else
							{
								String status = getJobStatus(completedJobs.get(printer).get(jobName));
								Log.d(TAG, "Print job " + jobName + " failed with status: " + status);
								job.fail(status);
							}
						}
					});
					stopTrackingJob(job);
				}
			}
		}
	}

	static public final String TAG = "PrintJobs";
}
