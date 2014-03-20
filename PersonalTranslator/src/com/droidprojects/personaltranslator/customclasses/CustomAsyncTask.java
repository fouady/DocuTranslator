package com.droidprojects.personaltranslator.customclasses;

import android.app.Activity;
import android.os.AsyncTask;

/**
 * Extension of an AsyncTask that supports the Activity destruction.
 * @author Fouad
 * @param <TParams>
 * @param <TProgress>
 * @param <TResult>
 */
public abstract class CustomAsyncTask<TParams, TProgress, TResult> extends AsyncTask<TParams, TProgress, TResult> {
    protected AsynctaskSupportedApplication mApp;	// Instance of the current application
    protected Activity mActivity;					// Activity with which the task is associated
 
    public CustomAsyncTask(Activity activity) {
        mActivity = activity;
        mApp = (AsynctaskSupportedApplication) mActivity.getApplication();
    }

    /**
     * Sets an activity associated with the task
     * @param activity
     */
    public void setActivity(Activity activity) {
        mActivity = activity;
        if (mActivity == null) {
            onActivityDetached();
        }
        else {
            onActivityAttached();
        }
    }
 
    /**
     * Override and do necessary stuff
     */
    protected void onActivityAttached() {}
 
    /**
     * Override and do necessary stuff
     */
    protected void onActivityDetached() {}
 
    @Override
    protected void onPreExecute() {
    	mApp.addTask(mActivity, this);
    }
 
    @Override
    protected void onPostExecute(TResult result) {
        mApp.removeTask(this);
    }
 
    @Override
    protected void onCancelled() {
        mApp.removeTask(this);
    }
}