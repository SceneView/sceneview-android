@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.sceneview.demo.update

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
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
    val showBanner = updateManager.updateState == InAppUpdateManager.UpdateState.DOWNLOADING
            || updateManager.updateState == InAppUpdateManager.UpdateState.READY_TO_INSTALL

    AnimatedVisibility(
        visible = showBanner,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (updateManager.updateState) {
                    InAppUpdateManager.UpdateState.READY_TO_INSTALL ->
                        MaterialTheme.colorScheme.primaryContainer
                    else ->
                        MaterialTheme.colorScheme.secondaryContainer
                }
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (updateManager.updateState) {
                                InAppUpdateManager.UpdateState.DOWNLOADING -> "Downloading update\u2026"
                                InAppUpdateManager.UpdateState.READY_TO_INSTALL -> "Update ready!"
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (updateManager.updateState == InAppUpdateManager.UpdateState.DOWNLOADING) {
                            Text(
                                text = "${(updateManager.downloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    if (updateManager.updateState == InAppUpdateManager.UpdateState.READY_TO_INSTALL) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { updateManager.completeUpdate() },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Restart")
                        }
                    }
                }

                if (updateManager.updateState == InAppUpdateManager.UpdateState.DOWNLOADING) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { updateManager.downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
