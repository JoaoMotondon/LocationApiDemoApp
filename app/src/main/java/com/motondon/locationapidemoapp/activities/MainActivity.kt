package com.motondon.locationapidemoapp.activities

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import com.motondon.locationapidemoapp.R
import com.motondon.locationapidemoapp.adapter.ViewPagerAdapter
import com.motondon.locationapidemoapp.fragments.ActivityRecognitionFragment
import com.motondon.locationapidemoapp.fragments.GeofencesFragment
import com.motondon.locationapidemoapp.fragments.LocationRequestFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        viewPager?.let { setupViewPager(it) }

        tabLayout?.setupWithViewPager(viewPager)

        setupTabListener()
    }

    private fun setupTabListener() {
        tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager?.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupViewPager(viewPager: ViewPager) {

        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFrag(LocationRequestFragment(), "Location Request")
        adapter.addFrag(ActivityRecognitionFragment(), "Activity Recognition")
        adapter.addFrag(GeofencesFragment(), "Geofences")
        viewPager.adapter = adapter
    }
}
