package com.robert.maps.applib.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Process;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

public class CrashReportHandler implements UncaughtExceptionHandler {
	private Activity m_context;
	private String mLogFileName;

	public static void attach(Activity context) {
		Thread.setDefaultUncaughtExceptionHandler(
			new CrashReportHandler(context)
		);
	}

	///////////////////////////////////////////// implementation

	private CrashReportHandler(Activity context) {
		m_context=context;
		mLogFileName = Ut.getRMapsMainDir(context, "")+"/log.txt";
	}

	public void uncaughtException(Thread thread, Throwable exception) {
		StringWriter stackTrace=new StringWriter();
		exception.printStackTrace(new PrintWriter(stackTrace));

		SharedPreferences uiState = m_context.getPreferences(0);
		SharedPreferences.Editor editor = uiState.edit();
		editor.putString("error", stackTrace.toString());
		editor.commit();
		
		Ut.appendLog(mLogFileName, String.format("%tc", System.currentTimeMillis()));
		Ut.appendLog(mLogFileName, stackTrace.toString());

		// from RuntimeInit.crash()
		Process.killProcess(Process.myPid());
		System.exit(10);
	}

}
