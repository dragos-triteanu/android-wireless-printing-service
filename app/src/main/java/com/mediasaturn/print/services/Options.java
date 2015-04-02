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

import android.content.Context;
import android.util.Log;

import java.io.*;
import android.printservice.*;

import com.mediasaturn.print.activities.AdvancedPrintOptionsActivity;

import org.cups.android.R;


public class Options
{
	public enum DoubleSided
	{
		NONE(0, null),
		SHORT(1, "sides=two-sided-short-edge"),
		LONG(2, "sides=two-sided-long-edge");

		public final int idx;
		public final String lpOption;
		public static final String name = "DoubleSided";

		DoubleSided(int idx, String str)
		{
			this.idx = idx;
			this.lpOption = str;
		}

		public static DoubleSided get(PrintJob job)
		{
			return get(job.getAdvancedIntOption(name));
		}

		public static DoubleSided get(int i)
		{
			return DoubleSided.values()[i];
			/*
			if (i == SHORT.idx)
				return SHORT;
			if (i == LONG.idx)
				return LONG;
			return NONE;
			*/
		}

		public static String[] getStrings(Context p)
		{
			return new String[]
			{
				p.getResources().getString(R.string.double_sided_none),
				p.getResources().getString(R.string.double_sided_short),
				p.getResources().getString(R.string.double_sided_long),
			};
		}
	}

	public enum MultiplePages
	{
		X1(0, null),
		X2(1, "number-up=2"),
		X4(2, "number-up=4"),
		X6(3, "number-up=6"),
		X9(4, "number-up=9"),
		X16(5, "number-up=16");

		public final int idx;
		public final String lpOption;
		public static final String name = "MultiplePages";

		MultiplePages(int idx, String str)
		{
			this.idx = idx;
			this.lpOption = str;
		}

		public static MultiplePages get(PrintJob job)
		{
			return get(job.getAdvancedIntOption(name));
		}

		public static MultiplePages get(int i)
		{
			return MultiplePages.values()[i];
			/*
			if (i == X2.idx)
				return X2;
			if (i == X4.idx)
				return X4;
			if (i == X6.idx)
				return X6;
			if (i == X9.idx)
				return X9;
			if (i == X16.idx)
				return X16;
			return X1;
			*/
		}

		public static String[] getStrings(Context p)
		{
			return new String[]
			{
				p.getResources().getString(R.string.multiple_pages_per_sheet_1),
				p.getResources().getString(R.string.multiple_pages_per_sheet_2),
				p.getResources().getString(R.string.multiple_pages_per_sheet_4),
				p.getResources().getString(R.string.multiple_pages_per_sheet_6),
				p.getResources().getString(R.string.multiple_pages_per_sheet_9),
				p.getResources().getString(R.string.multiple_pages_per_sheet_16),
			};
		}
	}

	public static final String optionsExt = ".cfg";

	public static void loadOptions(final AdvancedPrintOptionsActivity p, final String printer)
	{
		try
		{
			ObjectInputStream in = new ObjectInputStream(p.openFileInput(printer + optionsExt));
			p.doubleSided.setSelection(in.readInt());
			p.multiplePages.setSelection(in.readInt());
			in.close();
		}
		catch (Exception e)
		{
			Log.i(TAG, "Cannot load options file for printer " + printer);
			p.doubleSided.setSelection(0);
			p.multiplePages.setSelection(0);
		}
	}

	public static void saveOptions(final AdvancedPrintOptionsActivity p, final String printer)
	{
		try
		{
			ObjectOutputStream out = new ObjectOutputStream(p.openFileOutput(printer + optionsExt, p.MODE_PRIVATE));
			out.writeInt((int)p.doubleSided.getSelectedItemId());
			out.writeInt((int)p.multiplePages.getSelectedItemId());
			out.close();
		}
		catch (Exception e)
		{
			Log.i(TAG, "Cannot save options file for printer " + printer);
		}
	}

	static public final String TAG = "PrintOptions";
}
