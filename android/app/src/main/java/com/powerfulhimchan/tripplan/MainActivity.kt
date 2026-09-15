package com.powerfulhimchan.tripplan

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.google.firebase.messaging.FirebaseMessaging
import com.powerfulhimchan.tripplan.ui.TripApp
import com.powerfulhimchan.tripplan.ui.TripViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<TripViewModel>()
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener(viewModel::registerDevice)
        }
        setContent { TripApp(viewModel) }
    }
}

