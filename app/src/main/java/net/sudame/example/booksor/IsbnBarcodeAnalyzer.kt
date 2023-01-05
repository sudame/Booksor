package net.sudame.example.booksor

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class IsbnBarcodeAnalyzer(private val onResultChanged: (Barcode?) -> Unit) :
    ImageAnalysis.Analyzer {
    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()
    private var found = false

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image!!
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(image).addOnSuccessListener { results ->
            val isbnBarcode =
                results.find { result -> result.displayValue?.startsWith(ISBN_STARTS_WITH) == true }
            if (isbnBarcode != null) {
                if (!found) {
                    found = true
                    onResultChanged(isbnBarcode)
                }
            } else {
                if (found) {
                    found = false
                    onResultChanged(null)
                }
            }
            imageProxy.close()
        }
    }

    companion object {
        private const val ISBN_STARTS_WITH = "9784"
    }
}
