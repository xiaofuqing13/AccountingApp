package com.loveapp.accountbook

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        // 隐藏二级页面时的底部导航
        navController.addOnDestinationChangedListener { _, dest, _ ->
            bottomNav.visibility = when (dest.id) {
                R.id.nav_home, R.id.nav_account, R.id.nav_diary,
                R.id.nav_meeting, R.id.nav_settings -> android.view.View.VISIBLE
                else -> android.view.View.GONE
            }
        }
    }
}
