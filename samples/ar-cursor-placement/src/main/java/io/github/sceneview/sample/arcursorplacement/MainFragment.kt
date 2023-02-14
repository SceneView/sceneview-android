package io.github.sceneview.sample.arcursorplacement

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Anchor
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.CursorNode
import io.github.sceneview.math.Position
import io.github.sceneview.utils.SurfaceMirrorer
import io.github.sceneview.utils.doOnApplyWindowInsets
import java.io.IOException

private const val LOG_TAG = "AudioRecordTest"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
class MainFragment : Fragment(R.layout.fragment_main) {

    lateinit var sceneView: ArSceneView
    lateinit var loadingView: View
    lateinit var actionButton: ExtendedFloatingActionButton

    lateinit var cursorNode: CursorNode
    lateinit var modelNode: ArModelNode

    var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
            actionButton.isGone = value
        }

    var fileName: String = ""
    private var recorder: MediaRecorder? = null


    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingView = view.findViewById(R.id.loadingView)
        actionButton = view.findViewById<ExtendedFloatingActionButton>(R.id.actionButton).apply {
            val bottomMargin = (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
            doOnApplyWindowInsets { systemBarsInsets ->
                (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                    systemBarsInsets.bottom + bottomMargin
            }
//            setOnClickListener { cursorNode.createAnchor()?.let { anchorOrMove(it) } }
            setOnClickListener { startRecording() }
        }

        sceneView = view.findViewById<ArSceneView?>(R.id.sceneView).apply {
            planeRenderer.isVisible = false
            // Handle a fallback in case of non AR usage. The exception contains the failure reason
            // e.g. SecurityException in case of camera permission denied
            onArSessionFailed = { _: Exception ->
                // If AR is not available or the camara permission has been denied, we add the model
                // directly to the scene for a fallback 3D only usage
                modelNode.centerModel(origin = Position(x = 0.0f, y = 0.0f, z = 0.0f))
                modelNode.scaleModel(units = 1.0f)
                sceneView.addChild(modelNode)
            }
            onTapAr = { hitResult, _ ->
                anchorOrMove(hitResult.createAnchor())
            }
        }

        cursorNode = CursorNode(context = requireContext(), lifecycle = lifecycle).apply {
            onHitResult = { node, _ ->
                if (!isLoading) {
                    actionButton.isGone = !node.isTracking
                }
            }
        }
        sceneView.addChild(cursorNode)

        isLoading = true
        modelNode = ArModelNode(
            context = requireContext(),
            lifecycle = lifecycle,
            modelGlbFileLocation = "models/spiderbot.glb",
            onLoaded = { modelInstance ->
                actionButton.text = getString(R.string.move_object)
                actionButton.setIconResource(R.drawable.ic_target)
                isLoading = false
            })
        ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_RECORD_AUDIO_PERMISSION)

    }

    fun anchorOrMove(anchor: Anchor) {
        if (!sceneView.children.contains(modelNode)) {
            sceneView.addChild(modelNode)
        }
        modelNode.anchor = anchor
    }

    fun startRecording() {
        fileName = "${context?.externalCacheDir?.absolutePath}/audiorecordtest.mp4"
        recorder = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(fileName)
            setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT)
//            setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()
            val surfaceMirrorer = SurfaceMirrorer(sceneView.lifecycle)
            surfaceMirrorer.startMirroring(this.surface)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }
    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) activity?.finish()
    }
}