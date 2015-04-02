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

package com.mediasaturn.print.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.content.Intent;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.print.*;
import android.printservice.*;

import com.mediasaturn.print.services.Options;
import com.mediasaturn.print.services.Proc;

import org.cups.android.R;


public class AdvancedPrintOptionsActivity extends Activity
{
	private ScrollView scroll = null;
	private LinearLayout layout = null;
	private Button close = null;
	public Spinner doubleSided = null;
	public Spinner multiplePages = null;

	private PrintJobInfo.Builder jobInfo;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");


		PrintJobInfo oldJobInfo = (PrintJobInfo) getIntent().getParcelableExtra(PrintService.EXTRA_PRINT_JOB_INFO);
		if (oldJobInfo == null || oldJobInfo.getPrinterId() == null || oldJobInfo.getPrinterId().getLocalId() == null)
		{
			AdvancedPrintOptionsActivity.this.setResult(Activity.RESULT_CANCELED, new Intent());
			finish();
		}
		final String printerId = oldJobInfo.getPrinterId().getLocalId();

		jobInfo = new PrintJobInfo.Builder(oldJobInfo);

		scroll = new ScrollView(this);
		setContentView(scroll);

		layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
		layout.setPadding(10, 10, 10, 10);
		scroll.addView(layout);

		TextView text;

		text = new TextView(this);
		text.setText(getResources().getString(R.string.double_sided_printing));
		text.setTextSize(20);
		layout.addView(text);

		text = new TextView(this);
		text.setText(getResources().getString(R.string.double_sided_printing_hint));
		text.setTextSize(14);
		layout.addView(text);

		ArrayAdapter<CharSequence> adapter;

		doubleSided = new Spinner(this, Spinner.MODE_DROPDOWN);
		adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, Options.DoubleSided.getStrings(this));
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		doubleSided.setAdapter(adapter);
		doubleSided.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			public void onItemSelected(AdapterView parent, View view, int pos, long id)
			{
				Log.d(TAG, "DoubleSided: pos" + pos + " id " + id);
				jobInfo.putAdvancedOption(Options.DoubleSided.name, (int)id);
			}
			public void onNothingSelected(AdapterView parent)
			{
				Log.d(TAG, "DoubleSided: clear selection");
				jobInfo.putAdvancedOption(Options.DoubleSided.name, 0);
			}
		});
		layout.addView(doubleSided);

		text = new TextView(this);
		text.setText("");
		text.setTextSize(10);
		layout.addView(text);

		text = new TextView(this);
		text.setText(getResources().getString(R.string.multiple_pages_per_sheet));
		text.setTextSize(20);
		layout.addView(text);

		text = new TextView(this);
		text.setText(getResources().getString(R.string.multiple_pages_per_sheet_hint));
		text.setTextSize(14);
		layout.addView(text);

		multiplePages = new Spinner(this, Spinner.MODE_DROPDOWN);
		adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, Options.MultiplePages.getStrings(this));
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		multiplePages.setAdapter(adapter);
		multiplePages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			public void onItemSelected(AdapterView parent, View view, int pos, long id)
			{
				Log.d(TAG, "multiplePages: pos" + pos + " id " + id);
				jobInfo.putAdvancedOption(Options.MultiplePages.name, (int)id);
			}
			public void onNothingSelected(AdapterView parent)
			{
				Log.d(TAG, "MultiplePages: clear selection");
				jobInfo.putAdvancedOption(Options.MultiplePages.name, 0);
			}
		});
		layout.addView(multiplePages);

		text = new TextView(this);
		text.setText("");
		text.setTextSize(10);
		layout.addView(text);

		close = new Button(this);
		close.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		close.setText(getResources().getString(R.string.close));
		close.setTextSize(20);
		close.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Options.saveOptions(AdvancedPrintOptionsActivity.this, printerId);
				Intent resultIntent = new Intent();
				resultIntent.putExtra(PrintService.EXTRA_PRINT_JOB_INFO, jobInfo.build());
				AdvancedPrintOptionsActivity.this.setResult(Activity.RESULT_OK, resultIntent);
				AdvancedPrintOptionsActivity.this.finish();
			}
		});
		layout.addView(close);

		Options.loadOptions(this, printerId);
	}

	static public final String TAG = "AdvancedPrintOptionsActivity";
}
