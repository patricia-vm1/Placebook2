package com.raywenderlich.placebook2.ui

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.raywenderlich.placebook2.R
import com.raywenderlich.placebook2.adapter.BookmarkInfoWindowAdapter
import com.raywenderlich.placebook2.adapter.BookmarkListAdapter
import com.raywenderlich.placebook2.databinding.ActivityMapsBinding
import com.raywenderlich.placebook2.viewmodel.MapsViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private val mapsViewModel by viewModels<MapsViewModel>()
    private lateinit var databinding: ActivityMapsBinding
    private lateinit var bookmarkListAdapter: BookmarkListAdapter
    private var markers = HashMap<Long, Marker>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databinding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(databinding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupLocationClient()
        setupToolbar()
        setupPlacesClient()
        setupNavigationDrawer()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setupMapListeners()
        createBookmarkObserver()
        getCurrentLocation()
        }

    private fun setupMapListeners() {
        map.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
        map.setOnPoiClickListener {
            displayPoi(it)
        }
        map.setOnInfoWindowClickListener {
            handleInfoWindowClick(it)
        }
        databinding.mainMapView.fab.setOnClickListener {
            searchAtCurrentLocation()
        }
        map.setOnMapLongClickListener { latLng ->
            newBookmark(latLng)
        }
    }

    private fun setupPlacesClient() {
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)
    }

    private fun setupLocationClient() {
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION
        )
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissions()
        } else {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnCompleteListener {
                val location = it.result
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                    map.moveCamera(update)
                } else {
                    Log.e(TAG, "No location found")
                }
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Log.e(TAG, "Location permission denied")
            }
        }
    }

    private fun displayPoi(pointOfInterest: PointOfInterest) {
        displayPoiGetPlaceStep(pointOfInterest)
        showProgress()
    }

    private fun displayPoiGetPlaceStep(pointOfInterest: PointOfInterest) {
        val placeId = pointOfInterest.placeId

        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG,
            Place.Field.TYPES
        )

        val request = FetchPlaceRequest
            .builder(placeId, placeFields)
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                displayPoiGetPhotoStep(place)
            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.e(
                        TAG,
                        "Place not found" +
                                exception.message + ", " +
                                "statusCode: " + statusCode
                    )
                    hideProgress()
                }
            }
    }

    private fun displayPoiGetPhotoStep(place: Place) {
        val photoMetadata = place
            .photoMetadatas?.get(0)
        if (photoMetadata == null) {
            displayPoiDisplayStep(place, null)
            return
        }
        val photoRequest = FetchPhotoRequest
            .builder(photoMetadata)
            .setMaxWidth(resources.getDimensionPixelSize(
                R.dimen.default_image_width
            ))
            .setMaxHeight(resources.getDimensionPixelSize(
                R.dimen.default_image_height
            ))
            .build()

        placesClient.fetchPhoto(photoRequest)
            .addOnSuccessListener { fetchPhotoResponse ->
                val bitmap = fetchPhotoResponse.bitmap
                displayPoiDisplayStep(place, bitmap)
            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.e(
                        TAG,
                        "Place not found: " +
                                exception.message + ", " +
                                "statusCode: " + statusCode
                    )

                }
            hideProgress()
            }
    }

    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?) {
        hideProgress()
        val marker = map.addMarker(MarkerOptions()
            .position(place.latLng as LatLng)
            .title(place.name)
            .snippet(place.phoneNumber)
        )
        marker?.tag = PlaceInfo(place, photo)
        marker?.showInfoWindow()
         }

    private fun handleInfoWindowClick(marker: Marker) {
        when (marker.tag) {
            is PlaceInfo -> {
                val placeInfo = (marker.tag as PlaceInfo)
                if (placeInfo.place != null && placeInfo.image != null) {
                    GlobalScope.launch {
                        mapsViewModel.addBookmarkFromPlace(placeInfo.place,
                            placeInfo.image)
                    }
                }
                marker.remove();
            }
            is MapsViewModel.BookmarkView -> {
                val bookmarkView = (marker.tag as
                        MapsViewModel.BookmarkView)
                marker.hideInfoWindow()
                bookmarkView.id?.let {
                    startBookmarkDetails(it)
                }
            } }
    }

    private fun addPlaceMarker (
        bookmark: MapsViewModel.BookmarkView): Marker? {

        val marker = map.addMarker(MarkerOptions()
            .position(bookmark.location)
            .title(bookmark.name)
            .snippet(bookmark.phone)
            .icon(bookmark.categoryResourceId?.let {
                BitmapDescriptorFactory.fromResource(it)
            })
            .alpha(0.8f))
        marker!!.tag = bookmark
        bookmark.id?.let { markers.put(it, marker) }
        return marker
    }

    private fun newBookmark(latLng: LatLng) {
        GlobalScope.launch {
            val bookmarkId = mapsViewModel.addBookmark(latLng)
            bookmarkId?.let {
                startBookmarkDetails(it)
            }
        }
    }

    private fun displayAllBookmarks(
        bookmarks: List<MapsViewModel.BookmarkView>) {
        bookmarks.forEach { addPlaceMarker(it) }
    }

    private fun createBookmarkObserver() {
        mapsViewModel.getBookmarkViews()?.observe(
            this
        ) {
            map.clear()
            markers.clear()
            it?.let {
                displayAllBookmarks(it)
                bookmarkListAdapter.setBookmarkData(it)
            }
        }
    }
    private fun startBookmarkDetails(bookmarkId: Long) {
        val intent = Intent(this, BookmarkDetailsActivity::class.java)
        intent.putExtra(EXTRA_BOOKMARK_ID, bookmarkId)
        startActivity(intent)
    }

    private fun setupToolbar() {
        setSupportActionBar(databinding.mainMapView.toolbar)
        val toggle = ActionBarDrawerToggle(
            this, databinding.drawerLayout,
            databinding.mainMapView.toolbar,
            R.string.open_drawer, R.string.close_drawer)
        toggle.syncState()
    }

    private fun setupNavigationDrawer() {
        val layoutManager = LinearLayoutManager(this)
        databinding.drawerViewMaps.bookmarkRecyclerView.layoutManager = layoutManager
        bookmarkListAdapter = BookmarkListAdapter(null, this)
        databinding.drawerViewMaps.bookmarkRecyclerView.adapter = bookmarkListAdapter
    }

    private fun updateMapToLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,
            16.0f)) }

    fun moveToBookmark(bookmark: MapsViewModel.BookmarkView) {
        databinding.drawerLayout.closeDrawer(databinding.drawerViewMaps.drawerView)
        val marker = markers[bookmark.id]
        marker?.showInfoWindow()
        val location = Location("")
        location.latitude =  bookmark.location.latitude
        location.longitude = bookmark.location.longitude
        updateMapToLocation(location)
    }

    private fun searchAtCurrentLocation() {
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS,
            Place.Field.TYPES)
        val bounds =
            RectangularBounds.newInstance(map.projection.visibleRegion.latLngBounds)
        try {
            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, placeFields)
                .setLocationBias(bounds)
                .build(this)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        } catch (e: GooglePlayServicesRepairableException) {
            Toast.makeText(this, "Problems Searching",
                Toast.LENGTH_LONG).show()
        } catch (e: GooglePlayServicesNotAvailableException) {
            Toast.makeText(this, "Problems Searching. Google Play Not available",
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent? ){
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AUTOCOMPLETE_REQUEST_CODE ->
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val place = Autocomplete.getPlaceFromIntent(data)
                    val location = Location("")
                    location.latitude = place.latLng?.latitude ?: 0.0
                    location.longitude = place.latLng?.longitude ?: 0.0
                    updateMapToLocation(location)
                    showProgress()
                    displayPoiGetPhotoStep(place)
                }
        }
    }

    private fun disableUserInteraction() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
    private fun enableUserInteraction() {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun showProgress() {
        databinding.mainMapView.progressBar.visibility = ProgressBar.VISIBLE
        disableUserInteraction()
    }
    private fun hideProgress() {
        databinding.mainMapView.progressBar.visibility = ProgressBar.GONE
        enableUserInteraction()
    }

    companion object {
        const val EXTRA_BOOKMARK_ID = "com.raywenderlich.placebook2.EXTRA_BOOKMARK_ID"
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
        private const val AUTOCOMPLETE_REQUEST_CODE = 2
    }
    class PlaceInfo(val place: Place? = null, val image: Bitmap? = null)
}