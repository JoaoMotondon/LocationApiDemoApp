package com.motondon.locationapidemoapp.common

object Constants {

    ////////////////////////////////////////////////////////////////////////////////////
    // Activities Recognition constants
    const val ACTIVITY_RECOGNITION_BROADCAST_ACTION = "ACTIVITY_RECOGNITION_BROADCAST_ACTION"
    const val PROBABLE_ACTIVITIES = "PROBABLE_ACTIVITIES"
    const val MOST_PROBABLE_ACTIVITY = "MOST_PROBABLE_ACTIVITY"
    // End of Activities Recognition constants
    ////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////
    // Geofences constants
    // Used to set an expiration time for a geofence. After this amount of time Location Service stops tracking the geofence.
    const val GEOFENCE_EXPIRATION_IN_HOURS: Long = 12

    // For this sample, geofences expire after twelve hours.
    const val GEOFENCE_EXPIRATION_IN_MILLISECONDS = GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000
    // End of Geofences constants
    ////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////
    // Geocoder constants
    const val SUCCESS_RESULT = 0
    const val FAILURE_RESULT = 1

    const val FETCH_ADDRESS_BROADCAST_ACTION = "FETCH_ADDRESS_BROADCAST_ACTION"
    const val LOCATION_DATA_EXTRA = "LOCATION_DATA_EXTRA"
    const val FETCH_ADDRESS_RESULT = "FETCH_ADDRESS_RESULT"
    const val FETCH_ADDRESS_RESULT_CODE = "FETCH_ADDRESS_RESULT_CODE"

    // End of Geocoder constants
    ////////////////////////////////////////////////////////////////////////////////////

}
