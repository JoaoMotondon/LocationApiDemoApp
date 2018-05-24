package com.motondon.locationapidemoapp.fragments

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.DetectedActivity
import com.motondon.locationapidemoapp.R
import com.motondon.locationapidemoapp.common.Constants
import com.motondon.locationapidemoapp.service.detectedactivities.ActivityRecognitionIntentService
import kotlinx.android.synthetic.main.fragment_activity_recognition.*
import java.util.*

class ActivityRecognitionFragment : BaseFragment() {

    private var mActivityRecognitionUpdatesStarted = false

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (intent.action == Constants.ACTIVITY_RECOGNITION_BROADCAST_ACTION) {
                Log.d(TAG, "BroadcastReceiver::onReceive() - Received PROBABLE_ACTIVITIES.")

                val detectedActivities = intent.getParcelableArrayListExtra<DetectedActivity>(Constants.PROBABLE_ACTIVITIES)
                val mostProbableActivity = intent.getParcelableExtra<DetectedActivity>(Constants.MOST_PROBABLE_ACTIVITY)

                updateViews(mostProbableActivity, detectedActivities)
            }
        }
    }

    private val activityDetectionPendingIntent: PendingIntent
        get() {
            Log.d(TAG, "getActivityDetectionPendingIntent()")

            val intent = Intent(context, ActivityRecognitionIntentService::class.java)
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView()")
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater?.inflate(R.layout.fragment_activity_recognition, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated()")
        super.onViewCreated(view, savedInstanceState)

        btnRequestRemoveActivityRecognitionUpdate.setOnClickListener({ onBtnRequestRemoveActivityRecognitionClick() })
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")

        LocalBroadcastManager.getInstance(context)
            .registerReceiver(mReceiver, IntentFilter(Constants.ACTIVITY_RECOGNITION_BROADCAST_ACTION))
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause()")

        LocalBroadcastManager.getInstance(context).unregisterReceiver(mReceiver)
    }

    private fun onBtnRequestRemoveActivityRecognitionClick() {
        Log.d(TAG, "onBtnRequestRemoveActivityRecognitionClick()")

        googleApiClient?.let {
            if (!it.isConnected) {
                Log.e(TAG, "onBtnRequestRemoveActivityRecognitionClick() - GoogleApiClient not connected")
                Toast.makeText(context, "GoogleApiClient not connected", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (mActivityRecognitionUpdatesStarted) {
            stopActivityRecognitionUpdates()
        } else {
            startActivityRecognitionUpdates()
        }
    }

    private fun startActivityRecognitionUpdates() {
        Log.d(TAG, "startActivityRecognitionUpdates()")

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
            googleApiClient,
            2000,
            activityDetectionPendingIntent
        ).setResultCallback { result ->
            if (result.status.isSuccess) {
                Log.d(TAG, "onResult() - Successfully added activity detection")
            } else {
                Log.e(TAG, "onResult() - Error adding activity detection: ${result.status.statusMessage?.let { it }}")
            }

            mActivityRecognitionUpdatesStarted = true
            btnRequestRemoveActivityRecognitionUpdate?.text = "Remove Activity Recognition Update"
        }
    }

    private fun stopActivityRecognitionUpdates() {
        Log.d(TAG, "stopActivityRecognitionUpdates()")

        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
            googleApiClient,
            activityDetectionPendingIntent)
            .setResultCallback { result ->
                if (result.status.isSuccess) {
                    Log.d(TAG, "onResult() - Successfully removed activity detection")
                } else {
                    Log.e(TAG, "onResult() - Error removing activity detection: ${result.status.statusMessage.let { it }}")
                }

                tvMostProbableActivity?.text = ""
                tvInAVehicleActivity?.text = ""
                tvOnFootActivity?.text = ""
                tvRunningActivity?.text = ""
                tvStillActivity?.text = ""
                tvTiltingActivity?.text = ""
                tvWalkingActivity?.text = ""
                tvOnBicycleActivity?.text = ""
                tvUnknownActivity?.text = ""

                mActivityRecognitionUpdatesStarted = false
                btnRequestRemoveActivityRecognitionUpdate?.text = "Request Activity Recognition Update"
            }
    }

    private fun updateViews(mostProbableActivity: DetectedActivity, detectedActivities: ArrayList<DetectedActivity>) {
        Log.d(TAG, "updateViews()")

        // First update the most probable activity.
        val mostProbable = StringBuilder("Hey. Looks like you are ")

        when (mostProbableActivity.type) {
            DetectedActivity.IN_VEHICLE -> tvMostProbableActivity?.text = mostProbable.append("in a vehicle!")
            DetectedActivity.ON_FOOT -> tvMostProbableActivity?.text = mostProbable.append("on foot!")
            DetectedActivity.RUNNING -> tvMostProbableActivity?.text = mostProbable.append("running!")
            DetectedActivity.STILL -> tvMostProbableActivity?.text = mostProbable.append("still!")
            DetectedActivity.TILTING -> tvMostProbableActivity?.text = mostProbable.append("tilting!")
            DetectedActivity.WALKING -> tvMostProbableActivity?.text = mostProbable.append("walking!")
            DetectedActivity.ON_BICYCLE -> tvMostProbableActivity?.text = mostProbable.append("on a bicycle!")
            DetectedActivity.UNKNOWN -> tvMostProbableActivity?.text = StringBuilder("Sorry. Could not detected current activity.")
            else -> Log.e(TAG, "getActivityDetectionPendingIntent() - Unidentifiable activity")
        }


        // Now, update all probable activities and their confidence
        for (detectedActivity in detectedActivities) {

            when (detectedActivity.type) {
                DetectedActivity.IN_VEHICLE -> tvInAVehicleActivity?.text = "Confidence: " + detectedActivity.confidence + "%"
                DetectedActivity.ON_FOOT -> tvOnFootActivity?.text = "Confidence: " + detectedActivity.confidence + "%"
                DetectedActivity.RUNNING -> tvRunningActivity?.text = "Confidence: " + detectedActivity.confidence + "%"
                DetectedActivity.STILL -> tvStillActivity?.text = "Confidence: " + detectedActivity.confidence + "%"
                DetectedActivity.TILTING -> tvTiltingActivity?.text = "Confidence: " + detectedActivity.confidence + "%"
                DetectedActivity.WALKING -> tvWalkingActivity?.text = "Confidence: " + detectedActivity.confidence + "%"
                DetectedActivity.ON_BICYCLE -> tvOnBicycleActivity?.text = "Confidence: " + detectedActivity.confidence + "%"
                DetectedActivity.UNKNOWN -> tvUnknownActivity?.text = "Confidence: " + detectedActivity.confidence + "%"
                else -> Log.e(TAG, "getActivityDetectionPendingIntent() - Unidentifiable activity")
            }
        }
    }

    companion object {
        private val TAG = ActivityRecognitionFragment::class.java.simpleName
    }
}
