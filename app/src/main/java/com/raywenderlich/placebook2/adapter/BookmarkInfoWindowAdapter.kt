package com.raywenderlich.placebook2.adapter

import android.app.Activity
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.raywenderlich.placebook2.databinding.ContentBookmarkInfoBinding
import com.raywenderlich.placebook2.ui.MapsActivity
import com.raywenderlich.placebook2.viewmodel.MapsViewModel

class BookmarkInfoWindowAdapter(val context: Activity) : GoogleMap.InfoWindowAdapter {
    private val binding = ContentBookmarkInfoBinding.inflate(context.layoutInflater)
    override fun getInfoWindow(marker: Marker): View? {
        // This function is required, but can return null if
        // not replacing the entire info window
        return null
    }
    override fun getInfoContents(marker: Marker): View? {
        binding.title.text = marker.title ?: ""
        binding.phone.text = marker.snippet ?: ""
        val imageView = binding.photo

        when (marker.tag) {
            is MapsActivity.PlaceInfo -> {
                imageView.setImageBitmap(
                    (marker.tag as MapsActivity.PlaceInfo).image)
            }
            is MapsViewModel.BookmarkView -> {
                val bookMarkView = marker.tag as MapsViewModel.BookmarkView
                imageView.setImageBitmap(bookMarkView.getImage(context))

            }
        }

        return binding.root


    } }