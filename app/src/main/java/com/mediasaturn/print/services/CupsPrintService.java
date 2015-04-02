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

import android.content.Intent;
import android.os.Handler;
import android.print.PrintAttributes;
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintService;
import android.printservice.PrinterDiscoverySession;
import android.util.Log;

import com.mediasaturn.print.activities.MainActivityRemade;
import com.mediasaturn.print.init.Installer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class CupsPrintService extends PrintService
{
	public static boolean pluginEnabled = false;

	@Override public void onCreate()
	{
		Log.d(TAG, "onCreate()");
		super.onCreate();
		pluginEnabled = true;
		PrintJobs.init(this);
		// Initialize cached paper sizes
		Cups.getMediaSize(this, "A4");
	}

	@Override public void onDestroy()
	{
		Log.d(TAG, "onDestroy()");
		super.onDestroy();
		pluginEnabled = false;
		PrintJobs.destroy();
	}

	@Override public void onConnected()
	{
		Log.d(TAG, "onConnected()");
		super.onConnected();
		if (!Installer.isInstalled(this))
		{
			Intent dialogIntent = new Intent(getBaseContext(), MainActivityRemade.class);
			dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getApplication().startActivity(dialogIntent);
		}
		else{
			Cups.startCupsDaemon(this);
			// Initialize cached printer list
			Cups.updatePrintersInfo(this);
		}
	}

	@Override public void onDisconnected(){
		Log.d(TAG, "onDisconnected()");
		super.onDisconnected();
		Cups.stopCupsDaemon(this);
	}

	class CupsPrinterDiscoverySession extends PrinterDiscoverySession implements Runnable{
		private boolean shouldExit = false;
		private HashSet<PrinterId> trackedPrinters = new HashSet<PrinterId>();
		private Semaphore sem = new Semaphore(0);
		private Handler mainThread = null;

		CupsPrinterDiscoverySession()
		{
			mainThread = new Handler(CupsPrintService.this.getMainLooper());
			new Thread(this).start();
		}

		public synchronized void onDestroy()
		{
			Log.d(TAG, "onDestroy()");
			shouldExit = true;
			sem.release();
		}
		public synchronized void onStartPrinterDiscovery(List<PrinterId> priorityList)
		{
			// TODO: cache printer info, because invoking commandline tools is slow
			Log.d(TAG, "onStartPrinterDiscovery()");
			final ArrayList<PrinterInfo> ret = new ArrayList<PrinterInfo>();
			for (String pr: Cups.getPrinters(CupsPrintService.this))
			{
				PrinterId id = generatePrinterId(pr);
				ret.add(getPrinterInfoFull(id));
			}
			addPrinters(ret);
		}
		public synchronized void onStopPrinterDiscovery()
		{
			Log.d(TAG, "onStopPrinterDiscovery()");
		}
		public synchronized void onStartPrinterStateTracking(PrinterId id)
		{
			Log.d(TAG, "onStartPrinterTracking(): " + id.getLocalId());
			trackedPrinters.add(id);
			ArrayList<PrinterInfo> ret = new ArrayList<PrinterInfo>();
			ret.add(getPrinterInfoFull(id));
			addPrinters(ret);
			sem.release();
		}
		public synchronized void onStopPrinterStateTracking(PrinterId id)
		{
			Log.d(TAG, "onStopPrinterStateTracking(): " + id.getLocalId());
			trackedPrinters.remove(id);
		}
		public synchronized void onValidatePrinters(List<PrinterId> printerIds)
		{
			Log.d(TAG, "onValidatePrinters()");
			ArrayList<PrinterInfo> ret = new ArrayList<PrinterInfo>();
			for (PrinterId id: printerIds)
			{
				Log.d(TAG, "onValidatePrinters(): " + id.getLocalId());
				ret.add(getPrinterInfoFull(id));
			}
			addPrinters(ret);
			Log.d(TAG, "onValidatePrinters(): exit");
		}

		public void run()
		{
			while (!shouldExit)
			{
				try
				{
					sem.tryAcquire(8, TimeUnit.SECONDS);
				}
				catch(Exception e)
				{
				}
				synchronized(this)
				{
					if (shouldExit)
						return;
				}
				Cups.updatePrintersInfo(CupsPrintService.this);
				synchronized(this)
				{
					if (shouldExit)
						return;
					if (!trackedPrinters.isEmpty())
					{
						final ArrayList<PrinterInfo> ret = new ArrayList<PrinterInfo>();
						for (PrinterId id: trackedPrinters)
						{
							ret.add(getPrinterInfoFull(id));
						}
						Log.d(TAG, "onStartPrinterTracking(): finishing from discover thread");
						mainThread.post(new Runnable()
						{
							public void run()
							{
								addPrinters(ret);
							}
						});
					}
				}
			}
		}

		private PrinterInfo getPrinterInfoBasic(PrinterId id)
		{
			return getPrinterInfo(id, false);
		}
		private PrinterInfo getPrinterInfoFull(PrinterId id)
		{
			return getPrinterInfo(id, true);
		}
		private PrinterInfo getPrinterInfo(PrinterId id, boolean updateCaps)
		{
			String pr = id.getLocalId();
			PrinterInfo.Builder pi = new PrinterInfo.Builder(id, pr, Cups.getPrinterStatus(CupsPrintService.this, pr));
			pi.setDescription("");
			pi.setName(pr);
			if (!updateCaps)
				return pi.build();
			PrinterCapabilitiesInfo.Builder pc = new PrinterCapabilitiesInfo.Builder(id);
			Map<String, String[]> options = Cups.getPrinterOptions(CupsPrintService.this, pr);
			boolean hasPageSize = false;
			if (options.containsKey("PageSize"))
			{
				String pagesize[] = options.get("PageSize");
				if (pagesize.length > 0 && Cups.getMediaSize(CupsPrintService.this, pagesize[0]) != null)
				{
					pc.addMediaSize(Cups.getMediaSize(CupsPrintService.this, pagesize[0]), true);
					hasPageSize = true;
				}
				for (int i = 1; i < pagesize.length; i++)
					if (Cups.getMediaSize(CupsPrintService.this, pagesize[i]) != null)
						pc.addMediaSize(Cups.getMediaSize(CupsPrintService.this, pagesize[i]), false);
			}
			if (!hasPageSize)
			{
				// Just so it won't crash
				pc.addMediaSize(PrintAttributes.MediaSize.ISO_A4, true);
				pc.addMediaSize(PrintAttributes.MediaSize.NA_LETTER, false);
			}
			boolean hasResolution = false;
			if (options.containsKey("Resolution"))
			{
				String res[] = options.get("Resolution");
				if (res.length > 0)
				{
					pc.addResolution(Cups.getResolution(res[0]), true);
					hasResolution = true;
				}
				for (int i = 1; i < res.length; i++)
					pc.addResolution(Cups.getResolution(res[i]), false);
			}
			if (!hasResolution)
			{
				// Just so it won't crash
				pc.addResolution(new PrintAttributes.Resolution("Default", "Default", 300, 300), true);
			}
			pc.setColorModes(PrintAttributes.COLOR_MODE_COLOR | PrintAttributes.COLOR_MODE_MONOCHROME, PrintAttributes.COLOR_MODE_COLOR);
			pc.setMinMargins(PrintAttributes.Margins.NO_MARGINS);
			pi.setCapabilities(pc.build());
			return pi.build();
		}

		public static final String TAG = "CupsPrinterDiscoverySession";
	}

	@Override public PrinterDiscoverySession onCreatePrinterDiscoverySession()
	{
		Log.d(TAG, "onCreatePrinterDiscoverySession()");
		return new CupsPrinterDiscoverySession();
	}

	@Override public void onPrintJobQueued(android.printservice.PrintJob job)
	{
		Log.d(TAG, "=============== onPrintJobQueued() ===============");
		Map<String, String[]> options = Cups.getPrinterOptions(this, job.getInfo().getPrinterId().getLocalId());
		HashSet<String> pageSizes = new HashSet(Arrays.asList(options.containsKey("PageSize") ? options.get("PageSize") : new String[] {"A4", "Letter"}));
		HashSet<String> resolutions = new HashSet(Arrays.asList(options.containsKey("Resolution") ? options.get("Resolution") : new String[] {}));

		boolean landscape = false;
		String mediaSize = "A4";
		if (job.getInfo().getAttributes().getMediaSize() != null)
		{
			if (pageSizes.contains(job.getInfo().getAttributes().getMediaSize().getId()))
				mediaSize = job.getInfo().getAttributes().getMediaSize().getId();
			else
				mediaSize = "Custom." + 
					Math.round(job.getInfo().getAttributes().getMediaSize().asPortrait().getWidthMils() / 1000.0 / Cups.MillimetersToInches) + "x" +
					Math.round(job.getInfo().getAttributes().getMediaSize().asPortrait().getHeightMils() / 1000.0 / Cups.MillimetersToInches) + "mm";
			landscape = !job.getInfo().getAttributes().getMediaSize().isPortrait();
		}

		if (job.isQueued() && !job.isStarted())
			job.start();

		String[] jobId = Cups.printDocument(
							this,
							job,
							job.getInfo().getPrinterId().getLocalId(),
							job.getInfo().getLabel().length() > 0 ? job.getInfo().getLabel() : "PrintJob",
							job.getInfo().getCopies(),
							mediaSize,
							landscape,
							job.getInfo().getAttributes().getResolution() != null &&
							resolutions.contains(job.getInfo().getAttributes().getResolution().getId()) ?
							job.getInfo().getAttributes().getResolution().getId() : null,
							job.getInfo().getPages() != null && job.getInfo().getPages().length > 0 &&
							job.getInfo().getPages()[0].getStart() >=0 && job.getInfo().getPages()[0].getEnd() > 0 ?
							job.getInfo().getPages() : null );

		if (jobId[0].length() > 0)
		{
			// TODO: do not complete job immediately, use getPrinterJobs() and report job actual status back to Android
			Log.d(TAG, "Printing document: job started: job ID " + jobId[0]);
			job.setTag(jobId[0]);
			PrintJobs.trackJob(job);
		}
		else
		{
			Log.d(TAG, "Printing document: job failed: " + jobId[1]);
			job.fail(jobId[1]);
		}
	}

	@Override public void onRequestCancelPrintJob(android.printservice.PrintJob job)
	{
		Log.d(TAG, "=============== onRequestCancelPrintJob() ===============");
		PrintJobs.stopTrackingJob(job);
		if (job.getTag() != null && job.getTag().length() > 0)
			Cups.cancelPrintJob(this, job.getTag());
		job.cancel();
	}

	public static final String TAG = "CupsPrintService";
}
