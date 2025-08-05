package com.yourname.smartpermissionanalyzer.ui.dashboard.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScanButton(isScanning: Boolean, onScanClick: () -> Unit) {
    Button(
        onClick = onScanClick,
        enabled = !isScanning,
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        if (isScanning) {
            CircularProgressIndicator(Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Scanning Apps…")
        } else {
            Text("Scan All Apps")
        }
    }
}
