package io.github.sceneview.demo.update

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class InAppUpdateManager(private val activity: Activity) {

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)

    var updateState by mutableStateOf(UpdateState.IDLE)
        private set

    var downloadProgress by mutableStateOf(0f)
        private set

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                updateState = UpdateState.DOWNLOADING
                val totalBytes = state.totalBytesToDownload()
                if (totalBytes > 0) {
                    downloadProgress = state.bytesDownloaded().toFloat() / totalBytes.toFloat()
                }
            }
            InstallStatus.DOWNLOADED -> {
                updateState = UpdateState.READY_TO_INSTALL
            }
            InstallStatus.FAILED -> {
                updateState = UpdateState.IDLE
            }
            InstallStatus.INSTALLED -> {
                updateState = UpdateState.IDLE
                appUpdateManager.unregisterListener(installStateListener)
            }
            else -> {}
        }
    }

    fun checkForUpdate() {
        updateState = UpdateState.CHECKING
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    updateState = UpdateState.AVAILABLE
                    startFlexibleUpdate(info)
                } else {
                    updateState = UpdateState.UP_TO_DATE
                }
            }
            .addOnFailureListener {
                updateState = UpdateState.IDLE
            }
    }

    private fun startFlexibleUpdate(info: AppUpdateInfo) {
        appUpdateManager.registerListener(installStateListener)
        appUpdateManager.startUpdateFlow(
            info,
            activity,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        )
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    fun checkForStalledUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                updateState = UpdateState.READY_TO_INSTALL
            }
        }
    }

    enum class UpdateState {
        IDLE, CHECKING, AVAILABLE, DOWNLOADING, READY_TO_INSTALL, UP_TO_DATE
    }
}

@Composable
fun UpdateBanner(updateManager: InAppUpdateManager) {
    when (updateManager.updateState) {
        InAppUpdateManager.UpdateState.DOWNLOADING -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Downloading update… ${(updateManager.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        InAppUpdateManager.UpdateState.READY_TO_INSTALL -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Update ready",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { updateManager.completeUpdate() }) {
                        Text("Restart")
                    }
                }
            }
        }
        else -> {}
    }
}
