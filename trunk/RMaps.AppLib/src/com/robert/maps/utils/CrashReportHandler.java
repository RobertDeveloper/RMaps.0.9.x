package com.robert.maps.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Process;

public class CrashReportHandler implements UncaughtExceptionHandler {
	private Activity m_context;

	public static void attach(Activity context) {
		Thread.setDefaultUncaughtExceptionHandler(
			new CrashReportHandler(context)
		);
	}

	///////////////////////////////////////////// implementation

	private CrashReportHandler(Activity context) {
		m_context=context;
	}

	public void uncaughtException(Thread thread, Throwable exception) {
		StringWriter stackTrace=new StringWriter();
		exception.printStackTrace(new PrintWriter(stackTrace));

		SharedPreferences uiState = m_context.getPreferences(0);
		SharedPreferences.Editor editor = uiState.edit();
		editor.putString("error", stackTrace.toString());
		editor.commit();

		// from RuntimeInit.crash()
		Process.killProcess(Process.myPid());
		System.exit(10);
	}

}
