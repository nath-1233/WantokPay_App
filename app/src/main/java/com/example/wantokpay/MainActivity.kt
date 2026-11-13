package com.example.wantokpay

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.wantokpay.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.walletDashboardFragment,
                R.id.transfersFragment,
                R.id.merchantHubFragment,
                R.id.communityFragment,
                R.id.billsServicesFragment,
                R.id.agentNetworkFragment,
                R.id.securityAdminFragment,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.contentMain.bottomNavigation.setupWithNavController(navController)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "WantokPay Help & Quick Actions", Snackbar.LENGTH_LONG)
                .setAction("Call Support") {
                    Snackbar.make(view, "Connecting to PNG Support Hotline: +675 300 9000", Snackbar.LENGTH_LONG).show()
                }
                .setAnchorView(binding.contentMain.bottomNavigation)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
                navHostFragment.navController.navigate(R.id.securityAdminFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        return navHostFragment.navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}
