package com.motondon.locationapidemoapp.fragments

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import com.motondon.locationapidemoapp.R
import com.motondon.locationapidemoapp.common.Constants
import com.motondon.locationapidemoapp.common.GeofenceErrorMessages
import com.motondon.locationapidemoapp.service.geofence.GeofenceTransitionsIntentService
import kotlinx.android.synthetic.main.fragment_geofences.*
import java.lang.Double.valueOf
import java.util.*

class GeofencesFragment : BaseFragment(), BaseFragment.BaseFragmentListener, com.google.android.gms.location.LocationListener {

    private var googleMap: GoogleMap? = null
    private var mCurrentLocationMarker: Marker? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView()")
        super.onCreateView(inflater, container, savedInstanceState)

        val root = inflater?.inflate(R.layout.fragment_geofences, container, false)

        addListener(this)

        return root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated()")
        super.onViewCreated(view, savedInstanceState)

        // Do not use mMapView (or any other View) before onViewCreated, since they will not be created completely. This is due to the fact we are using kotlin
        // extension that creates a class attribute for every view automatically, and they are ready to use only after the View is completed (i.e. after onViewCreated
        // method is called). Otherwise we will get a NPE!
        mMapView?.onCreate(savedInstanceState)
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
        mMapView?.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")
        super.onPause()
        mMapView?.onPause()
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

    override fun onConnected() {
        initializeMap()
    }

    override fun onConnectionSuspended() {
    }

    /**
     * In a real application maybe we might not want to suppress the missing permission warning. But to keep things simple here, we added this
     * annotation just to avoid a warning from the Android Studio.
     *
     * So, if the required permissions are not available when opening this fragment, the app will crash.
     *
     */
    @SuppressLint("MissingPermission")
    private fun initializeMap() {
        Log.d(TAG, "initializeMap()")

        try {
            MapsInitializer.initialize(activity.applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mMapView?.getMapAsync { mMap ->

            Log.d(TAG, "initializeMap::onMapReady()")

            googleMap = mMap

            var currentLocationLatLng = LatLng(-27.593500, -48.558540) // Florianopolis/Brazil
            // Get device location. Actually it will get the best and most recent location, so, this is not a good approach
            // for those apps that requires fine-grained location. If location is not available, use default value (from Florianopolis city)
            val lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
            if (lastLocation != null) {
                currentLocationLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
            }

            // Now, create a LocationRequest object and request to the Location Services to start sending location updates.
            val mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setFastestInterval(5000)
                    .setInterval(5000)
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this)

            // Set camera settings and update map
            //
            // Maybe we could store current zoom, bearing ant tilt, since user can change these settings and reuse them when
            // moving the camera.
            val target = CameraPosition.builder()
                    .target(currentLocationLatLng)
                    .bearing(0f) // Direction which the camera points at
                    .tilt(60f)
                    .zoom(20.0f)
                    .build()
            googleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(target))

            // Add a marker to the map just to initialize mCurrentLocationMarker
            mCurrentLocationMarker = googleMap?.addMarker(MarkerOptions().position(LatLng(0.0, 0.0)))

            googleMap?.let {
                // Do not show my location button
                it.isMyLocationEnabled = false

                it.uiSettings.isZoomControlsEnabled = true

                it.setOnMapLongClickListener { latLng ->
                    Log.d(TAG, "initializeMap::onMapReady::setOnMapLongClickListener()")
                    showContextMenu(latLng)
                }
            }
        }
    }

    private fun showContextMenu(target: LatLng) {
        Log.d(TAG, "showContextMenu()")

        val inflater = LayoutInflater.from(context)
        val dialogNewMaker = inflater.inflate(R.layout.context_menu, null)
        val tvCreateGeofence = dialogNewMaker.findViewById<View>(R.id.tvCreateGeofence) as TextView

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogNewMaker)

        val dialog = builder.create()

        tvCreateGeofence.setOnClickListener { _ ->
            dialog.cancel()
            showAddGeofenceDialog(target)
        }

        dialog.show()
    }

    private fun showAddGeofenceDialog(target: LatLng) {
        Log.d(TAG, "showAddGeofenceDialog()")

        val inflater = LayoutInflater.from(context)
        val dialogNewCircle = inflater.inflate(R.layout.dialog_create_geofence, null)
        val etGeofenceTitle = dialogNewCircle.findViewById<View>(R.id.etGeofenceTitle) as EditText
        val spCircleRadiusValue = dialogNewCircle.findViewById<View>(R.id.spGeofenceRadiusValue) as Spinner

        // Populate our radius spinner with values from 1 to 5
        val mapTypes = Arrays.asList("10m", "20m", "50m", "100m", "300m")
        val mapTypesAdapter = ArrayAdapter(
                context, android.R.layout.simple_spinner_item, mapTypes)
        mapTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCircleRadiusValue.adapter = mapTypesAdapter

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogNewCircle).setTitle("Create Circle")

        builder
            .setPositiveButton("Create") { _, _ ->
                val geofenceName = etGeofenceTitle.text.toString()
                var radius = spCircleRadiusValue.selectedItem as String
                radius = radius.replace("m", "")

                Log.d(TAG, "showAddGeofenceDialog() - Creating a geofence. Name: $geofenceName. Radius: ${radius}m. Target: $target")

                createGeofence(geofenceName, Integer.valueOf(radius), target)
            }
            .setNegativeButton("Cancel") { dialog, id -> dialog.cancel() }

        val dialog = builder.create()
        dialog.show()
    }

    private fun createGeofence(name: String, radius: Int, target: LatLng) {
        Log.d(TAG, "createGeofence()")

        // Just ensure GoogleApiClient is connected, so we can add a geofence to our map
        googleApiClient?.let {
            if (!it.isConnected) {
                Log.e(TAG, "createGeofence() - GoogleApiClient not connected")
                Toast.makeText(context, "GoogleApiClient not connected", Toast.LENGTH_SHORT).show()
                return
            }
        }

        try {

            // Create the pending intent which will handle GeofenceEvent when users enter/leave the geofence area
            val intent = Intent(context, GeofenceTransitionsIntentService::class.java)
            val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            // Create the GeofenceRequest which contains a list of geofences (in our case a list with only one geofence)
            val geofencingRequest = buildGeofencingRequest(name, radius, target)

            // Finally request the geofence creation and listen for the result.
            LocationServices.GeofencingApi.addGeofences(
                googleApiClient,
                geofencingRequest,
                pendingIntent

            ).setResultCallback { status ->

                // If successful, create a circle on the map
                if (status.status.isSuccess) {
                    Log.d(TAG, "createGeofence() - Geofence added successfully ")

                    googleMap?.addCircle(CircleOptions()
                        .center(target)
                        .radius(valueOf(radius.toDouble()))
                        .strokeColor(Color.BLUE)
                        .strokeWidth(0.1.toFloat())
                        .fillColor(Color.argb(64, 0, 0, 255)))

                    Toast.makeText(context, "Geofence added successfully", Toast.LENGTH_SHORT).show()

                    // Note we should hold here the geofenceId if we want later to remove it individually. Currently it is not supported by this app.

                } else {
                    Log.e(TAG, "createGeofence() - Error while trying to add geofence. Message: ${GeofenceErrorMessages.getErrorString(status.statusCode)}")
                }
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "createGeofence() - Invalid location permission.", e)
        }

    }

    private fun buildGeofencingRequest(name: String, radius: Int, target: LatLng): GeofencingRequest {
        Log.d(TAG, "buildGeofencingRequest()")

        val builder = GeofencingRequest.Builder().apply {

            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)

            addGeofences(buildGeofence(name, radius, target))
        }

        return builder.build()
    }

    private fun buildGeofence(name: String, radius: Int, target: LatLng): List<Geofence> {
        Log.d(TAG, "buildGeofence()")

        val geofence = Geofence.Builder()

            .setRequestId(name)

            .setCircularRegion(
                target.latitude,
                target.longitude,
                radius.toFloat()

            ).setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)

            .build()

        return  ArrayList<Geofence>().apply {
            add(geofence)
        }
    }

    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "onLocationChanged() - Last location -> lat: ${location.latitude} - lng: ${location.longitude}")

        moveMarker(LatLng(location.latitude, location.longitude))
    }

    private fun moveMarker(latLng: LatLng) {
        Log.d(TAG, "moveMarker() - Lat: ${latLng.latitude} - Lng: ${latLng.longitude}")

        // Move marker to the new position
        mCurrentLocationMarker?.position = latLng

        // If new position is outside visible region, move camera to it.
        googleMap?.let {
            val currentScreen = it.projection.visibleRegion.latLngBounds
            if (!currentScreen.contains(mCurrentLocationMarker?.position)) {
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

        private val TAG = GeofencesFragment::class.java.simpleName
    }
}
