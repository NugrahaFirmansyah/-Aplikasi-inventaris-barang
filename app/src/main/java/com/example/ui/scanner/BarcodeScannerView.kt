package com.example.ui.scanner

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

data class ScannedBarcode(
    val value: String,
    val format: String,
    val formatId: Int = Barcode.FORMAT_UNKNOWN,
    val timestamp: Long = System.currentTimeMillis()
)

fun getBarcodeFormatName(format: Int): String {
    return when (format) {
        Barcode.FORMAT_QR_CODE -> "QR Code"
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_CODE_93 -> "Code 93"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        Barcode.FORMAT_AZTEC -> "Aztec"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_CODABAR -> "Codabar"
        else -> "Barcode"
    }
}

/**
 * Komponen BarcodeScanner menggunakan CameraX dan Google ML Kit.
 * Menangkap kode barcode dan menampilkan hasilnya secara real-time.
 */
@Composable
fun BarcodeScanner(
    onBarcodeScanned: (ScannedBarcode) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    onManualAdjustmentClicked: (() -> Unit)? = null,
    autoConfirmDelayMs: Long? = 1000L,
    enableManualInput: Boolean = true
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var isTorchOn by remember { mutableStateOf(false) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var showManualInputDialog by remember { mutableStateOf(false) }

    // Real-time scanned barcode state
    var liveScannedBarcode by remember { mutableStateOf<ScannedBarcode?>(null) }
    var hasConfirmed by remember { mutableStateOf(false) }

    val triggerVibration = {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(70)
            }
        } catch (e: Exception) {
            Log.e("BarcodeScanner", "Vibrate error: ${e.message}")
        }
    }

    // Auto-confirm logic when a barcode is locked in real-time
    LaunchedEffect(liveScannedBarcode) {
        val current = liveScannedBarcode
        if (current != null && !hasConfirmed && autoConfirmDelayMs != null && autoConfirmDelayMs > 0) {
            delay(autoConfirmDelayMs)
            if (!hasConfirmed) {
                hasConfirmed = true
                onBarcodeScanned(current)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("barcode_scanner_component")
    ) {
        // Camera Preview + ML Kit Image Analysis
        CameraScannerView(
            cameraSelector = cameraSelector,
            onCameraBound = { boundCamera ->
                camera = boundCamera
            },
            onBarcodeFound = { barcodeValue, barcodeFormat ->
                if (!hasConfirmed) {
                    val prev = liveScannedBarcode
                    if (prev?.value != barcodeValue) {
                        triggerVibration()
                        liveScannedBarcode = ScannedBarcode(
                            value = barcodeValue,
                            format = getBarcodeFormatName(barcodeFormat),
                            formatId = barcodeFormat
                        )
                    }
                }
            }
        )

        // Viewfinder targeting HUD overlay
        ViewfinderOverlay(
            isDetected = liveScannedBarcode != null,
            modifier = Modifier.fillMaxSize()
        )

        // Top Control Bar (Close, Torch, Camera Flip)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .testTag("close_scanner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Pemindai",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(46.dp))
            }

            // Real-time Detection Status Badge
            Surface(
                color = if (liveScannedBarcode != null) Color(0xFF047857).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (liveScannedBarcode != null) Color(0xFF10B981) else Color(0xFF10B981).copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (liveScannedBarcode != null) Icons.Default.CheckCircle else Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = if (liveScannedBarcode != null) Color.White else Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (liveScannedBarcode != null) "Barcode Terdeteksi" else "Memindai Real-time...",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Torch toggle
                IconButton(
                    onClick = {
                        val next = !isTorchOn
                        camera?.cameraControl?.enableTorch(next)
                        isTorchOn = next
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .testTag("torch_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Senter",
                        tint = if (isTorchOn) Color(0xFFFBBF24) else Color.White
                    )
                }

                // Camera Switch (Front/Back)
                IconButton(
                    onClick = {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .testTag("switch_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Ganti Kamera",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Section: Real-time Barcode Card + Manual Input button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 28.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Real-Time Result Banner / Card
            AnimatedVisibility(
                visible = liveScannedBarcode != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                liveScannedBarcode?.let { barcode ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("realtime_barcode_result_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${barcode.format} • REAL-TIME",
                                        color = Color(0xFF10B981),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(barcode.value))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Salin Kode",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = barcode.value,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.testTag("realtime_scanned_barcode_text")
                                )
                            }

                            // Confirm Button
                            Button(
                                onClick = {
                                    hasConfirmed = true
                                    onBarcodeScanned(barcode)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_realtime_barcode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = "Gunakan Kode Ini",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (onManualAdjustmentClicked != null) {
                Button(
                    onClick = onManualAdjustmentClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scanner_manual_adjust_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Koreksi Stok Manual (Jika Barcode Rusak)")
                }
            }

            if (enableManualInput) {
                Button(
                    onClick = { showManualInputDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_barcode_input_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Input Barcode Manual")
                }
            }
        }

        // Manual Input Dialog
        if (showManualInputDialog) {
            ManualBarcodeDialog(
                onDismiss = { showManualInputDialog = false },
                onSubmit = { manualCode ->
                    showManualInputDialog = false
                    if (manualCode.isNotBlank()) {
                        triggerVibration()
                        val manualBarcode = ScannedBarcode(
                            value = manualCode,
                            format = "Manual Entry"
                        )
                        hasConfirmed = true
                        onBarcodeScanned(manualBarcode)
                    }
                }
            )
        }
    }
}

/**
 * Wrapper Screen yang mengimplementasikan BarcodeScanner untuk MainActivity.
 */
@Composable
fun BarcodeScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    onManualAdjustmentClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BarcodeScanner(
        onBarcodeScanned = { scanned ->
            onBarcodeScanned(scanned.value)
        },
        onClose = onClose,
        onManualAdjustmentClicked = onManualAdjustmentClicked,
        modifier = modifier
    )
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraScannerView(
    cameraSelector: CameraSelector,
    onCameraBound: (Camera) -> Unit,
    onBarcodeFound: (value: String, format: Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val rawValue = barcode.rawValue
                                    if (!rawValue.isNullOrBlank()) {
                                        onBarcodeFound(rawValue, barcode.format)
                                        break
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Log.e("Scanner", "Barcode analysis failure: ${it.message}")
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    onCameraBound(camera)
                } catch (e: Exception) {
                    Log.e("Scanner", "Camera binding failed: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ViewfinderOverlay(
    isDetected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_progress"
    )

    BoxWithConstraints(modifier = modifier) {
        val boxWidth = maxWidth * 0.78f
        val boxHeight = boxWidth * 0.68f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val left = (canvasW - boxWidth.toPx()) / 2f
            val top = (canvasH - boxHeight.toPx()) / 2.5f
            val right = left + boxWidth.toPx()
            val bottom = top + boxHeight.toPx()

            // Dark semi-transparent scrim outside viewfinder
            drawRect(
                color = Color.Black.copy(alpha = 0.58f),
                size = size
            )

            // Transparent cutout for viewfinder
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxWidth.toPx(), boxHeight.toPx()),
                blendMode = BlendMode.Clear
            )

            // Laser line inside cutout
            val laserY = top + (boxHeight.toPx() * laserProgress)
            val laserColor = if (isDetected) Color(0xFF10B981) else Color(0xFF22D3EE)
            drawLine(
                color = laserColor,
                start = Offset(left + 10.dp.toPx(), laserY),
                end = Offset(right - 10.dp.toPx(), laserY),
                strokeWidth = if (isDetected) 4.dp.toPx() else 2.5.dp.toPx()
            )
        }

        // Corner Guides with glowing feedback on real-time lock
        Box(
            modifier = Modifier
                .size(boxWidth, boxHeight)
                .align(Alignment.Center)
                .border(
                    width = if (isDetected) 3.dp else 2.dp,
                    color = if (isDetected) Color(0xFF10B981) else Color(0xFF22D3EE).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp)
                )
        )
    }
}

@Composable
private fun ManualBarcodeDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Input Barcode Manual") },
        text = {
            Column {
                Text(
                    text = "Ketik nomor barcode atau SKU barang:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Contoh: 8991234567890") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_barcode_text_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(text.trim()) },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("manual_barcode_submit_button")
            ) {
                Text("Gunakan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
