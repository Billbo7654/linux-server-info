package com.marekguran.serverinfo

import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.marekguran.serverinfo.databinding.ActivityMainBinding
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val serviceIntent = Intent(this, TemperatureMonitoringService::class.java)
        startService(serviceIntent)
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_hw, R.id.navigation_system, R.id.navigation_settings
            )
        )
        navView.setupWithNavController(navController)

        val darkMode = DarkMode(this)

        // Check if dark mode is enabled
        if (darkMode.isDarkModeEnabled()) {
            // Set the theme for dark mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            // Set the theme for light mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // check if notification permission is granted
        if (!isNotificationPermissionGranted()) {
            // request permission
            requestNotificationPermission()
        }
        // Fetch JSON data initially
        fetchDataAndUpdateIcon(navView)

        // Schedule periodic data refresh every 5 seconds
        handler.postDelayed(object : Runnable {
            override fun run() {
                fetchDataAndUpdateIcon(navView)
                handler.postDelayed(this, 5000) // 5000 milliseconds = 5 seconds
            }
        }, 5000) // Initial delay also set to 5 seconds
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun isNotificationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission granted
                Toast.makeText(this, "Permission granted.", Toast.LENGTH_LONG).show()
            } else {
                // permission denied, ask again
                Toast.makeText(this, "If you want to receive CPU temperature warnings, enable for app notifications permission in settings.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchDataAndUpdateIcon(navView: BottomNavigationView) {
        val jsonUrl = "http://10.0.1.1:9000/system_info.json" // Adjust the URL as needed
        FetchJsonDataTask(navView).execute(jsonUrl)
    }

    private class FetchJsonDataTask(private val navView: BottomNavigationView) :
        AsyncTask<String, Void, JSONObject>() {

        override fun doInBackground(vararg params: String?): JSONObject? {
            val urlString = params[0]
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection

                try {
                    val inputStream: InputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuilder = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }

                    return JSONObject(stringBuilder.toString())
                } finally {
                    connection.disconnect()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }

        override fun onPostExecute(jsonObject: JSONObject?) {
            super.onPostExecute(jsonObject)
            jsonObject?.let {
                // Assuming you have parsed the distribution data
                val os = jsonObject.getJSONObject("os")
                val distribution = os.getString("distribution").toLowerCase()

                // Determine the drawable resource based on the distribution
                val drawableResId = when {
                    distribution.contains("ubuntu") -> R.drawable.dist_ubuntu
                    distribution.contains("debian") -> R.drawable.dist_debian
                    distribution.contains("raspbian") -> R.drawable.dist_raspbian
                    else -> R.drawable.dist_default // Default drawable if no match is found
                }

                // Find the menu item in your navigation view
                val menuItem = navView.menu.findItem(R.id.navigation_system)

                // Update the icon for the menu item
                menuItem.setIcon(drawableResId)
            }
        }

    }
}
