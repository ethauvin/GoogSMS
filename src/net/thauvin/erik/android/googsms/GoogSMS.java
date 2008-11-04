/*
 * @(#)GoogSMS.java
 *
 * Copyright (c) 2008, Erik C. Thauvin (http://erik.thauvin.net/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the authors nor the names of its contributors may be
 * used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * $Id$
 *
 */
package net.thauvin.erik.android.googsms;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.telephony.gsm.SmsManager;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * The <code>GoogSMS</code> class implements a simple front-end for Google SMS.
 * 
 * @author <a href="mailto:erik@thauvin.net">Erik C. Thauvin</a>
 * @version $Revision$, $Date$
 * @created Nov 2, 2008
 * @since 1.0
 */
public class GoogSMS extends Activity
{
	private static final int MAX_HISTORY_SIZE = 15;
	private static final int MENU_ABOUT = 0;
	private static final int MENU_PREFS = 1;
	private static final String PREFS_HISTORY = "history";

	private final List<String> mHistory = new ArrayList<String>(MAX_HISTORY_SIZE);

	private String mLocation;
	private String mSmsNumber;

	/**
	 * Adds to the history.
	 * 
	 * @param entry The entry to add.
	 */
	private void addHistory(String entry)
	{
		if (!mHistory.contains(entry))
		{
			if (mHistory.size() >= MAX_HISTORY_SIZE)
			{
				mHistory.remove(0);
			}

			mHistory.add(entry);

			final SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
			final SharedPreferences.Editor editor = settings.edit();
			editor.putString(PREFS_HISTORY, TextUtils.join(",", mHistory));
			editor.commit();
		}
	}

	/**
	 * Returns the location.
	 * 
	 * @return the location
	 */
	private String getLocation()
	{
		return mLocation;
	}

	/**
	 * Returns the SMS service number.
	 * 
	 * @return the SMS number
	 */
	private String getSmsNumber()
	{
		return mSmsNumber;
	}

	/**
	 * Returns the current version number.
	 * 
	 * @return The current version number or empty.
	 */
	private String getVersionNumber()
	{
		try
		{
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		}
		catch (final NameNotFoundException e)
		{
			return "";
		}
	}

	/**
	 * Returns whether the location has been set.
	 * 
	 * @return <code>true</code> if set, <code>false</code> otherwise.
	 */
	private boolean hasLocation()
	{
		return !TextUtils.isEmpty(mLocation);
	}

	/**
	 * Initializes the various controls.
	 */
	private void init()
	{
		loadPrefs();

		final AutoCompleteTextView queryFld = (AutoCompleteTextView) findViewById(R.id.main_query_fld);
		final Spinner typeSpin = (Spinner) findViewById(R.id.main_type_spin);
		final Button sendBtn = (Button) findViewById(R.id.main_send_btn);

		final SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
		setHistory(settings.getString(PREFS_HISTORY, ""));
		setAutoComplete(queryFld);

		final ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this, R.array.main_type_array,
				android.R.layout.simple_spinner_item);
		typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		typeSpin.setAdapter(typeAdapter);

		final ClipboardManager clip = (ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
		if (clip.hasText())
		{
			queryFld.setText(clip.getText());
		}

		final String[] hints = getResources().getStringArray(R.array.main_hint_array);
		final String[] cmds = getResources().getStringArray(R.array.main_cmd_array);

		typeSpin.setOnItemSelectedListener(new Spinner.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				queryFld.setHint(hints[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
				; // do nothing
			}
		});

		sendBtn.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View view)
			{
				final String localToken = "%l";
				final String searchToken = "%s";
				final String optSearchToken = "%%s";

				final String cmd = cmds[typeSpin.getSelectedItemPosition()];
				final String query = queryFld.getText().toString();

				final boolean isLocal = (cmd.indexOf(localToken) != -1);
				final boolean isQuery = (cmd.indexOf(searchToken) != -1);
				final boolean isOptQuery = (cmd.indexOf(optSearchToken) != -1);

				if ((!isOptQuery) && TextUtils.isEmpty(query) && isQuery)
				{
					Toast.makeText(GoogSMS.this, R.string.main_query_err_txt, Toast.LENGTH_SHORT).show();
				}
				else if (!hasLocation() && isLocal)
				{
					Toast.makeText(GoogSMS.this, R.string.main_location_err_txt, Toast.LENGTH_SHORT).show();
					launchPrefs();
				}
				else
				{
					final String s1;

					if (isLocal)
					{
						s1 = cmd.replace(localToken, getLocation());
					}
					else
					{
						s1 = cmd;
					}

					final String s2;

					if (isOptQuery)
					{
						if (TextUtils.isEmpty(query))
						{
							s2 = s1.replace(' ' + optSearchToken, "");
						}
						else
						{
							s2 = s1.replace(optSearchToken, query);
						}
					}
					else if (isQuery)
					{
						s2 = s1.replace(searchToken, query);
					}
					else
					{
						s2 = s1;
					}

					final SmsManager sms = SmsManager.getDefault();
					sms.sendTextMessage(getSmsNumber(), null, s2, null, null);
					
					addHistory(query);
					setAutoComplete(queryFld);

					Toast.makeText(GoogSMS.this, R.string.main_sms_sent_txt, Toast.LENGTH_SHORT).show();

					Log.v("SMS SENT: ", s2);
				}
			}

		});
	}

	/**
	 * Launches the preferences screen.
	 */
	private void launchPrefs()
	{
		startActivity(new Intent(this, PrefsScreen.class));
	}

	/**
	 * Loads the preferences.
	 * 
	 * @param isFirst The first call flag.
	 */
	public void loadPrefs()
	{
		final SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
		setLocation(settings.getString(getString(R.string.prefs_key_loc), ""));
		setSmsNumber(settings.getString(getString(R.string.prefs_key_sms), getString(R.string.default_google_sms)));
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		init();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_ABOUT, 0, R.string.about_menu_txt).setIcon(android.R.drawable.ic_menu_info_details);
		menu.add(0, MENU_PREFS, 0, R.string.prefs_menu_txt).setIcon(android.R.drawable.ic_menu_preferences);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == MENU_ABOUT)
		{
			final LayoutInflater factory = LayoutInflater.from(this);
			final View aboutView = factory.inflate(R.layout.about, null);

			new AlertDialog.Builder(this).setView(aboutView).setIcon(android.R.drawable.ic_dialog_info).setTitle(
					getString(R.string.app_name) + ' ' + getVersionNumber()).setPositiveButton(R.string.alert_dialog_ok,
					new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int whichButton)
						{
							// do nothing
						}
					}).show();

			return true;
		}
		else if (item.getItemId() == MENU_PREFS)
		{
			launchPrefs();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		loadPrefs();
	}

	/**
	 * Sets the auto-complete values of the specified field.
	 * 
	 * @param field The field to the auto-complete for.
	 */
	private void setAutoComplete(AutoCompleteTextView field)
	{
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, mHistory);
		field.setAdapter(adapter);
	}

	/**
	 * Sets the history.
	 * 
	 * @param history The comma-delimited history string.
	 */
	private void setHistory(String history)
	{
		final String[] entries = TextUtils.split(history, ",");
		for (final String entry : entries)
		{
			mHistory.add(entry);
		}
	}

	/**
	 * Sets the location.
	 * 
	 * @param location the location to set
	 */
	private void setLocation(String location)
	{
		mLocation = location;
	}

	/**
	 * Sets the SMS service number.
	 * 
	 * @param smsNumber the SMS number to set
	 */
	private void setSmsNumber(String smsNumber)
	{
		mSmsNumber = smsNumber;
	}
}