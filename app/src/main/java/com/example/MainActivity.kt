package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.credentials.CredentialManager
import com.example.data.auth.AuthManager
import com.example.data.model.Product
import com.example.ui.auth.SignInScreen
import com.example.ui.inventory.BarcodeScanResultDialog
import com.example.ui.inventory.InventoryDashboardScreen
import com.example.ui.inventory.InventoryViewModel
import com.example.ui.inventory.ManualStockAdjustmentDialog
import com.example.ui.inventory.ProductFormDialog
import com.example.ui.inventory.QuickMutationDialog
import com.example.ui.inventory.ReportsScreen
import com.example.ui.scanner.BarcodeScannerScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = coroutineScope
            )
        }
    }

    if (currentUser == null) {
        SignInScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        MainInventoryApp(
            onSignOut = {
                AuthManager.signOut(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = {
                        currentUser = null
                    },
                    scope = coroutineScope
                )
            }
        )
    }
}

@Composable
fun MainInventoryApp(
    onSignOut: () -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableIntStateOf(0) } // 0: Dashboard, 1: Reports

    // Dialog & overlay states
    var isScannerOpen by remember { mutableStateOf(false) }
    var scannedBarcodeForDialog by remember { mutableStateOf<String?>(null) }
    var productForForm by remember { mutableStateOf<Product?>(null) }
    var prefilledBarcodeForForm by remember { mutableStateOf<String?>(null) }
    var showProductForm by remember { mutableStateOf(false) }
    var quickMutationTarget by remember { mutableStateOf<Pair<Product, String>?>(null) }
    var showManualAdjustmentDialog by remember { mutableStateOf(false) }
    var manualAdjustmentProduct by remember { mutableStateOf<Product?>(null) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isScannerOpen = true
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk memindai barcode", Toast.LENGTH_LONG).show()
        }
    }

    val launchScanner = {
        val hasCamPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCamPermission) {
            isScannerOpen = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Handle Back Press
    if (isScannerOpen) {
        BackHandler {
            isScannerOpen = false
        }
    } else if (currentTab != 0) {
        BackHandler {
            currentTab = 0
        }
    }

    if (isScannerOpen) {
        BarcodeScannerScreen(
            onBarcodeScanned = { scannedCode ->
                isScannerOpen = false
                scannedBarcodeForDialog = scannedCode
            },
            onClose = {
                isScannerOpen = false
            },
            onManualAdjustmentClicked = {
                isScannerOpen = false
                manualAdjustmentProduct = null
                showManualAdjustmentDialog = true
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Inventory, contentDescription = "Inventaris") },
                        label = { Text("Inventaris") },
                        modifier = Modifier.testTag("nav_tab_inventory")
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { launchScanner() },
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Pindai Barcode") },
                        label = { Text("Pindai") },
                        modifier = Modifier.testTag("nav_tab_scanner")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.History, contentDescription = "Riwayat Pindaian") },
                        label = { Text("Riwayat") },
                        modifier = Modifier.testTag("nav_tab_history")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Laporan Cloud") },
                        label = { Text("Laporan") },
                        modifier = Modifier.testTag("nav_tab_reports")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    0 -> InventoryDashboardScreen(
                        state = state,
                        onSearchChanged = viewModel::setSearchQuery,
                        onCategorySelected = viewModel::setSelectedCategory,
                        onStockFilterChanged = viewModel::setStockFilter,
                        onScanClicked = { launchScanner() },
                        onManualAdjustmentClicked = {
                            manualAdjustmentProduct = null
                            showManualAdjustmentDialog = true
                        },
                        onAddProductClicked = {
                            productForForm = null
                            prefilledBarcodeForForm = null
                            showProductForm = true
                        },
                        onEditProductClicked = { prod ->
                            productForForm = prod
                            prefilledBarcodeForForm = null
                            showProductForm = true
                        },
                        onMutateClicked = { prod, type ->
                            quickMutationTarget = Pair(prod, type)
                        },
                        onDeleteProductClicked = viewModel::deleteProduct,
                        onSignOutClicked = onSignOut,
                        onClearUserMessage = viewModel::clearUserMessage
                    )
                    1 -> ReportsScreen(
                        state = state,
                        onRestockProduct = { prod ->
                            quickMutationTarget = Pair(prod, "IN")
                        }
                    )
                    2 -> com.example.ui.history.ScanHistory(
                        transactions = state.transactions,
                        products = state.products,
                        isLoading = state.isLoading,
                        onQuickInbound = { prod ->
                            quickMutationTarget = Pair(prod, "IN")
                        },
                        onQuickOutbound = { prod ->
                            quickMutationTarget = Pair(prod, "OUT")
                        },
                        onScanNewClicked = { launchScanner() },
                        onItemClick = { tx ->
                            val matched = state.products.firstOrNull { it.id == tx.productId || it.barcode == tx.barcode }
                            if (matched != null) {
                                productForForm = matched
                                showProductForm = true
                            }
                        }
                    )
                }
            }
        }
    }

    // Scanned Barcode Result Dialog
    scannedBarcodeForDialog?.let { scannedCode ->
        val matchedProduct = viewModel.findProductByBarcode(scannedCode)
        BarcodeScanResultDialog(
            barcode = scannedCode,
            foundProduct = matchedProduct,
            currencyFormat = currencyFormat,
            onDismiss = { scannedBarcodeForDialog = null },
            onInbound = { prod ->
                scannedBarcodeForDialog = null
                quickMutationTarget = Pair(prod, "IN")
            },
            onOutbound = { prod ->
                scannedBarcodeForDialog = null
                quickMutationTarget = Pair(prod, "OUT")
            },
            onEdit = { prod ->
                scannedBarcodeForDialog = null
                productForForm = prod
                showProductForm = true
            },
            onRegisterNew = { barcode ->
                scannedBarcodeForDialog = null
                productForForm = null
                prefilledBarcodeForForm = barcode
                showProductForm = true
            }
        )
    }

    // Product Add/Edit Form Dialog
    if (showProductForm) {
        ProductFormDialog(
            initialProduct = productForForm,
            prefilledBarcode = prefilledBarcodeForForm,
            onDismiss = { showProductForm = false },
            onScanRequested = {
                showProductForm = false
                launchScanner()
            },
            onSave = { updatedProduct ->
                viewModel.saveProduct(updatedProduct) { success ->
                    if (success) {
                        showProductForm = false
                    }
                }
            }
        )
    }

    // Quick Mutation Dialog
    quickMutationTarget?.let { (prod, defaultType) ->
        QuickMutationDialog(
            product = prod,
            defaultType = defaultType,
            onDismiss = { quickMutationTarget = null },
            onSubmit = { type, qty, reason ->
                viewModel.recordMutation(
                    product = prod,
                    type = type,
                    quantity = qty,
                    reason = reason
                ) { success ->
                    if (success) {
                        quickMutationTarget = null
                    }
                }
            }
        )
    }

    // Manual Stock Adjustment Dialog
    if (showManualAdjustmentDialog) {
        ManualStockAdjustmentDialog(
            products = state.products,
            initialProduct = manualAdjustmentProduct,
            onDismiss = {
                showManualAdjustmentDialog = false
                manualAdjustmentProduct = null
            },
            onSubmitAdjustment = { prod, type, qty, reason ->
                viewModel.recordMutation(
                    product = prod,
                    type = type,
                    quantity = qty,
                    reason = reason
                ) { success ->
                    if (success) {
                        showManualAdjustmentDialog = false
                        manualAdjustmentProduct = null
                    }
                }
            }
        )
    }
}
