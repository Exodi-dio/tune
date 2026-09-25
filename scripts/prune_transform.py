#!/usr/bin/env python3
"""Phase 4 call-site rewiring for the sync cutover (GPLv3).

Every edit asserts its anchor; any miss fails loudly with no partial state
(the workflow commits only after this script exits 0).
"""
import sys

R = "androidApp/src/main/kotlin/com/exodidio/tune"
fails = []


def read(p):
    with open(p) as f:
        return f.read()


def write(p, t):
    with open(p, "w") as f:
        f.write(t)


def remove_once(path, text, anchor, name):
    n = text.count(anchor)
    if n != 1:
        fails.append(f"{path}: anchor {name} found {n}x (want 1)")
        return text
    print(f"{path}: removed {name}")
    return text.replace(anchor, "", 1)


def check_absent(path, text, needle, name):
    if needle in text:
        fails.append(f"{path}: still contains {name}")


def remove_span(path, text, start_anchor, end_pred, name):
    i = text.find(start_anchor)
    if i < 0:
        fails.append(f"{path}: span start {name} not found")
        return text
    j = end_pred(text, i + len(start_anchor))
    if j < 0:
        fails.append(f"{path}: span end {name} not found")
        return text
    print(f"{path}: removed span {name}")
    return text[:i] + text[j:]


def end_brace4(text, k):
    """Index just past the first line that is exactly '    }' at/after k."""
    while True:
        nl = text.find("\n", k)
        if nl < 0:
            return -1
        line_start = k if k == 0 or text[k - 1] == "\n" else text.rfind("\n", 0, k) + 1
        line = text[line_start:nl]
        if line == "    }":
            return nl + 1
        k = nl + 1


# ---------------- MainActivity ----------------
p = f"{R}/MainActivity.kt"
t = read(p)
for imp in [
    "import com.exodidio.tune.pairing.AndroidPairingClock\n",
    "import com.exodidio.tune.pairing.AndroidPairingIdGenerator\n",
    "import com.exodidio.tune.pairing.HiveMqPairingTransport\n",
    "import com.exodidio.tune.pairing.HiveMqSyncSession\n",
    "import com.exodidio.tune.pairing.MobilePairingUseCase\n",
    "import com.exodidio.tune.pairing.PairingPreferences\n",
    "import com.exodidio.tune.pairing.AndroidTrustedDesktopDiscovery\n",
    "import com.exodidio.tune.sync.LibrarySyncService\n",
    "import com.exodidio.tune.sync.AndroidPlaylistReconciliationTransport\n",
    "import com.exodidio.tune.sync.PlaylistReconciliationClock\n",
    "import com.exodidio.tune.sync.PlaylistReconciliationCoordinator\n",
    "import com.exodidio.tune.sync.PlaylistReconciliationPublisher\n",
    "import com.exodidio.tune.sync.PlaylistSyncProtocol\n",
    "import com.exodidio.tune.sync.PlaylistReconciliationOutcome\n",
    "import com.exodidio.tune.sync.PlaylistMutationStatus\n",
]:
    t = remove_once(p, t, imp, imp.strip())
t = remove_span(p, t, "    private val syncViewModel: SyncViewModel by viewModels {", end_brace4, "syncViewModel property")
t = remove_once(p, t, "            val syncUiState by syncViewModel.uiState.collectAsStateWithLifecycle()\n", "syncUiState collect")
t = remove_once(p, t, """                    syncState = syncUiState,
                    onPairingQrScanned = { raw ->
                        if (!syncViewModel.acceptsQr(raw)) false else {
                            syncViewModel.pair(raw)
                            viewModel.dispatch(AppIntent.NavigateBack)
                            true
                        }
                    },
                    onUnpair = syncViewModel::unpair,
                    onSyncScreenVisible = syncViewModel::onSyncScreenVisible,
                    onSyncScreenHidden = syncViewModel::onSyncScreenHidden,
""", "settings sync wiring")
t = remove_once(p, t, "                onDismissSyncFailure = AndroidSyncRuntime::idle,\n", "onDismissSyncFailure arg")
t = remove_once(p, t, """        InsightViewModel.Factory(
            AndroidSyncRuntime.syncStore(),
            PairingPreferences(applicationContext),
            AndroidPlaybackRuntime.controller(),
        )""", "insight factory (old)")
t = t.replace("""        InsightViewModel.Factory(
            AndroidSyncRuntime.syncStore(),
            AndroidPlaybackRuntime.controller(),
        )""", """        InsightViewModel.Factory(
            AndroidSyncRuntime.syncStore(),
            AndroidPlaybackRuntime.controller(),
        )""", 1)
if t.count("reconciliationMutex") != 1 or t.count("withLock") != 1 or t.count("Mutex()") != 1:
    fails.append("MainActivity: unexpected Mutex/withLock counts before import removal")
else:
    t = remove_once(p, t, "import kotlinx.coroutines.sync.Mutex\n", "Mutex import")
    t = remove_once(p, t, "import kotlinx.coroutines.sync.withLock\n", "withLock import")
    t = remove_once(p, t, "    private val reconciliationMutex = Mutex()\n", "reconciliationMutex")
for needle in ["SyncViewModel", "HiveMq", "PairingPreferences", "MobilePairingUseCase", "AndroidTrustedDesktopDiscovery",
               "PlaylistReconciliation", "PlaylistSyncProtocol", "PlaylistMutationStatus", "LibrarySyncService",
               "AndroidPlaylistReconciliationTransport", "reconciliationMutex", "withLock", "syncUiState",
               "onDismissSyncFailure", "SettingsSync"]:
    check_absent(p, t, needle, needle)
if "import com.exodidio.tune.sync.AndroidSyncRuntime\n" not in t:
    fails.append("MainActivity: AndroidSyncRuntime import must be kept")
write(p, t)

# ---------------- App.kt ----------------
p = f"{R}/App.kt"
t = read(p)
t = remove_once(p, t, "import com.exodidio.tune.sync.AndroidSyncState\n", "AndroidSyncState import")
t = remove_once(p, t, "    onDismissSyncFailure: () -> Unit = {},\n", "onDismissSyncFailure param")
t = remove_once(p, t, "        val showSyncAddAction = currentPage == AppStackPage.SettingsSync && settings.syncState.desktop == null && !settings.syncState.isPairing\n", "showSyncAddAction")
t = remove_once(p, t, "hasActions = showSyncAddAction || showLibrarySortAction", "hasActions sync token")
t = remove_once(p, t, """                if (showSyncAddAction) {
                    TuneGlassIconButton(
                        hazeState = hazeState,
                        symbol = MaterialSymbols.Add,
                        label = stringResource(R.string.sync_add_device),
                        onClick = { onIntent(AppIntent.OpenPage(AppStackPage.SettingsSyncScanner)) },
                    )
                } else if (showPlaylistAddAction) {""", "sync add button")
t = t.replace("""                } else if (showPlaylistAddAction) {""", """                if (showPlaylistAddAction) {""", 1)
t = remove_span(p, t, "            (settings.syncState.librarySync as? AndroidSyncState.Failed)?.takeIf {",
                lambda text, k: (lambda n: n + len("\n            }\n") if n >= 0 else -1)(text.find("\n            }\n", text.find("onDismiss = onDismissSyncFailure,", k))),
                "sync failure dialog")
for needle in ["AndroidSyncState", "showSyncAddAction", "onDismissSyncFailure", "SettingsSync"]:
    check_absent(p, t, needle, needle)
write(p, t)

# ---------------- AppDestinationModels.kt ----------------
p = f"{R}/AppDestinationModels.kt"
t = read(p)
for field in [
    "    val syncState: SyncUiState = SyncUiState(),\n",
    "    val onPairingQrScanned: (String) -> Boolean = { false },\n",
    "    val onUnpair: () -> Unit = {},\n",
    "    val onSyncScreenVisible: () -> Unit = {},\n",
    "    val onSyncScreenHidden: () -> Unit = {},\n",
]:
    t = remove_once(p, t, field, field.strip())
for imp, typ in [("import com.exodidio.tune.sync.LibraryPlaylist\n", "LibraryPlaylist"),
                 ("import com.exodidio.tune.sync.LibraryTrack\n", "LibraryTrack")]:
    if t.count(typ) <= 1:
        t = remove_once(p, t, imp, imp.strip())
check_absent(p, t, "SyncUiState", "SyncUiState")
check_absent(p, t, "onSyncScreenVisible", "onSyncScreenVisible")
write(p, t)

# ---------------- AppDestinationContent.kt ----------------
p = f"{R}/ui/navigation/AppDestinationContent.kt"
t = read(p)
t = remove_once(p, t, "import com.exodidio.tune.ui.screens.SyncContent\n", "SyncContent import")
t = remove_once(p, t, "import com.exodidio.tune.ui.screens.SyncScannerContent\n", "SyncScannerContent import")
for line in [
    "    val syncUiState = settings.syncState\n",
    "    val onPairingQrScanned = settings.onPairingQrScanned\n",
    "    val onUnpair = settings.onUnpair\n",
    "    val onSyncScreenVisible = settings.onSyncScreenVisible\n",
    "    val onSyncScreenHidden = settings.onSyncScreenHidden\n",
]:
    t = remove_once(p, t, line, line.strip())
t = remove_once(p, t, """                            AppStackPage.SettingsSync -> SyncContent(
                                syncUiState = syncUiState,
                                onUnpair = onUnpair,
                                onOpenExternalUrl = { url -> onIntent(AppIntent.OpenExternalUrl(url)) },
                                onScreenVisible = onSyncScreenVisible,
                                onScreenHidden = onSyncScreenHidden,
                                modifier = settingsPageModifier,
                            )
""", "SettingsSync route")
t = remove_once(p, t, """                            AppStackPage.SettingsSyncScanner -> SyncScannerContent(
                                onQrScanned = onPairingQrScanned,
                                modifier = Modifier.padding(contentPadding),
                            )
""", "SettingsSyncScanner route")
t = remove_once(p, t, """                                onSyncSelected = {
                                    onIntent(AppIntent.OpenPage(AppStackPage.SettingsSync))
                                },
""", "onSyncSelected arg")
for needle in ["SyncContent", "SyncScannerContent", "syncUiState", "onPairingQrScanned", "onUnpair",
               "onSyncScreenVisible", "onSyncScreenHidden", "onSyncSelected", "SettingsSync"]:
    check_absent(p, t, needle, needle)
write(p, t)

# ---------------- AppDestination.kt ----------------
p = f"{R}/AppDestination.kt"
t = read(p)
t = remove_once(p, t, "    SettingsSync,\n    SettingsSyncScanner,\n", "sync page entries")
t = remove_once(p, t, "        AppStackPage.SettingsSync,\n        AppStackPage.SettingsSyncScanner,\n", "sync destination branches")
check_absent(p, t, "SettingsSync", "SettingsSync")
write(p, t)

# ---------------- AppNavigationMetadata.kt ----------------
p = f"{R}/ui/navigation/AppNavigationMetadata.kt"
t = read(p)
t = remove_once(p, t, "    AppStackPage.SettingsSync -> R.string.sync_title\n", "sync title branch")
t = remove_once(p, t, "    AppStackPage.SettingsSyncScanner -> R.string.sync_scan_title\n", "sync scanner title branch")
check_absent(p, t, "SettingsSync", "SettingsSync")
write(p, t)

# ---------------- SettingsContent.kt ----------------
p = f"{R}/ui/screens/SettingsContent.kt"
t = read(p)
t = remove_once(p, t, "    onSyncSelected: () -> Unit,\n", "onSyncSelected param")
t = remove_once(p, t, """                ActionListItem(
                    R.string.settings_sync,
                    leadingSymbol = MaterialSymbols.Refresh,
                    onClick = onSyncSelected,
                ),
""", "sync list item")
check_absent(p, t, "onSyncSelected", "onSyncSelected")
check_absent(p, t, "settings_sync", "settings_sync")
write(p, t)

# ---------------- InsightViewModel.kt ----------------
p = f"{R}/ui/screens/InsightViewModel.kt"
t = read(p)
t = remove_once(p, t, "import com.exodidio.tune.pairing.PairingPreferences\n", "PairingPreferences import")
t = remove_once(p, t, """    class Factory(
        private val store: AndroidLibrarySyncStore,
        private val preferences: PairingPreferences,
        private val playbackController: PlaybackController,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = InsightViewModel(
            store,
            flow { emit(preferences.identity()) },
            preferences.pairedDesktop,
            playbackController,
        ) as T
    }""", "insight Factory (old)")
t = t.replace("""    class Factory(
        private val store: AndroidLibrarySyncStore,
        private val playbackController: PlaybackController,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = InsightViewModel(
            store,
            flow { emit(preferences.identity()) },
            preferences.pairedDesktop,
            playbackController,
        ) as T
    }""", """PLACEHOLDER_NEVER_MATCHES""", 1)
t = remove_once(p, t, """    class Factory(
        private val store: AndroidLibrarySyncStore,
        private val playbackController: PlaybackController,
    ) : ViewModelProvider.Factory {""", "UNUSED")
write(p, t)
fails.append("InsightViewModel: manual rewrite required (placeholder guard)")

# ---------------- AndroidManifest.xml ----------------
p = "androidApp/src/main/AndroidManifest.xml"
t = read(p)
t = remove_once(p, t, """        <service
            android:name=".sync.LibrarySyncService"
            android:exported="false"
            android:foregroundServiceType="dataSync" />
""", "dataSync service")
for perm in ["CAMERA", "CHANGE_WIFI_MULTICAST_STATE", "FOREGROUND_SERVICE_DATA_SYNC"]:
    t = remove_once(p, t, f'    <uses-permission android:name="android.permission.{perm}" />\n', f"{perm} permission")
write(p, t)

if fails:
    print("PRUNE TRANSFORM FAILURES:")
    for f in fails:
        print(" -", f)
    sys.exit(1)
print("prune transform OK")
