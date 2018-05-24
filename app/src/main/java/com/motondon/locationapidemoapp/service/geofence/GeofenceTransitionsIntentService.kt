package com.motondon.locationapidemoapp.service.geofence

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.NotificationCompat
import android.text.TextUtils
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.motondon.locationapidemoapp.R
import com.motondon.locationapidemoapp.activities.MainActivity
import com.motondon.locationapidemoapp.common.GeofenceErrorMessages
import java.util.*

class GeofenceTransitionsIntentService : IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG, "onHandleIntent() - Begin")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent.hasError()) {
            val error = GeofenceErrorMessages.getErrorString(geofencingEvent.errorCode)
            Log.e(TAG, "Got an error in the GeofenceEvent: $error")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER, Geofence.GEOFENCE_TRANSITION_EXIT -> {

                when (geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> Log.d(TAG, "onHandleIntent() - Entered in a geofence")
                }
                when (geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> Log.d(TAG, "onHandleIntent() - Left a geofence")
                }

                val triggeringGeofences = geofencingEvent.triggeringGeofences

                val geofenceTransitionDetails = getGeofaceTransitionDetails(geofenceTransition, triggeringGeofences)

                sendNotification(geofenceTransitionDetails)
            }
            else -> Log.e(TAG, "onHandleIntent() - Received an invalid transition type.")
        }

        Log.d(TAG, "onHandleIntent() - End")
    }

    private fun getGeofaceTransitionDetails(geofenceTransition: Int, triggeringGeofences: List<Geofence>): String {

        val geofenceTransitionString = getTransitionString(geofenceTransition)

        val triggeringGeofencesIdsList = ArrayList<String>()
        triggeringGeofences.forEach { geofence -> triggeringGeofencesIdsList.add(geofence.requestId) }

        val triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList)

        return "$geofenceTransitionString: $triggeringGeofencesIdsString"
    }

    private fun getTransitionString(transitionType: Int): String {
        return when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "You entered in the geofence"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "Exited from geofence"
            else -> "Unknown Transition"
        }
    }

    private fun sendNotification(notificationDetails: String) {
        Log.d(TAG, "sendNotification() - Begin")

        // Create an explicit content Intent that starts the main Activity.
        val notificationIntent = Intent(applicationContext, MainActivity::class.java)

        // Construct a task stack.
        val stackBuilder = TaskStackBuilder.create(this)

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity::class.java)

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent)

        // Get a PendingIntent containing the entire back stack.
        val notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        // Get a notification builder that's compatible with platform versions >= 4
        val builder = NotificationCompat.Builder(this)

        // Define the notification settings.
        builder.setSmallIcon(R.drawable.ic_launcher)
            // In a real app, you may want to use a library like Volley
            // to decode the Bitmap.
            .setLargeIcon(BitmapFactory.decodeResource(resources,
                    R.drawable.ic_launcher))
            .setColor(Color.RED)
            .setContentTitle(notificationDetails)
            .setContentText("Click on the notification to return to the app")
            .setContentIntent(notificationPendingIntent)

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true)

        // Get an instance of the Notification manager
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Issue the notification
        mNotificationManager.notify(0, builder.build())

        Log.d(TAG, "sendNotification() - End")
    }

    companion object {
        private val TAG = GeofenceTransitionsIntentService::class.java.simpleName
    }
}
