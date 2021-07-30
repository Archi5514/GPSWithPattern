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
    private var markers: ArrayList<Marker> = arrayListOf()
    private var polylines: ArrayList<Polyline> = arrayListOf()

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
        if(!ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        || !ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION),
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

        locationManager.requestLocationUpdates(provider, 10000, 10f) {
            val lat = it.latitude
            val long = it.longitude
            val currentPosition = LatLng(lat, long)

            textLatitude.text = long.toString()
            textLongitude.text = lat.toString()

            currentMarker.position = currentPosition
            currentMarker.isVisible = true

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 12.0.toFloat()))
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
                textAdress.post {
                    textAdress.text = addresses[0].getAddressLine(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

    }

    private fun addMarker(location: LatLng) {
        val title = "Dot ${markers.size + 1}"
        val marker = mMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker()))
        markers.add(marker)
    }

    private fun drawLine() {
        val last = markers.size - 1
        if(last >= 1) {
            val previous = markers[last - 1].position
            val current = markers[last].position
            val polyline = mMap.addPolyline(PolylineOptions()
                .add(previous, current)
                .color(Color.CYAN)
                .width(10f))
            polylines.add(polyline)
        }
    }

    override fun onBackPressed() {
        if(markers.size >= 2) {
            polylines[markers.size - 2].remove()
            polylines.removeAt(markers.size - 2)
            markers[markers.size - 1].remove()
            markers.removeAt(markers.size - 1)
        } else if(markers.size == 1) {
            markers[markers.size - 1].remove()
            markers.removeAt(markers.size - 1)
        } else {
            super.onBackPressed()
        }
    }
}