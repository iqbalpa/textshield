package org.textshield.project

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.textshield.project.data.datasource.SmsDataSourceProvider
import org.textshield.project.domain.detector.SpamDetectorProvider

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private val REQUEST_DEFAULT_SMS_APP = 1001
    
    // Request SMS permissions at runtime
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Permissions result: $permissions")
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Permissions granted, now check if we're default SMS app
            checkIfDefaultSmsApp()
        } else {
            // Log which permissions were denied
            permissions.forEach { (permission, granted) ->
                if (!granted) {
                    Log.w(TAG, "Permission denied: $permission")
                }
            }
            
            // Handle permission denial
            Toast.makeText(
                this,
                "SMS permissions are required for full functionality",
                Toast.LENGTH_LONG
            ).show()
            
            // Still show the app but it won't be able to access real SMS
            setContent {
                App()
            }
        }
    }
    
    // For Android 11+ (API 30+), we need to use the Role API to request default SMS app status
    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Role request result: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Successfully set as default SMS app via Role API")
            // Now we can fully initialize the app
            initializeApp()
        } else {
            Log.d(TAG, "User declined to set app as default SMS app")
            // Show a message explaining why being the default SMS app is needed
            Toast.makeText(
                this,
                "TextShield needs to be the default SMS app to delete messages",
                Toast.LENGTH_LONG
            ).show()
            
            // Start app with limited functionality
            setContent {
                App()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "onCreate")
        
        try {
            // Initialize SmsDataSourceProvider with application context
            SmsDataSourceProvider.init(applicationContext)
            
            // Initialize SpamDetectorProvider with application context
            SpamDetectorProvider.init(applicationContext)
            
            // Check and request permissions
            checkAndRequestPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            // Show the app even if there was an error
            setContent {
                App()
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS
        )
        
        // Check which permissions we need to request
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        Log.d(TAG, "Permissions to request: ${permissionsToRequest.joinToString()}")
        
        if (permissionsToRequest.isEmpty()) {
            Log.d(TAG, "All permissions already granted")
            // All permissions already granted, check if default SMS app
            checkIfDefaultSmsApp()
        } else {
            // Request permissions
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }
    
    private fun checkIfDefaultSmsApp() {
        val isDefault = isDefaultSmsApp()
        Log.d(TAG, "Is default SMS app? $isDefault")
        
        if (isDefault) {
            // We're already the default SMS app, initialize fully
            initializeApp()
        } else {
            // Request to become the default SMS app
            requestDefaultSmsApp()
        }
    }
    
    private fun isDefaultSmsApp(): Boolean {
        val defaultApp = Telephony.Sms.getDefaultSmsPackage(this)
        val isDefault = packageName == defaultApp
        Log.d(TAG, "Current default SMS app: $defaultApp, our package: $packageName, isDefault: $isDefault")
        return isDefault
    }
    
    private fun requestDefaultSmsApp() {
        try {
            // Different approaches for different Android versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+ (API 29+), use the Role Manager
                Log.d(TAG, "Using Role API to request default SMS app status (Android 10+)")
                val roleManager = getSystemService(RoleManager::class.java)
                
                if (roleManager != null) {
                    val isRoleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
                    val isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_SMS)
                    
                    Log.d(TAG, "SMS role available: $isRoleAvailable, held: $isRoleHeld")
                    
                    if (isRoleAvailable && !isRoleHeld) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                        requestRoleLauncher.launch(intent)
                    } else if (isRoleHeld) {
                        // We already have the role
                        initializeApp()
                    } else {
                        // Role not available
                        Log.w(TAG, "SMS role is not available on this device")
                        Toast.makeText(
                            this,
                            "This device doesn't support changing the default SMS app",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // Start app with limited functionality
                        setContent {
                            App()
                        }
                    }
                } else {
                    Log.e(TAG, "Could not get RoleManager service")
                    // Fallback to older method
                    requestDefaultSmsAppLegacy()
                }
            } else {
                // For older Android versions
                requestDefaultSmsAppLegacy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting default SMS app: ${e.message}", e)
            // Start app with limited functionality
            setContent {
                App()
            }
        }
    }
    
    private fun requestDefaultSmsAppLegacy() {
        Log.d(TAG, "Using legacy method to request default SMS app status")
        try {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            startActivityForResult(intent, REQUEST_DEFAULT_SMS_APP)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching default SMS app change intent: ${e.message}", e)
            // Show the app with limited functionality
            setContent {
                App()
            }
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        
        if (requestCode == REQUEST_DEFAULT_SMS_APP) {
            // Check if we're now the default SMS app
            if (isDefaultSmsApp()) {
                Log.d(TAG, "Successfully set as default SMS app")
                initializeApp()
            } else {
                Log.d(TAG, "Failed to set as default SMS app")
                Toast.makeText(
                    this,
                    "TextShield needs to be the default SMS app to delete messages",
                    Toast.LENGTH_LONG
                ).show()
                
                // Start app with limited functionality
                setContent {
                    App()
                }
            }
        }
    }
    
    private fun initializeApp() {
        Log.d(TAG, "Initializing app with full SMS functionality")
        // Start the app with full SMS functionality
        setContent {
            App()
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - Checking if default SMS app status has changed")
        
        // Every time the app resumes, check if the default SMS app status has changed
        val isDefault = isDefaultSmsApp()
        Log.d(TAG, "Current default SMS app status: $isDefault")
    }
}