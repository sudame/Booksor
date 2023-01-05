package net.sudame.example.booksor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Size
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.common.Barcode

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private val cameraSelector =
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        if (!isCameraPermissionGranted()) requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)
        else run()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.all { result -> result == PackageManager.PERMISSION_GRANTED }) {
            run()
        } else {
            showManualPermissionRequestDialog()
        }
    }

    /** 手動で権限を許可するように促すダイアログを表示する */
    private fun showManualPermissionRequestDialog() {
        val packageUri = Uri.fromParts("package", packageName, null)
        val openApplicationSettingIntent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = packageUri }

        AlertDialog.Builder(this)
            .apply {
                setTitle(getString(R.string.cannot_access_camera))
                setMessage(getString(R.string.please_allow_camera_access))
                setPositiveButton(getString(R.string.ok)) { _, _ ->
                    startActivity(openApplicationSettingIntent)
                }
            }
            .show()
    }

    /** カメラの権限が許可されているか否かを返す */
    private fun isCameraPermissionGranted(): Boolean {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    /** カメラの状態が変化したときに呼び出されるコールバック */
    private fun onCameraStateChanged(code: Barcode?) {
        val openWebPageButton = findViewById<Button>(R.id.open_url_button)

        if (code == null) {
            openWebPageButton.isEnabled = false
        } else {
            openWebPageButton.isEnabled = true
            val url = Uri.parse("$BOOKS_OR_JP_URL_BASE/${code.displayValue}")
            val intent = Intent(Intent.ACTION_VIEW, url)
            openWebPageButton.setOnClickListener { startActivity(intent) }
        }
    }

    /** カメラを起動してプレビュービューとアナライザーにバインドする */
    private fun run() {
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                bindPreview(cameraProvider)
                bindAnalyzer(cameraProvider)
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    /** カメラをプレビュービューにバインドする */
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val previewView = findViewById<PreviewView>(R.id.preview_view)
        val preview: Preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
    }

    /** カメラをアナライザーにバインドする */
    private fun bindAnalyzer(cameraProvider: ProcessCameraProvider) {
        val imageAnalysisBuilder =
            ImageAnalysis.Builder().apply { setTargetResolution(Size(1280, 720)) }
        val imageAnalysis = imageAnalysisBuilder.build()
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(this),
            IsbnBarcodeAnalyzer(::onCameraStateChanged),
        )
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
    }

    companion object {
        private const val BOOKS_OR_JP_URL_BASE = "https://books.or.jp/book-details"
    }
}
