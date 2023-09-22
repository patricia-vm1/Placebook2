package com.raywenderlich.placebook2.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.raywenderlich.placebook2.model.Bookmark
import com.raywenderlich.placebook2.repository.BookmarkRepo
import com.raywenderlich.placebook2.util.ImageUtils

class BookmarkDetailsViewModel (application: Application) : AndroidViewModel(application) {
    private val bookmarkRepo = BookmarkRepo(getApplication())
    private var bookmarkDetailsView: LiveData<BookmarkDetailsView>? = null

    data class BookmarkDetailsView(
        var id: Long? = null,
        var name: String = "",
        var phone: String = "",
        var address: String = "",
        var notes: String = ""
    ) {
        fun getImage(context: Context) = id?.let {
            ImageUtils.loadBitmapFromFile(
                context,
                Bookmark.generateImageFilename(it)
            )
        }
    }

    private fun bookmarkToBookmarkView (bookmark: Bookmark) : BookmarkDetailsView {
        return BookmarkDetailsView (
            bookmark.id,
            bookmark.name,
            bookmark.phone,
            bookmark.address,
            bookmark.notes
        )
    }


}
