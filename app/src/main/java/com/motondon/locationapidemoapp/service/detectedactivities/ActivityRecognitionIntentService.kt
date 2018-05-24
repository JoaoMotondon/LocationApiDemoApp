package com.motondon.locationapidemoapp.service.detectedactivities

import android.app.IntentService
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.motondon.locationapidemoapp.common.Constants
import java.util.*

class ActivityRecognitionIntentService : IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        val recognitionResult = ActivityRecognitionResult.extractResult(intent)
        val detectedActivities = recognitionResult.probableActivities as ArrayList<DetectedActivity>
        val mostProbableActivity = recognitionResult.mostProbableActivity

        Log.d(TAG, "onHandleIntent() - Received a list of probably activities")

        // First create an intent...
        val i = Intent(Constants.ACTIVITY_RECOGNITION_BROADCAST_ACTION)

        // ...then, add just received probably activities list to the intent...
        i.putExtra(Constants.PROBABLE_ACTIVITIES, detectedActivities)
        i.putExtra(Constants.MOST_PROBABLE_ACTIVITY, mostProbableActivity)

        // ... and finally send the broadcast message.
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(i)
    }

    companion object {
        private val TAG = ActivityRecognitionIntentService::class.java.simpleName
    }
}
