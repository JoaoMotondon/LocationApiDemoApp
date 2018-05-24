package com.motondon.locationapidemoapp.common

import com.google.android.gms.location.GeofenceStatusCodes

object GeofenceErrorMessages {

    fun getErrorString(errorCode: Int): String {
        return when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "Geofence service is not available now"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "Your app has registered too many geofences"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "You have provided too many PendingIntents to the addGeo"
            else -> "Unknown error: the Geofence service is not available now"
        }
    }
}
