package com.raywenderlich.placebook2.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.raywenderlich.placebook2.model.Bookmark
import com.raywenderlich.placebook2.repository.BookmarkRepo
import androidx.lifecycle.map
import com.raywenderlich.placebook2.util.ImageUtils


class MapsViewModel(application: Application) :
    AndroidViewModel(application) {
    private var bookmarks: LiveData<List<BookmarkView>>? = null
    private val TAG = "MapsViewModel"
    private val bookmarkRepo: BookmarkRepo = BookmarkRepo(
        getApplication())

    private fun bookmarkToBookmarkView(bookmark: Bookmark) = BookmarkView(
        bookmark.id,
        LatLng(bookmark.latitude, bookmark.longitude),
        bookmark.name,
        bookmark.phone,
        bookmarkRepo.getCategoryResourceId(bookmark.category)
    )

    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.placeId = place.id
        bookmark.name = place.name.toString()
        bookmark.longitude = place.latLng?.longitude ?: 0.0
        bookmark.latitude = place.latLng?.latitude ?: 0.0
        bookmark.phone = place.phoneNumber.toString()
        bookmark.address = place.address.toString()
        bookmark.category = getPlaceCategory(place)

        val newId = bookmarkRepo.addBookmark(bookmark)
        image?.let { bookmark.setImage(it, getApplication()) }
        Log.i(TAG, "New bookmark $newId added to the database.")
    }

    fun addBookmark(latLng: LatLng) : Long? {
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.name = "Untitled"
        bookmark.longitude = latLng.longitude
        bookmark.latitude = latLng.latitude
        bookmark.category = "Other"
        return bookmarkRepo.addBookmark(bookmark)
    }

    data class BookmarkView(
        val id: Long? = null,
        val location: LatLng = LatLng(0.0, 0.0),
        val name: String = "",
        val phone: String = "",
        val categoryResourceId: Int? = null
    )  {
        fun getImage(context: Context) = id?.let {
            ImageUtils.loadBitmapFromFile(context,
                Bookmark.generateImageFilename(it))
        }
    }

    private fun mapBookmarksToBookmarkView () {
        bookmarks = bookmarkRepo.allBookmarks.map { repoBookmarks ->
            repoBookmarks.map { bookmark ->
                bookmarkToBookmarkView(bookmark)
            }
        }
    }
    fun getBookmarkViews() :
        LiveData<List<BookmarkView>>? {
            if (bookmarks == null) {
                mapBookmarksToBookmarkView()
        }
        return bookmarks
    }

    private fun getPlaceCategory(place: Place): String {
        var category = "Other"
        val types = place.types
        types?.let { placeTypes ->
            if (placeTypes.size > 0) {
                val placeType = placeTypes[0]
                category = bookmarkRepo.placeTypeToCategory(placeType)
            }
        }
        return category
    }



}