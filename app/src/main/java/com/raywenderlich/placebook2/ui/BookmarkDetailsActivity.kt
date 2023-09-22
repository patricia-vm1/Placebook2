package com.raywenderlich.placebook2.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.raywenderlich.placebook2.R
import com.raywenderlich.placebook2.databinding.ActivityBookmarkDetailsBinding

class BookmarkDetailsActivity : AppCompatActivity() {
    private lateinit var databinding:
            ActivityBookmarkDetailsBinding
    override fun onCreate(savedInstanceState: android.os.Bundle?)
    {
        super.onCreate(savedInstanceState)
        databinding = DataBindingUtil.setContentView(this,
            R.layout.activity_bookmark_details)
        setupToolbar()
    }
    private fun setupToolbar() {
        setSupportActionBar(databinding.toolbar)
    }
}