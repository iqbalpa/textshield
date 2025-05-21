package org.textshield.project

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.textshield.project.data.datasource.SmsDataSourceProvider

class MainActivity : ComponentActivity() {
    // Request SMS permissions at runtime
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Permissions granted, we can proceed
            setContent {
                App()
            }
        } else {
            // Handle permission denial
            // In a real app, you would show a message explaining why permissions are needed
            // For now, we'll still show the app but it won't be able to access real SMS
            setContent {
                App()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize SmsDataSourceProvider with application context
        SmsDataSourceProvider.init(applicationContext)
        
        // Check and request permissions
        checkAndRequestPermissions()
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
        
        val allPermissionsGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (allPermissionsGranted) {
            // All permissions already granted
            setContent {
                App()
            }
        } else {
            // Request permissions
            requestPermissionLauncher.launch(permissions)
        }
    }
}