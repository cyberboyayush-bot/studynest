package me.aysh.studynest.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
private const val LOCAL_NETWORK_PROTECTION_SDK = 37

/**
 * Android 17 (API 37) blocks traffic to private addresses, such as the local dev server at
 * 10.0.2.2, unless the user grants this runtime permission. Ask once when the app starts.
 */
@Composable
fun RequestLocalNetworkAccess() {
    if (Build.VERSION.SDK_INT < LOCAL_NETWORK_PROTECTION_SDK) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(ACCESS_LOCAL_NETWORK)
    }
}
