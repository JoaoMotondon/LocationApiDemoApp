package com.motondon.locationapidemoapp.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.LocationServices

abstract class BaseFragment : Fragment(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private var listener: BaseFragmentListener? = null

    protected var googleApiClient: GoogleApiClient? = null
        private set

    // Bool to track whether the app is already resolving an error
    private var mResolvingError = false

    interface BaseFragmentListener {
        fun onConnected()
        fun onConnectionSuspended()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")

        super.onCreate(savedInstanceState)

        // We could here check whether play services is installed and up to date and prevent app to run in case of something not satisfied
        Log.i(TAG, "onCreate() - Is Google Play Services available and up to date? " + GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context))

        buildGoogleApiClient()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() - connecting...")
        googleApiClient?.connect()
    }

    override fun onStop() {
        Log.d(TAG, "onStop()")

        super.onStop()
        googleApiClient?.let {
            Log.d(TAG, "onStop() - disconnecting...")
            it.disconnect()
        }
    }

    fun addListener(listener: BaseFragmentListener) {
        this.listener = listener
    }

    /**
     * Start a manually managed connection
     *
     * See section "Manually manage Connections" on the link below:
     * - https://developers.google.com/android/guides/api-client
     *
     */
    private fun buildGoogleApiClient() {
        Log.d(TAG, "buildGoogleApiClient() - Building the client")

        if (googleApiClient == null) {
            googleApiClient = GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
        }
    }

    /**
     * Called when `mGoogleApiClient` is connected.
     */
    override fun onConnected(bundle: Bundle?) {
        Log.d(TAG, "onConnected() - Begin")

        listener?.let { it.onConnected() }
    }

    /**
     * Called when `mGoogleApiClient` is disconnected.
     */
    override fun onConnectionSuspended(i: Int) {
        when (i) {
            1 -> Log.i(TAG, "onConnectionSuspended() - Connection suspended - Cause: " + "Service disconnected")
            2 -> Log.i(TAG, "onConnectionSuspended() - Connection suspended - Cause: " + "Connection lost")
            else -> Log.i(TAG, "onConnectionSuspended() - Connection suspended - Cause: " + "Unknown")
        }

        listener?.let { it.onConnectionSuspended() }
    }

    /**
     * Called when `mGoogleApiClient` is trying to connect but failed.
     */
    override fun onConnectionFailed(result: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed() - result: " + result.toString())

        if (mResolvingError) {
            Log.i(TAG, "onConnectionFailed() - Already attempting to resolve an error. Nothing to do anymore.")
            // Already attempting to resolve an error.
            return

        }

        when {
            result.hasResolution() -> try {
                mResolvingError = true

                Log.i(TAG, "onConnectionFailed() - Trying to resolve the Connection failed error: " + result.toString())

                // Tries to resolve the connection failure by trying to restart this activity
                result.startResolutionForResult(activity, RESOLVE_CONNECTION_REQUEST_CODE)

            } catch (e: IntentSender.SendIntentException) {
                Log.e(TAG, "onConnectionFailed() - Exception while starting resolution activity", e)
                // There was an error with the resolution intent. Try again.
                googleApiClient?.connect()
            }
            else -> {
                // Display error dialog
                GoogleApiAvailability.getInstance().getErrorDialog(activity, result.errorCode, 0).show()
                mResolvingError = true
                return
            }
        }
    }

    /**
     * Handles resolution callbacks.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(TAG, "onActivityResult() - requestCode: $requestCode - resultCode: $resultCode")

        when (requestCode) {
            RESOLVE_CONNECTION_REQUEST_CODE -> {
                mResolvingError = false
                if (resultCode == RESULT_OK) {

                    googleApiClient?.let {
                        // Make sure the app is not already connected or attempting to connect
                        if (!it.isConnecting && !it.isConnected) {
                            Log.i(TAG, "onActivityResult() - resolving connection, connecting...")
                            it.connect()
                        }
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {

        private val TAG = BaseFragment::class.java.simpleName
        private const val RESOLVE_CONNECTION_REQUEST_CODE = 100
    }

}
