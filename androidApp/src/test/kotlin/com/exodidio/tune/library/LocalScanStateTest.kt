package com.exodidio.tune.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalScanStateTest {
    @Test fun grantedPermissionStartsClean() {
        val state = reduceScanPermission(LocalScanUiState(), granted = true, showRationale = false)
        assertEquals(ScanPermission.Granted, state.permission)
        assertFalse(state.scanning)
        assertNull(state.error)
    }

    @Test fun deniedWithRationaleRequestsIt() {
        val state = reduceScanPermission(LocalScanUiState(), granted = false, showRationale = true)
        assertEquals(ScanPermission.NeedsRationale, state.permission)
    }

    @Test fun deniedWithoutRationaleIsDenied() {
        val state = reduceScanPermission(LocalScanUiState(), granted = false, showRationale = false)
        assertEquals(ScanPermission.Denied, state.permission)
    }

    @Test fun permissionChangePreservesLastResult() {
        val before = LocalScanUiState(lastResult = LocalImportSummary(inserted = 10, skipped = 1))
        val after = reduceScanPermission(before, granted = true, showRationale = false)
        assertEquals(LocalImportSummary(inserted = 10, skipped = 1), after.lastResult)
        assertTrue(after.error == null)
    }
}
