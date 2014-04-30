/*****
 * BugReport Dialog - based on ACRA's Crash Report Dialog  (org.acra.CrashReportDialog)
 */
package com.virtual_hotel_agent.search.controllers.activities;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.sender.GoogleFormSender;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.evature.util.Log;
import com.virtual_hotel_agent.search.R;

public class BugReportDialog extends Activity implements
		DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
	private static final String STATE_COMMENT = "comment";
	protected static final String TAG = "BugReportDialog";
	private EditText userComment;
	String mReportFileName;
	AlertDialog mDialog;


	// allow pressing "OK" without dismissing the dialog... http://stackoverflow.com/questions/6142308/android-dialog-keep-dialog-open-when-button-is-pressed
	class CustomListener implements View.OnClickListener {
	    private final Dialog dialog;
		private Context context;
	    public CustomListener(Dialog dialog, Context ctx) {
	        this.dialog = dialog;
	        this.context = ctx;
	    }
	    @Override
	    public void onClick(View v) {
	    	// clicked ok
			if (userComment.getText().toString().equals("")) {
				Toast.makeText(context, "Please enter some description of the bug", Toast.LENGTH_LONG).show();
				return;
			}
			sendBugReport();
			dialog.dismiss();
	    }
	};
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle(R.string.bugreport_dialog_title);
		dialogBuilder.setIconAttribute(android.R.attr.alertDialogIcon);
		dialogBuilder.setView(buildCustomView(savedInstanceState));
		dialogBuilder.setPositiveButton(R.string._ok, this);
		dialogBuilder.setNegativeButton(R.string.cancel, this);
		this.mDialog = dialogBuilder.create();
		this.mDialog.setCanceledOnTouchOutside(false);
		this.mDialog.setOnDismissListener(this);
		this.mDialog.show();
		
		Button theButton = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
		theButton.setOnClickListener(new CustomListener(mDialog, this));
	}

	private View buildCustomView(Bundle savedInstanceState) {
		LinearLayout root = new LinearLayout(this);
		root.setOrientation(1);
		root.setPadding(10, 10, 10, 10);
		root.setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
		root.setFocusable(true);
		root.setFocusableInTouchMode(true);

		ScrollView scroll = new ScrollView(this);
		root.addView(scroll, new LinearLayout.LayoutParams(-1, -1, 1.0F));
		LinearLayout scrollable = new LinearLayout(this);
		scrollable.setOrientation(1);
		scroll.addView(scrollable);

		TextView text = new TextView(this);
		text.setText(getText(R.string.bugreport_dialog_text));
		scrollable.addView(text);

		TextView label = new TextView(this);
		label.setText(getText(R.string.bugreport_dialog_comment_prompt));

		label.setPadding(label.getPaddingLeft(), 10, label.getPaddingRight(),
				label.getPaddingBottom());
		scrollable.addView(label, new LinearLayout.LayoutParams(-1, -2));

		this.userComment = new EditText(this);
		this.userComment.setLines(3);
		if (savedInstanceState != null) {
			String savedValue = savedInstanceState.getString(STATE_COMMENT);
			if (savedValue != null) {
				this.userComment.setText(savedValue);
			}
		}
		scrollable.addView(this.userComment);

		return root;
	}


	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_NEGATIVE) {
			// canceled
			dialog.cancel();
			return;
		}
	}

	private void sendBugReport() {
		final ErrorReporter bugReporter = ACRA.getErrorReporter();
		bugReporter.putCustomData("bug_report", "true");
		bugReporter.setReportSender(new ReportSender() {

			@Override
			public void send(CrashReportData report)
					throws ReportSenderException {
				Log.i(TAG, "Sending bug report");
				report.put(ReportField.USER_COMMENT, userComment.getText().toString());
				GoogleFormSender gfs = new GoogleFormSender("dC1fQTdXMlg0RmplcVpvVzNmZ2Q2amc6MA");
				gfs.send(report);
				Log.i(TAG, "Reseting to crash report config");
				// reset to crash reports
				bugReporter.setReportSender(new GoogleFormSender("dDk0dGxhc1B6Z05vaXh3Q0xxWnhnZlE6MQ"));
				bugReporter.putCustomData("bug_report", "false");
			}
			
		});
		
		Exception e = new Exception("Bug Report");
		bugReporter.handleException(e);
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if ((this.userComment != null) && (this.userComment.getText() != null)) {
			outState.putString(STATE_COMMENT, this.userComment.getText().toString());
		}
	}

	public void onDismiss(DialogInterface dialog) {
		finish();
	}
}
