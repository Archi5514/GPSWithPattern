package com.example.gpswithpattern

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 228
    }

    private lateinit var mMap: GoogleMap
    private lateinit var currentMarker: Marker
    private var markersList: ArrayList<Marker> = arrayListOf()
    private var polylinesList: ArrayList<Polyline> = arrayListOf()
    private var addressesList: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        currentMarker = mMap.addMarker(MarkerOptions().position(LatLng(0.0, 0.0)).title("Your current position").visible(false))

        requestLocation()

        mMap.setOnMapLongClickListener {
            getAddress(it)
            addMarker(it)
            drawLine()
        }
    }

    private fun requestLocationPermissions() {
        if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
        || !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions()
        }

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_COARSE
        val provider = locationManager.getBestProvider(criteria, true) ?: return

        val lat = locationManager.getLastKnownLocation(provider)?.latitude
        val long = locationManager.getLastKnownLocation(provider)?.longitude
        getLocation(locationManager, provider, lat, long)

        locationManager.requestLocationUpdates(provider, 10000, 10f) {
            getLocation(locationManager, provider, it.latitude, it.longitude)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == PERMISSION_REQUEST_CODE) {
            if(grantResults.size == 2 &&
                (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                requestLocation()
            } else {
                Toast.makeText(this, "Give permission !", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getAddress(location: LatLng) {
        val geocoder = Geocoder(this)
        Thread {
            try {
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                addressesList.add(addresses[0].getAddressLine(0))
                textAdress.post {
                    textAdress.text = addresses[0].getAddressLine(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun addMarker(location: LatLng) {
        val title = "Dot ${markersList.size + 1}"
        val marker = mMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker()))
        markersList.add(marker)
    }

    private fun drawLine() {
        val last = markersList.size - 1
        if(last >= 1) {
            val previous = markersList[last - 1].position
            val current = markersList[last].position
            val polyline = mMap.addPolyline(PolylineOptions()
                .add(previous, current)
                .color(Color.CYAN)
                .width(10f))
            polylinesList.add(polyline)
        }
    }

    private fun getLocation(locationManager: LocationManager, provider: String, lat: Double?, long: Double?) {
        if (lat != null && long != null) {
            val position = LatLng(lat, long)
            textLatitude.text = long.toString()
            textLongitude.text = lat.toString()

            currentMarker.position = position
            currentMarker.isVisible = true

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 12.0.toFloat()))
        }
    }

    override fun onBackPressed() {
        if(markersList.size >= 2) {

            polylinesList.last().remove()
            polylinesList.removeLast()

            markersList.last().remove()
            markersList.removeLast()

            addressesList.removeLast()
            textAdress.text = addressesList.last()

        } else if(markersList.size == 1) {

            markersList.last().remove()
            markersList.removeLast()

            addressesList.removeLast()
            textAdress.text = ""

        } else {
            super.onBackPressed()
        }
    }
}