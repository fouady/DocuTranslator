package com.droidprojects.personaltranslator.customclasses;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.Application;

/**
 * This is a custom Application class that has extra capability to maintain a list
 * of Async tasks when an app goes out of context. The async task is restored
 * when the app gains the context again.
 * @author Fouad
 */
public class AsynctaskSupportedApplication extends Application{
	private Map<String, List<CustomAsyncTask<?,?,?>>> mActivityTaskMap;		// Map of the async tasks
	private String mSessionID;												// Session of the app
	 
    public AsynctaskSupportedApplication() {
        mActivityTaskMap = new HashMap<String, List<CustomAsyncTask<?,?,?>>>();
        
        // When an application is created, get a timestamp to save as session id.
        mSessionID = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }
 
    /**
     * Remove a particular async task
     * @param task
     */
    public void removeTask(CustomAsyncTask<?,?,?> task) {
    	
    	// Traverse through the whole list of group of tasks tasks
        for (Entry<String, List<CustomAsyncTask<?,?,?>>> entry : mActivityTaskMap.entrySet()) {
            List<CustomAsyncTask<?,?,?>> tasks = entry.getValue();
            // Traverse through the whole list of tasks in each group
            for (int i = 0; i < tasks.size(); i++) {
            	// remove the task from this group
                if (tasks.get(i) == task) {
                    tasks.remove(i);
                    break;
                }
            }
 
            // if no more in a group, remove that group.
            if (tasks.size() == 0) {
                mActivityTaskMap.remove(entry.getKey());
                return;
            }
        }
    }
 
    /**
     * Adds a new task entry to the tasks map.
     * @param activity
     * @param task
     */
    public void addTask(Activity activity, CustomAsyncTask<?,?,?> task) {
        String key = activity.getClass().getCanonicalName();
        List<CustomAsyncTask<?,?,?>> tasks = mActivityTaskMap.get(key);
        if (tasks == null) {
            tasks = new ArrayList<CustomAsyncTask<?,?,?>>();
            mActivityTaskMap.put(key, tasks);
        }
        tasks.add(task);
    }
 
    /**
     * Detaches a specific activity from a task when the activity is being destroyed
     * but the task is still running.
     * @param activity
     */
    public void detach(Activity activity) {
        List<CustomAsyncTask<?,?,?>> tasks = mActivityTaskMap.get(activity.getClass().getCanonicalName());
        if (tasks != null) {
            for (CustomAsyncTask<?,?,?> task : tasks) {
                task.setActivity(null);
            }
        }
    }
 
    /**
     * Attaches an activity back to the task when activity comes into context.
     * @param activity
     */
    public void attach(Activity activity) {
        List<CustomAsyncTask<?,?,?>> tasks = mActivityTaskMap.get(activity.getClass().getCanonicalName());
        if (tasks != null) {
            for (CustomAsyncTask<?,?,?> task : tasks) {
                task.setActivity(activity);
            }
        }
    }
    
	public String getSessionID() {
		return mSessionID;
	}
}
