package com.motondon.locationapidemoapp.service.fetchaddress

import android.app.IntentService
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.motondon.locationapidemoapp.common.Constants
import java.io.IOException
import java.util.*

class FetchAddressIntentService : IntentService(TAG) {

    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG, "onHandleIntent() - Begin")

        var errorMessage = ""

        // Get the location passed to this service through an extra.
        val location = intent?.getParcelableExtra<Location>(Constants.LOCATION_DATA_EXTRA)

        // Only keep doing this job when location is not null. This handler should be called only when Geocoder is available, but anyway
        // we double check it here.
        location?.let {
            val geocoder = Geocoder(this, Locale.getDefault())

            var addresses: List<Address>? = null

            try {
                addresses = geocoder.getFromLocation(
                    it.latitude,
                    it.longitude,
                    // In this sample, get just a single address.
                    1)

            } catch (ioException: IOException) {
                // Catch network or other I/O problems.
                errorMessage = "Service not available"
                Log.e(TAG, errorMessage, ioException)

            } catch (illegalArgumentException: IllegalArgumentException) {
                // Catch invalid latitude or longitude values.
                errorMessage = "Invalid Lat/Lng"
                Log.e(TAG, "$errorMessage. Latitude = ${it.latitude}, Longitude = ${it.longitude}", illegalArgumentException)
            }

            // Handle case where no address was found.
            if (addresses == null || addresses.isEmpty()) {
                if (errorMessage.isEmpty()) {
                    errorMessage = "No address found"
                    Log.e(TAG, errorMessage)
                }

                deliverResultToReceiver(Constants.FAILURE_RESULT, it, errorMessage)

            } else {
                val address = addresses[0]

                if (address.getAddressLine(0).isEmpty()) {
                    errorMessage = "No address line found"
                    Log.e(TAG, errorMessage)
                    deliverResultToReceiver(Constants.FAILURE_RESULT, it, errorMessage)
                    return
                }

                // According to the docs, geocoded location description may vary, so pay attention to it.
                val addressLine = address.getAddressLine(0)
                Log.i(TAG, "Address found")
                deliverResultToReceiver(Constants.SUCCESS_RESULT, it, addressLine)
            }
        }
    }

    private fun deliverResultToReceiver(resultCode: Int, location: Location, message: String) {
        Log.i(TAG, "deliverResultToReceiver() - resultCode: $resultCode - message: $message")
        // First create an intent...
        val i = Intent(Constants.FETCH_ADDRESS_BROADCAST_ACTION)

        // ...then, add just received address to the intent...
        i.putExtra(Constants.FETCH_ADDRESS_RESULT, message)
        i.putExtra(Constants.LOCATION_DATA_EXTRA, location)
        i.putExtra(Constants.FETCH_ADDRESS_RESULT_CODE, resultCode)

        // ... and finally send the broadcast message.
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(i)
    }

    companion object {
        private val TAG = FetchAddressIntentService::class.java.simpleName
    }
}
