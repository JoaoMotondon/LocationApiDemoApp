package com.motondon.locationapidemoapp.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.*
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.motondon.locationapidemoapp.R
import com.motondon.locationapidemoapp.common.Constants
import com.motondon.locationapidemoapp.service.fetchaddress.FetchAddressIntentService
import kotlinx.android.synthetic.main.fragment_location_request.*

class LocationRequestFragment : BaseFragment(), LocationListener, BaseFragment.BaseFragmentListener {

    private var googleMap: GoogleMap? = null
    private var mMarker: Marker? = null

    private var mLastLocation: Location? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (intent.action == Constants.FETCH_ADDRESS_BROADCAST_ACTION) {
                Log.d(TAG, "BroadcastReceiver::onReceive() - Received FETCH_ADDRESS_RESULT.")

                val address = intent.getStringExtra(Constants.FETCH_ADDRESS_RESULT)
                val location = intent.getParcelableExtra<Location>(Constants.LOCATION_DATA_EXTRA)
                val resultCode = intent.getIntExtra(Constants.FETCH_ADDRESS_RESULT_CODE, Constants.FAILURE_RESULT)

                updateAddress(resultCode, location, address)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)

        // Add a listener in order to receive callback such as onConnected.
        addListener(this)

        // Create a LocationRequest object which will be used by the LocationSettingsRequest object below.
        mLocationRequest = LocationRequest().apply {

            // Set the priority of the request.
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY

            // This method sets the fastest rate in milliseconds at which your app can handle location updates.
            fastestInterval = 5000

            // This method sets the rate in milliseconds at which your app prefers to receive location updates.
            interval = 5000
        }

        mLocationRequest?.let {
            // Instantiate the LocationSettingsRequest object. It will be used after connect to the Google Play Services to start listening for
            // location updates.
            mLocationSettingsRequest = LocationSettingsRequest.Builder()

                // By setting setAlwaysShow to true, the dialog will show up if the location settings do not satisfy the request, even if a user
                // has previously chosen "Never". Instead the default Yes, Not now and Never buttons if you call setAlwaysShow(true); you will
                // have only Yes and No, so the user won't choose Never and you will never receive SETTINGS_CHANGE_UNAVAILABLE. See link below for
                // details: http://stackoverflow.com/questions/29861580/locationservices-settingsapi-reset-settings-change-unavailable-flag
                .setAlwaysShow(false)

                .addLocationRequest(it).build()
        }

    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView()")
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater?.inflate(R.layout.fragment_location_request, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated()")
        super.onViewCreated(view, savedInstanceState)

        // Do not use any View before onViewCreated, since they will not be created completely. This is due to the fact we are using kotlin extension
        // that creates a class attribute for every view automatically, and they are ready to use only after the View is completed (i.e. after onViewCreated
        // method is called). Otherwise we will get a NPE!
        initializeMap(savedInstanceState)
    }

    override fun onStop() {
        Log.d(TAG, "onStop()")
        super.onStop()

        googleApiClient?.let {
            if (it.isConnected) {
                stopListenLocation()
            }
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
        mMapView?.onResume()

        googleApiClient?.let {
            if (it.isConnected) { //&& !mRequestingLocationUpdates) {
                startListenLocation()
            }
        }

        LocalBroadcastManager.getInstance(context)
            .registerReceiver(mReceiver, IntentFilter(Constants.FETCH_ADDRESS_BROADCAST_ACTION))
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")
        super.onPause()

        mMapView?.onPause()

        LocalBroadcastManager.getInstance(context).unregisterReceiver(mReceiver)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")

        super.onDestroy()
        mMapView?.onDestroy()
    }

    override fun onLowMemory() {
        Log.d(TAG, "onLowMemory()")

        super.onLowMemory()
        mMapView?.onLowMemory()
    }

    /**
     * In a real application maybe we might not want to suppress the missing permission warning. But to keep things simple here, we added this
     * annotation only to avoid a warning from the Android Studio.
     *
     * So, if the required permissions are not available when opening this fragment, the app will crash.
     *
     */
    @SuppressLint("MissingPermission")
    private fun initializeMap(savedInstanceState: Bundle?) {
        Log.d(TAG, "initializeMap()")

        mMapView?.onCreate(savedInstanceState)

        try {
            MapsInitializer.initialize(activity.applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mMapView?.getMapAsync { mMap ->

            Log.d(TAG, "initializeMap::onMapReady()")

            googleMap = mMap

            // Set camera settings and update map
            //
            // Maybe we could store current zoom, bearing ant tilt, since user can change these settings and reuse them when
            // moving the camera.
            val target = CameraPosition.builder()
                    .target(LatLng(0.0, 0.0))
                    .bearing(0f) // Direction which the camera points at
                    .tilt(60f)
                    .zoom(20.0f)
                    .build()

            googleMap?.let {
                it.animateCamera(CameraUpdateFactory.newCameraPosition(target))

                // Add a marker to the map
                mMarker = it.addMarker(MarkerOptions().position(LatLng(0.0, 0.0)))

                // Do not show my location button
                it.isMyLocationEnabled = false

                it.uiSettings.isZoomControlsEnabled = true
            }
        }
    }

    private fun startFetchAddressIntentService() {
        Log.d(TAG, "startFetchAddressIntentService()")
        val intent = Intent(context, FetchAddressIntentService::class.java).apply {
            putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation)
        }

        activity.startService(intent)
    }

    fun updateAddress(resultCode: Int, location: Location, address: String) {
        Log.d(TAG, "updateAddress()")

        when (resultCode) {
            Constants.SUCCESS_RESULT -> {
                Log.d(TAG, "updateAddress() - Fetch address for lat: ${location.latitude} - lng: ${location.longitude}: $address")
                tvAddress?.text = address
            }
            else -> {
                Log.d(TAG, "updateAddress() - Failure while fetching address for lat: ${location.latitude} - lng: ${location.longitude}. Error: $address")
                tvAddress?.text = address
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startListenLocation() {
        Log.d(TAG, "startListenLocation()")
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this)
    }

    private fun stopListenLocation() {
        Log.d(TAG, "stopListenLocation()")
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
    }

    override fun onLocationChanged(location: Location) {
        updateLocation(location)
    }

    private fun updateLocation(location: Location) {
        Log.d(TAG, "updateLocation() - Last location -> lat: ${location.latitude} - lng: ${location.longitude}")

        mLastLocation = location

        tvLatitudeValue?.text = location.latitude.toString()
        tvLongitudeValue?.text = location.longitude.toString()

        // Determine whether Geocoder is available.
        if (!Geocoder.isPresent()) {
            Toast.makeText(context, "No Geocoder available", Toast.LENGTH_LONG).show()
            return
        }

        // If Geocoder is available request an address based on the lat/lng
        startFetchAddressIntentService()

        // And finally updated current position on the map by adding a marker to it.
        moveMarker(LatLng(location.latitude, location.longitude))
    }

    /**
     * This method is called after a dialog is presented to the user informing this app will request both FINE and COARSE permissions
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.e(TAG, "onRequestPermissionsResult() - Begin")

        when (requestCode) {
            TAG_CODE_PERMISSION_LOCATION -> if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                Log.e(TAG, "onRequestPermissionsResult() - Detected either ACCESS_FINE_LOCATION nor ACCESS_COARSE_LOCATION permissions (or both) was not granted EVEN after request it to the user.")
                Toast.makeText(context, "Detected either ACCESS_FINE_LOCATION nor ACCESS_COARSE_LOCATION permissions (or both) was not granted.", Toast.LENGTH_LONG).show()
                return
            }

            else -> {
            }
        }
    }

    /**
     * This method is called if GPS or Wifi is disabled. Then a dialog will be presented to the user in order for him/her to
     * enable such features. After that, this method is called. If user agree to enable those features, start listening for
     * location updates.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(TAG, "onActivityResult() - requestCode: $requestCode - resultCode: $resultCode")

        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> if (resultCode == RESULT_OK) {
                // All location settings are now satisfied. The client can initialize location requests here.
                Log.d(TAG, "onActivityResult() - All location settings are now satisfied. Initializing location requests...")

                startListenLocation()
            } else {
                Log.e(TAG, "onActivityResult() - REQUEST_CHECK_SETTINGS result failure. Did user deny enabling GPS/Wifi settings?")
            }

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onConnected() {
        Log.d(TAG, "onConnected")

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            /**
             * Both ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION are part of the Android 6.0 runtime permission system. In addition to
             * having them in the manifest as you do, you also have to request them from the user at runtime (using requestPermissions()) and see if you have them
             * (using checkSelfPermission()).
             *
             * See links below for details:
             * - https://developers.google.com/maps/documentation/android-api/location
             * - http://stackoverflow.com/questions/32083913/android-gps-requires-access-fine-location-error-even-though-my-manifest-file
             *
             */
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    TAG_CODE_PERMISSION_LOCATION)

            Log.e(TAG, "onConnected() - Detected either ACCESS_FINE_LOCATION nor ACCESS_COARSE_LOCATION permissions (or both) was not granted.")
            return
        }

        // Now that we are connected to the Google Play Services, it is time to check whether all required features are enabled in order to run this app accordingly.
        // If so, start listening for location updates, otherwise, depends on the status code, request system to show to the user a dialog whether he can enable such
        // features, or, if nothing can be done anymore, just log it.
        LocationServices.SettingsApi.checkLocationSettings(googleApiClient, mLocationSettingsRequest)
            .setResultCallback { locationSettingsResult ->

                val status = locationSettingsResult.status
                Log.d(TAG, "LocationSettingsResult -> status: $status")

                val locationSettingsStates = locationSettingsResult.locationSettingsStates

                when (status.statusCode) {
                        // All location settings are satisfied. The client can initialize location requests here.
                        LocationSettingsStatusCodes.SUCCESS -> {
                        Log.d(TAG, "LocationSettingsResult -> All location settings are satisfied. Get last location Initializing location requests...")

                        // Get device location. Actually it will get the best and most recent location, so, this is not a good approach
                        // for those apps that requires fine-grained location. If location is not available, mLastLocation will be null.
                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)

                        mLastLocation?.let {
                            updateLocation(it)

                        } ?: Log.e(TAG, "onConnected() - Could not get last location")

                        Log.d(TAG, "LocationSettingsResult -> Now initializing location requests...")
                        startListenLocation()
                    }

                    // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Log.w(TAG, "LocationSettingsResult -> Location settings are not satisfied. Ask user to fix it...")

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    activity,
                                    REQUEST_CHECK_SETTINGS)

                        } catch (e: IntentSender.SendIntentException) {
                            // Ignore the error.
                        }
                    }

                    // Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.
                    // Maybe user previously marked "never" option.
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> Log.e(TAG, "LocationSettingsResult -> Location settings are not satisfied and no way to fix it.")
                }
            }
    }

    override fun onConnectionSuspended() {
        Log.d(TAG, "onConnectionSuspended")

    }

    private fun moveMarker(latLng: LatLng) {
        Log.d(TAG, "moveMarker() - Lat: ${latLng.latitude} - Lng: ${latLng.longitude}")

        // Move marker to the new position
        mMarker?.position = latLng

        googleMap?.let {
            // If new position is outside visible region, move camera to it.
            val currentScreen = it.projection.visibleRegion.latLngBounds
            if (!currentScreen.contains(mMarker?.position)) {
                Log.d(TAG, "moveMarker() - Marker is out of map visible area. Moving camera...")

                val target = CameraPosition.builder()
                    .target(latLng)
                    .bearing(0f) // Direction which the camera points at
                    .tilt(60f)
                    .zoom(18.0f)
                    .build()

                it.moveCamera(CameraUpdateFactory.newCameraPosition(target))
            }
        }
    }

    companion object {

        private val TAG = LocationRequestFragment::class.java.simpleName

        private const val REQUEST_CHECK_SETTINGS = 104
        private const val TAG_CODE_PERMISSION_LOCATION = 200
    }
}
