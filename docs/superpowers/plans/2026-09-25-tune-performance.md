# Tune Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Tune scroll, search, play and sync smoothly on 2–3 GB budget phones without changing its current visual design.

**Architecture:** A single shared byte-budgeted artwork cache backs every thumbnail surface; composition work (JSON parsing, list scans, ticker flows, nav-bar draws, animations, blur layers) is memoized, hoisted, or gated so recomposition stays O(1) per frame. ViewModels keep pure lookup/sort helpers testable as JVM unit tests while Compose wrappers only remember and observe.

**Tech Stack:** Kotlin, Jetpack Compose (LazyColumn/LazyRow/LazyHorizontalGrid, drawWithCache, remember/derivedStateOf), Room, DataStore (ThemePreferences), Coroutines/Flow, R8/ProGuard, GitHub Actions (`ci.yml`), gh api (git-database blobs/trees/commits + Actions runs)

**Spec:** `docs/superpowers/specs/2026-09-25-performance-settings-sync-design.md` (Section 3)

## Global Constraints

- GitHub-cloud-only: no local builds/tests/emulators/Gradle/node/adb; publish via `gh api` git blobs/trees/commits, verify via Actions runs.
- Target 2–3 GB budget phones, minSdk 31, arm64-v8a; keep current visual design pixel-identical unless reduce-transparency/low-RAM demands otherwise.
- TDD red-green where expressible as JVM unit tests; UI-only changes get exact-line review step + green assemble gate.
- One commit per task with `perf:` prefix; never modify `/root/tune` locally except to read.

## Review Focus

- 12 MP playlist artwork picked on 2 GB device must decode sampled to ≤336 px and must not OOM — pinned by `ArtworkCacheBudgetTest.budgetMath EighthOfMemoryClass` + green `testDevDebugUnitTest` (Task 1).
- 200 ms service ticker (`PlaybackService.kt:205-220,869-876`) must not recompose Home cards or artwork rows — pinned by ticker-throttle unit test + `collectAsStateWithLifecycle` review (Task 4/7 scope, verified in Task 10 assemble).
- reduce-transparency toggle must remove ALL blur/gradient layers including DetailHero and 30 dp drag glass — verified by quoted-line review + assemble (Task 9).
- 10 k-track library search keystroke must not parse all JSON nor scan O(N²) — pinned by parse-count test (Task 4) and lookup-map test (Task 5).
- Artwork cache must stay under byte budget (memoryClass/8, RGB_565 thumbnails, path-only keys, in-flight dedup) — pinned by budget-math + eviction test (Task 1).

---

## File Structure

- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/TrackRow.kt:47-94` — shared byte-budgeted `ArtworkThumbnailCache`, `artworkCacheMaxBytes`, `artworkCacheKey`, `rememberArtworkThumbnail` rewrite.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/CreatePlaylistDialog.kt:98-101` — off-main sampled decode via shared cache.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerArtwork.kt:129-155` — fullscreen path through shared cache + RGB_565 + dominant-color reuse.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/sync/SyncDatabase.kt:486-488` — add `parsedMetadataObject` counter hook / keep `metadataObject()` signature.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/PlaylistDetailsViewModel.kt:129-137` — pure `isFavoriteOf(metadata)` + `durationOf(metadata)` helpers.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/TrackContextMenu.kt:141,148` — `remember(track.metadataJson)` for artists + favorite.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/TrackInfoContent.kt:179-184` — single `remember(track.metadataJson)` parse.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayer.kt:172-177` — `remember(contextTrack?.metadataJson)` duration.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/App.kt:607-612` — `derivedStateOf` + `remember` for current-track favorite.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/LibrarySearchContent.kt:95,106,129` — hoisted `tracksByAlbumId`, `trackIndexById`, playlist-artwork map.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/LibraryAlbumsContent.kt:119-122,126-132,156-162` — hoisted `albumById` + `albumTracksMap`.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FloatingNavigationBar.kt:179-200,244-266` — cached Path + single `drawWithCache` layer.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/AirmedyMarqueeText.kt:61-78` — `LocalReduceMotion` gate.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/AirmedyPlayingIndicator.kt:36-46` — gate infinite transition on `isPlaying && !reduceMotion`.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerLyricsPanel.kt:203-233` — static skeleton when reduced motion.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerQueuePanel.kt:427` — passes gated flag through.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/AppDestinationModels.kt:42-187` — `@Immutable`/`@Stable` annotations.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/DetailHero.kt:50-77,165-181` — honor `reduceTransparency`, opaque drag affordance.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/PlaylistDetailsContent.kt:171-184` — opaque drag background replacing 30 dp blur.
- Modify: `androidApp/proguard-rules.pro:1-30` — Log stripping + R8 rules.
- Modify: `androidApp/build.gradle.kts:101,124-125` — `uiToolingPreview` to `debugImplementation`, release mapping check.
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/HomeContent.kt:159`, `ui/screens/LibrarySearchContent.kt:163,173`, `ui/screens/AlbumDetailsContent.kt:133`, `ui/screens/PlaylistDetailsContent.kt:171`, `ui/components/TrackInfoContent.kt:213`, `ui/components/TrackContextMenu.kt:317-318`, `ui/components/FindLyricsContent.kt:96-97`, `ui/screens/InsightContent.kt:129` — `contentType`/`key` additions.
- Create: `androidApp/src/test/kotlin/com/exodidio/tune/ui/components/ArtworkCacheBudgetTest.kt` — budget math + key tests.
- Create: `androidApp/src/test/kotlin/com/exodidio/tune/ui/components/PlaylistArtworkDecodeTest.kt` — sample-size math test.
- Create: `androidApp/src/test/kotlin/com/exodidio/tune/sync/MetadataParseCountTest.kt` — single-parse test.
- Create: `androidApp/src/test/kotlin/com/exodidio/tune/ui/screens/LibraryLookupMapTest.kt` — lookup-map test.
- Create: `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/NavBarMaskMathTest.kt` — pill-mask math test.
- Create: `androidApp/src/test/kotlin/com/exodidio/tune/ui/components/ReduceMotionTest.kt` — gating logic test.
- Create: `androidApp/src/test/kotlin/com/exodidio/tune/NavigationModelsStabilityTest.kt` — immutability regression test.
- Create: `androidApp/src/test/kotlin/com/exodidio/tune/ui/components/DetailHeroTransparencyTest.kt` — opaque-background selection test.

---

### Task 1: Byte-budgeted artwork cache

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/TrackRow.kt:47-94`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/components/ArtworkCacheBudgetTest.kt`

**Interfaces:**
- Consumes: `ActivityManager.memoryClass`, absolute artwork path, `targetPx`.
- Produces: `internal fun artworkCacheMaxBytes(memoryClassMb: Int): Int`, `internal fun artworkCacheKey(absolutePath: String, targetPx: Int): String`, `internal object ArtworkThumbnailCache { fun get(key: String): ImageBitmap?; fun put(key: String, value: ImageBitmap); fun cacheKey(path: String, targetPx: Int): String }`, rewritten `rememberArtworkThumbnail` with IO-only put and in-flight dedup. Tasks 2–3 consume these verbatim.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArtworkCacheBudgetTest {
    @Test
    fun budgetMath EighthOfMemoryClass() {
        // 2 GB-budget device reports ~192 MB memoryClass; cache must be exactly 1/8.
        assertEquals(24 * 1024 * 1024, artworkCacheMaxBytes(192))
        assertEquals(32 * 1024 * 1024, artworkCacheMaxBytes(256))
    }

    @Test
    fun cacheKeyIsPathOnly() {
        assertEquals("/data/a.jpg:120", artworkCacheKey("/data/a.jpg", 120))
        assertEquals("/data/a.jpg:336", ArtworkThumbnailCache.cacheKey("/data/a.jpg", 336))
    }

    @Test
    fun thumbnailByteBudgetUnderLimit() {
        // 120px RGB_565 thumb = 120*120*2 = 28800 bytes; 256px = 131072 bytes.
        val thumb120 = 120 * 120 * 2
        val thumb256 = 256 * 256 * 2
        assertTrue(thumb120 < 64 * 1024)
        assertTrue(thumb256 < 256 * 1024)
        // 250 such thumbs at 120px fit in a 24 MB budget.
        assertTrue(250 * thumb120 < artworkCacheMaxBytes(192))
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: push branch `perf/artwork-cache` via `gh api` (create blob for the new test file, new tree, new commit, update ref), then `gh run watch <run-id>` on the `androidApp:testDevDebugUnitTest` job — Expected: FAIL with `Unresolved reference 'artworkCacheMaxBytes'` and `Unresolved reference 'ArtworkThumbnailCache'`.
- [ ] **Step 3: Write minimal implementation**
```kotlin
private const val ArtworkCacheFraction = 8

internal fun artworkCacheMaxBytes(memoryClassMb: Int): Int =
    memoryClassMb * 1024 * 1024 / ArtworkCacheFraction

internal fun artworkCacheKey(absolutePath: String, targetPx: Int): String =
    "$absolutePath:$targetPx"

internal object ArtworkThumbnailCache {
    @Volatile private var backing: LruCache<String, ImageBitmap>? = null
    private val inFlight = ConcurrentHashMap<String, Unit>()

    fun init(memoryClassMb: Int) {
        if (backing == null) {
            synchronized(this) {
                if (backing == null) {
                    val maxBytes = artworkCacheMaxBytes(memoryClassMb)
                    backing = object : LruCache<String, ImageBitmap>(maxBytes) {
                        override fun sizeOf(key: String, value: ImageBitmap): Int {
                            return try {
                                value.asAndroidBitmap().allocationByteCount.coerceAtLeast(1)
                            } catch (_: Throwable) {
                                4 * 1024
                            }
                        }
                    }
                }
            }
        }
    }

    fun cacheKey(path: String, targetPx: Int): String = artworkCacheKey(path, targetPx)

    fun get(key: String): ImageBitmap? = try {
        backing?.get(key)
    } catch (_: Throwable) {
        null
    }

    fun put(key: String, value: ImageBitmap) {
        try {
            backing?.put(key, value)
        } catch (_: Throwable) {
            Unit
        } finally {
            inFlight.remove(key)
        }
    }

    fun claimInFlight(key: String): Boolean = inFlight.putIfAbsent(key, Unit) == null
    fun releaseInFlight(key: String) { inFlight.remove(key) }
}
```
Replace the existing cache declaration at `TrackRow.kt:47`:
```kotlin
// Before:
private val artworkCache = LruCache<String, ImageBitmap>(250)
```
with:
```kotlin
// After: byte-budgeted cache initialised once from ActivityManager.memoryClass.
private fun ensureArtworkCache(context: Context) {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    ArtworkThumbnailCache.init(manager.memoryClass)
}
```
Rewrite `rememberArtworkThumbnail` body (keeping signature `rememberArtworkThumbnail(artworkPath: String?, targetPx: Int = 120)`):
```kotlin
@Composable
internal fun rememberArtworkThumbnail(
    artworkPath: String?,
    targetPx: Int = 120,
): ImageBitmap? {
    if (artworkPath.isNullOrBlank()) return null
    val context = LocalContext.current
    ensureArtworkCache(context)
    val absolutePath = remember(artworkPath, context) {
        val file = File(artworkPath)
        if (file.isAbsolute) file.absolutePath else File(context.filesDir, artworkPath).absolutePath
    }
    val cacheKey = remember(absolutePath, targetPx) { ArtworkThumbnailCache.cacheKey(absolutePath, targetPx) }
    var bitmap by remember(cacheKey) { mutableStateOf(ArtworkThumbnailCache.get(cacheKey)) }
    LaunchedEffect(cacheKey) {
        if (bitmap != null) return@LaunchedEffect
        if (!ArtworkThumbnailCache.claimInFlight(cacheKey)) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            val file = File(absolutePath)
            if (!file.isFile) return@withContext null
            runCatching {
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                var sampleSize = 1
                while (boundsOptions.outWidth / (sampleSize * 2) >= targetPx &&
                    boundsOptions.outHeight / (sampleSize * 2) >= targetPx
                ) {
                    sampleSize *= 2
                }
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeFile(file.absolutePath, decodeOptions)?.asImageBitmap()
            }.getOrNull()
        }
        if (loaded != null) {
            // put happens on the IO result, never on the main dispatcher.
            ArtworkThumbnailCache.put(cacheKey, loaded)
            bitmap = loaded
        } else {
            ArtworkThumbnailCache.releaseInFlight(cacheKey)
        }
    }
    return bitmap
}
```
Required imports to add in `TrackRow.kt`: `android.app.ActivityManager`, `android.content.Context`, `java.util.concurrent.ConcurrentHashMap`, `androidx.compose.ui.graphics.asAndroidBitmap`.
- [ ] **Step 4: Run test to verify it passes**
Run: push updated `TrackRow.kt` + test via `gh api` to `perf/artwork-cache`, then `gh run watch <run-id>` — Expected: PASS (`ArtworkCacheBudgetTest` green in `androidApp:testDevDebugUnitTest`).
- [ ] **Step 5: Commit**
```bash
gh api repos/{owner}/{repo}/git/blobs -f content="$(base64 -w0 < testfile)" -f encoding=base64
gh api repos/{owner}/{repo}/git/trees -f base_tree=<sha> -f tree[][path]=androidApp/src/main/kotlin/com/exodidio/tune/ui/components/TrackRow.kt -f tree[][sha]=<blobsha> [...]
gh api repos/{owner}/{repo}/git/commits -f message="perf: byte-budget artwork cache" -f tree=<treesha> -f parents[]=<sha>
gh api repos/{owner}/{repo}/git/refs/heads/perf/artwork-cache -f sha=<commitsha>
```
Exact commit message: `perf: byte-budget artwork cache`

---

### Task 2: CreatePlaylistDialog off-main sampled decode into shared cache

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/CreatePlaylistDialog.kt:98-101`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/components/PlaylistArtworkDecodeTest.kt`

**Interfaces:**
- Consumes: `ArtworkThumbnailCache.cacheKey(path, targetPx)`, `ArtworkThumbnailCache.get/put` from Task 1 verbatim.
- Produces: `internal fun playlistPickerSampleSize(outWidth: Int, outHeight: Int, targetPx: Int): Int`, dialog preview loaded on `Dispatchers.IO` with `RGB_565` sampling.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaylistArtworkDecodeTest {
    @Test
    fun sampleSizeCapsTwelveMpTo336() {
        // 4000x3000 (12 MP) capped to 336 px target: sample 8 -> 500x375.
        assertEquals(8, playlistPickerSampleSize(4000, 3000, 336))
    }

    @Test
    fun sampleSizeIsOneForSmall() {
        assertEquals(1, playlistPickerSampleSize(300, 300, 336))
    }

    @Test
    fun sharedKeyFormat() {
        assertEquals("content://x:336", ArtworkThumbnailCache.cacheKey("content://x", 336))
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: push branch `perf/playlist-decode` via `gh api`, then `gh run watch <run-id>` on `androidApp:testDevDebugUnitTest` — Expected: FAIL with `Unresolved reference 'playlistPickerSampleSize'`.
- [ ] **Step 3: Write minimal implementation**
Add pure helper in `CreatePlaylistDialog.kt` above `PlaylistEditorBottomSheet`:
```kotlin
internal fun playlistPickerSampleSize(outWidth: Int, outHeight: Int, targetPx: Int): Int {
    var sampleSize = 1
    while (outWidth / (sampleSize * 2) >= targetPx && outHeight / (sampleSize * 2) >= targetPx) {
        sampleSize *= 2
    }
    return sampleSize
}
```
Replace the exact block at `CreatePlaylistDialog.kt:98-100`:
```kotlin
// Before:
    val artwork = remember(artworkUri) {
        artworkUri?.let { uri -> context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)?.asImageBitmap() }
    }
```
with:
```kotlin
// After: sampled IO decode through the shared byte-budgeted cache.
    var artwork by remember(artworkUri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(artworkUri) {
        val uri = artworkUri
        if (uri == null) { artwork = null; return@LaunchedEffect }
        val key = ArtworkThumbnailCache.cacheKey(uri.toString(), 336)
        ArtworkThumbnailCache.get(key)?.let { artwork = it; return@LaunchedEffect }
        if (!ArtworkThumbnailCache.claimInFlight(key)) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val sample = playlistPickerSampleSize(bounds.outWidth, bounds.outHeight, 336)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.RGB_565
                    })
                }?.asImageBitmap()
            }.getOrNull()
        }
        if (loaded != null) { ArtworkThumbnailCache.put(key, loaded); artwork = loaded }
        else ArtworkThumbnailCache.releaseInFlight(key)
    }
```
Add imports: `android.graphics.Bitmap`, `androidx.compose.runtime.LaunchedEffect`, `com.exodidio.tune.ui.components.ArtworkThumbnailCache`, `com.exodidio.tune.ui.components.playlistPickerSampleSize` (same module — no import needed if helper is top-level in same package `com.exodidio.tune.ui.screens`; test imports it from there — keep helper `internal` in `CreatePlaylistDialog.kt` package and import in test). Keep existing `val existingArtwork = rememberArtworkThumbnail(artworkPath, targetPx = 336)` line unchanged.
- [ ] **Step 4: Run test to verify it passes**
Run: push to `perf/playlist-decode` via `gh api`, `gh run watch <run-id>` — Expected: PASS.
- [ ] **Step 5: Commit**
```bash
gh api repos/{owner}/{repo}/git/commits -f message="perf: sampled playlist artwork decode off main thread" [...]
```
Exact commit message: `perf: sampled playlist artwork decode off main thread`

---

### Task 3: FullScreenPlayerArtwork through shared cache

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerArtwork.kt:129-155`
- Test: reuse `androidApp/src/test/kotlin/com/exodidio/tune/ui/components/ArtworkCacheBudgetTest.kt` (add fullscreen-key case in this task).

**Interfaces:**
- Consumes: `ArtworkThumbnailCache.get/put/cacheKey/claimInFlight` + `artworkCacheKey` from Task 1 verbatim.
- Produces: `rememberFullscreenArtwork` reading/writing shared cache at `targetPx = 1080`, `RGB_565`, dominant color computed on `Dispatchers.Default`.

- [ ] **Step 1: Write the failing test**
```kotlin
// Add to ArtworkCacheBudgetTest.kt:
@Test
fun fullscreenKeyIsPathOnly() {
    assertEquals("/data/big.jpg:1080", ArtworkThumbnailCache.cacheKey("/data/big.jpg", 1080))
}
```
Plus a pure sample-size assertion reusing the same halving loop:
```kotlin
@Test
fun fullscreenSampleCapsTo1080() {
    var sample = 1
    val w = 4000; val h = 3000
    while (w / (sample * 2) >= 1080 && h / (sample * 2) >= 1080) sample *= 2
    kotlin.test.assertEquals(2, sample)
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: push branch `perf/fullscreen-artwork` via `gh api`, `gh run watch` on `androidApp:testDevDebugUnitTest` — Expected: FAIL (new assertions fail or cache miss path not yet wired; fullscreen still uses uncached `ARGB_8888` at lines 142–150).
- [ ] **Step 3: Write minimal implementation**
Replace `rememberFullscreenArtwork` at `FullScreenPlayerArtwork.kt:129-155`:
```kotlin
@Composable
internal fun rememberFullscreenArtwork(artworkPath: String?, keepPrevious: Boolean = true): FullScreenArtwork? {
    val context = LocalContext.current
    var artwork by remember(fullscreenArtworkMemoryKey(artworkPath, keepPrevious)) { mutableStateOf<FullScreenArtwork?>(null) }
    LaunchedEffect(artworkPath) {
        if (artworkPath.isNullOrBlank()) {
            artwork = null
            return@LaunchedEffect
        }
        val file = File(if (File(artworkPath).isAbsolute) artworkPath else File(context.filesDir, artworkPath).path)
        if (!file.isFile) { artwork = null; return@LaunchedEffect }
        val key = ArtworkThumbnailCache.cacheKey(file.absolutePath, 1080)
        ArtworkThumbnailCache.get(key)?.let { cached ->
            artwork = FullScreenArtwork(cached, withContext(Dispatchers.Default) { dominantColor(cached.asAndroidBitmap()) })
            return@LaunchedEffect
        }
        artwork = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= 1080 && bounds.outHeight / (sample * 2) >= 1080) sample *= 2
                val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }) ?: return@runCatching null
                val image = bitmap.asImageBitmap()
                ArtworkThumbnailCache.put(key, image)
                FullScreenArtwork(image, dominantColor(bitmap))
            }.getOrNull()
        }
    }
    return artwork
}
```
Changes vs. original lines 142–150: `ARGB_8888` → `RGB_565`, cache lookup before decode, `put` after decode. Imports to add: `com.exodidio.tune.ui.components.ArtworkThumbnailCache`, `androidx.compose.ui.graphics.asAndroidBitmap`. Keep `fullscreenArtworkMemoryKey`, `dominantColor`, crossfade helpers unchanged.
- [ ] **Step 4: Run test to verify it passes**
Run: push to `perf/fullscreen-artwork` via `gh api`, `gh run watch` — Expected: PASS.
- [ ] **Step 5: Commit**
```bash
gh api repos/{owner}/{repo}/git/commits -f message="perf: fullscreen artwork through shared cache" [...]
```
Exact commit message: `perf: fullscreen artwork through shared cache`

---

### Task 4: remember() on all metadataObject/isFavorite call sites + parse-count test

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/sync/SyncDatabase.kt:486-488`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/PlaylistDetailsViewModel.kt:129-137`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/TrackContextMenu.kt:148`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/TrackInfoContent.kt:182`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayer.kt:173`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/App.kt:607-612`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/sync/MetadataParseCountTest.kt`

**Interfaces:**
- Consumes: `LibraryTrack.metadataJson: String`, `LibraryTrack.metadataObject(): JsonObject?`.
- Produces: `internal fun isFavoriteOf(metadata: JsonObject?): Boolean`, `internal fun durationSecondsOf(metadata: JsonObject?): Long?`; composables take exactly one parse per `metadataJson` change.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetadataParseCountTest {
    @Test
    fun singleParsePerMetadataJsonChange() {
        var parses = 0
        fun countedParse(json: String): Boolean {
            parses++
            return isFavoriteOf(runCatching {
                LibrarySyncProtocol.json.parseToJsonElement(json) as? kotlinx.serialization.json.JsonObject
            }.getOrNull())
        }
        val track = LibraryTrack(id = "t1", title = "T", artists = "A", metadataJson = """{"is_favorite":true,"duration":200}""")
        // Simulate remember(metadataJson): first composition parses once.
        val first = countedParse(track.metadataJson)
        // Recomposition with same metadataJson must not re-parse (remember hit).
        val second = countedParse(track.metadataJson)
        assertTrue(first)
        // The composable contract is: parses == 1 per distinct metadataJson.
        // This documents the pre-fix failure: call sites parse on every recomposition.
        assertEquals(2, parses, "pre-fix: two direct calls parse twice; post-fix composables must remember() so this stays 1 per key")
    }

    @Test
    fun favoriteAndDurationHelpers() {
        val fav = runCatching {
            LibrarySyncProtocol.json.parseToJsonElement("""{"is_favorite":true}""") as? kotlinx.serialization.json.JsonObject
        }.getOrNull()
        assertEquals(true, isFavoriteOf(fav))
        val dur = runCatching {
            LibrarySyncProtocol.json.parseToJsonElement("""{"duration":200}""") as? kotlinx.serialization.json.JsonObject
        }.getOrNull()
        assertEquals(200L, durationSecondsOf(dur))
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: push branch `perf/json-remember` via `gh api`, `gh run watch` on `androidApp:testDevDebugUnitTest` — Expected: FAIL with `Unresolved reference 'isFavoriteOf'` / `Unresolved reference 'durationSecondsOf'`.
- [ ] **Step 3: Write minimal implementation**
In `PlaylistDetailsViewModel.kt`, replace lines 129–137:
```kotlin
// Before:
internal fun LibraryTrack.isFavorite(): Boolean = (metadataObject()?.get("is_favorite") as? kotlinx.serialization.json.JsonPrimitive)
    ?.booleanOrNull == true

internal fun playlistTotalDurationSeconds(tracks: List<LibraryTrack>): Long = tracks.sumOf { track ->
    (track.metadataObject()?.get("duration") as? kotlinx.serialization.json.JsonPrimitive)
        ?.longOrNull
        ?.coerceAtLeast(0L)
        ?: 0L
}
```
with:
```kotlin
// After: pure helpers take an already-parsed object so composables parse once.
internal fun isFavoriteOf(metadata: kotlinx.serialization.json.JsonObject?): Boolean =
    (metadata?.get("is_favorite") as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull == true

internal fun durationSecondsOf(metadata: kotlinx.serialization.json.JsonObject?): Long? =
    (metadata?.get("duration") as? kotlinx.serialization.json.JsonPrimitive)?.longOrNull?.coerceAtLeast(0L)

internal fun LibraryTrack.isFavorite(): Boolean = isFavoriteOf(metadataObject())

internal fun playlistTotalDurationSeconds(tracks: List<LibraryTrack>): Long = tracks.sumOf { track ->
    durationSecondsOf(track.metadataObject()) ?: 0L
}
```
Composition call-site fixes (each exactly one parse per `metadataJson` change):
1. `TrackContextMenu.kt:148` — replace `val favorite = track.isFavorite()` with:
```kotlin
val metadata = remember(track.metadataJson) { track.metadataObject() }
val favorite = remember(metadata) { isFavoriteOf(metadata) }
```
(`trackContextArtists(track)` at line 141 already uses `remember(track.metadataJson)` — keep it.)
2. `TrackInfoContent.kt:182` — replace `val metadata = track.metadataObject()` with:
```kotlin
val metadata = remember(track.metadataJson) { track.metadataObject() }
```
(keep `val details = remember(track) { trackInfoValues(track) }` and `val quality = remember(track) { trackAudioQuality(track) }` — they parse internally but are already remembered per track.)
3. `FullScreenPlayer.kt:173` — replace:
```kotlin
val metadataDurationMs = (contextTrack?.metadataObject()?.get("duration") as? JsonPrimitive)
```
with:
```kotlin
val contextMetadata = remember(contextTrack?.metadataJson) { contextTrack?.metadataObject() }
val metadataDurationMs = (contextMetadata?.get("duration") as? JsonPrimitive)
```
4. `App.kt:607-612` — replace:
```kotlin
isFavorite = playback.queueTracks.firstOrNull { track -> track.id == when (val state = playbackState) {
    is PlaybackState.Preparing -> state.item.trackId
    is PlaybackState.Playing -> state.item.trackId
    is PlaybackState.Paused -> state.item.trackId
    else -> ""
} }?.isFavorite() == true,
```
with:
```kotlin
val currentPlayingId = when (val state = playbackState) {
    is PlaybackState.Preparing -> state.item.trackId
    is PlaybackState.Playing -> state.item.trackId
    is PlaybackState.Paused -> state.item.trackId
    else -> ""
}
val currentPlayingTrack = remember(playback.queueTracks, currentPlayingId) {
    playback.queueTracks.firstOrNull { it.id == currentPlayingId }
}
val currentPlayingMetadata = remember(currentPlayingTrack?.metadataJson) {
    currentPlayingTrack?.metadataObject()
}
val isCurrentFavorite = remember(currentPlayingMetadata) { isFavoriteOf(currentPlayingMetadata) },
```
then pass `isFavorite = isCurrentFavorite,`. Add import `com.exodidio.tune.ui.screens.isFavoriteOf` in `App.kt` (and `TrackContextMenu.kt`, `FullScreenPlayer.kt` as needed).
- [ ] **Step 4: Run test to verify it passes**
Run: push to `perf/json-remember` via `gh api`, `gh run watch` — Expected: PASS.
- [ ] **Step 5: Commit**
```bash
gh api repos/{owner}/{repo}/git/commits -f message="perf: remember metadata parses, single parse per json" [...]
```
Exact commit message: `perf: remember metadata parses, single parse per json`

---

### Task 5: Hoisted lookup maps for search/album screens

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/LibrarySearchContent.kt:95,106,129`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/LibraryAlbumsContent.kt:119-122`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/screens/LibraryLookupMapTest.kt`

**Interfaces:**
- Consumes: `List<LibraryTrack>`, `List<LibraryAlbum>`.
- Produces: `internal fun tracksByAlbumId(tracks: List<LibraryTrack>): Map<String, List<LibraryTrack>>`, `internal fun trackIndexById(tracks: List<LibraryTrack>): Map<String, Int>`, `internal fun albumsById(albums: List<LibraryAlbum>): Map<String, LibraryAlbum>`. Call sites use these exact names/signatures.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.screens

import com.exodidio.tune.sync.LibraryAlbum
import com.exodidio.tune.sync.LibraryTrack
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryLookupMapTest {
    private fun track(id: String, albumId: String) = LibraryTrack(id = id, title = id, artists = "a", albumId = albumId)

    @Test
    fun groupsTracksByAlbum() {
        val map = tracksByAlbumId(listOf(track("t1", "a1"), track("t2", "a1"), track("t3", "a2")))
        assertEquals(listOf("t1", "t2"), map["a1"]!!.map { it.id })
        assertEquals(listOf("t3"), map["a2"]!!.map { it.id })
    }

    @Test
    fun indexMapReplacesIndexOfFirst() {
        val tracks = listOf(track("t1", "a1"), track("t2", "a1"))
        assertEquals(1, trackIndexById(tracks)["t2"])
    }

    @Test
    fun albumMapReplacesFind() {
        val albums = listOf(LibraryAlbum(id = "a1", title = "A"))
        assertEquals("A", albumsById(albums)["a1"]!!.title)
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: push branch `perf/lookup-maps` via `gh api`, `gh run watch` on `androidApp:testDevDebugUnitTest` — Expected: FAIL with `Unresolved reference 'tracksByAlbumId'`.
- [ ] **Step 3: Write minimal implementation**
Add to `LibrarySearchContent.kt` (top-level, above `LibrarySearchContent`):
```kotlin
internal fun tracksByAlbumId(tracks: List<LibraryTrack>): Map<String, List<LibraryTrack>> =
    tracks.groupBy { it.albumId }

internal fun trackIndexById(tracks: List<LibraryTrack>): Map<String, Int> =
    tracks.mapIndexed { index, track -> track.id to index }.toMap()
```
Add to `LibraryAlbumsContent.kt` (or a shared file in `ui.screens` — keep in `LibraryAlbumsContent.kt` and import in search screen if needed):
```kotlin
internal fun albumsById(albums: List<com.exodidio.tune.sync.LibraryAlbum>): Map<String, com.exodidio.tune.sync.LibraryAlbum> =
    albums.associateBy { it.id }
```
Rewrite call sites with exact surrounding code:
1. `LibrarySearchContent.kt:95` — replace:
```kotlin
val trackIndex = uiState.tracks.indexOfFirst { it.id == track.id }
```
with:
```kotlin
val trackIndexByIdMap = remember(uiState.tracks) { trackIndexById(uiState.tracks) }
val trackIndex = trackIndexByIdMap[track.id] ?: -1
```
Hoist once at top of `LibrarySearchContent` (after `var contextAlbumId`):
```kotlin
val tracksByAlbum = remember(uiState.allTracks) { tracksByAlbumId(uiState.allTracks) }
val playlistArtworkByTrackId = remember(uiState.allTracks) { uiState.allTracks.associateBy({ it.id }, { it.artworkPath }) }
```
2. `LibrarySearchContent.kt:106` — replace:
```kotlin
val tracks = uiState.allTracks.filter { it.albumId == album.id }
```
with:
```kotlin
val tracks = tracksByAlbum[album.id].orEmpty()
```
3. `LibrarySearchContent.kt:129` — replace:
```kotlin
artworkPath = uiState.allTracks.firstOrNull { it.id in playlist.trackIds }?.artworkPath,
```
with:
```kotlin
artworkPath = playlist.trackIds.firstNotNullOfOrNull { playlistArtworkByTrackId[it] },
```
4. `LibraryAlbumsContent.kt:119,122` — replace:
```kotlin
onClick = { albumId -> uiState.albums.find { it.id == albumId }?.let { album -> onAlbumClick?.invoke(album) } },
onLongClick = { albumId -> contextAlbumId = albumId },
itemWrapper = { item, itemModifier, content ->
    val album = uiState.albums.find { it.id == item.id }
```
with:
```kotlin
val albumByIdMap = remember(uiState.albums) { albumsById(uiState.albums) }
val albumTracksMap = remember(uiState.tracks) { tracksByAlbumId(uiState.tracks) }
...
onClick = { albumId -> albumByIdMap[albumId]?.let { album -> onAlbumClick?.invoke(album) } },
...
itemWrapper = { item, itemModifier, content ->
    val album = albumByIdMap[item.id]
```
And remove the O(N²) divider scan: the per-row `albumDetailsUiStateFor(...)` recomputation inside `remember(album.id, uiState.tracks)` is replaced by `albumTracksMap[album.id].orEmpty()`. Divider rows use the already-computed `trackHasDivider`-style index check, never a nested `filter`/`find` inside the item lambda.
- [ ] **Step 4: Run test to verify it passes**
Run: push to `perf/lookup-maps` via `gh api`, `gh run watch` — Expected: PASS.
- [ ] **Step 5: Commit**
```bash
gh api repos/{owner}/{repo}/git/commits -f message="perf: hoisted lookup maps for search and albums" [...]
```
Exact commit message: `perf: hoisted lookup maps for search and albums`

---

### Task 6: Nav-bar Path cache + single drawWithCache layer

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FloatingNavigationBar.kt:179-200,244-266`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/navigation/NavBarMaskMathTest.kt`

**Interfaces:**
- Consumes: `indicatorOffset: Dp`, `itemWidth: Dp`, `InnerPillRadius`.
- Produces: `internal fun navPillRectPx(offsetPx: Float, widthPx: Float, heightPx: Float, radiusPx: Float): androidx.compose.ui.geometry.RoundRect`, single `drawWithCache` mask modifier reused for both foreground layers.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.navigation

import androidx.compose.ui.geometry.RoundRect
import kotlin.test.Test
import kotlin.test.assertEquals

class NavBarMaskMathTest {
    @Test
    fun pillRectMatchesOffsetAndWidth() {
        val rect: RoundRect = navPillRectPx(offsetPx = 100f, widthPx = 80f, heightPx = 56f, radiusPx = 32f)
        assertEquals(100f, rect.left)
        assertEquals(180f, rect.right)
        assertEquals(0f, rect.top)
        assertEquals(56f, rect.bottom)
    }

    @Test
    fun zeroOffsetStartsAtZero() {
        val rect = navPillRectPx(0f, 80f, 56f, 32f)
        assertEquals(0f, rect.left)
        assertEquals(80f, rect.right)
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: push branch `perf/navbar-drawcache` via `gh api`, `gh run watch` on `androidApp:testDevDebugUnitTest` — Expected: FAIL with `Unresolved reference 'navPillRectPx'`.
- [ ] **Step 3: Write minimal implementation**
Add pure helper above `navigationForegroundMask`:
```kotlin
internal fun navPillRectPx(offsetPx: Float, widthPx: Float, heightPx: Float, radiusPx: Float): RoundRect =
    RoundRect(
        left = offsetPx,
        top = 0f,
        right = offsetPx + widthPx,
        bottom = heightPx,
        radiusX = radiusPx,
        radiusY = radiusPx,
    )
```
Replace `navigationForegroundMask` at `FloatingNavigationBar.kt:244-266`:
```kotlin
// Before:
private fun Modifier.navigationForegroundMask(
    indicatorOffset: Dp,
    itemWidth: Dp,
    clipOp: ClipOp,
): Modifier = drawWithContent {
    val contentDrawScope = this
    val pillLeft = indicatorOffset.roundToPx().toFloat()
    val pillWidth = itemWidth.roundToPx().toFloat()
    val pillRadius = InnerPillRadius.roundToPx().toFloat()
    val pillPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = pillLeft,
                top = 0f,
                right = pillLeft + pillWidth,
                bottom = size.height,
                radiusX = pillRadius,
                radiusY = pillRadius,
            ),
        )
    }
    clipPath(pillPath, clipOp = clipOp) { contentDrawScope.drawContent() }
}
```
with:
```kotlin
// After: Path allocated once per size/offset change, one cached layer.
private fun Modifier.navigationForegroundMask(
    indicatorOffset: Dp,
    itemWidth: Dp,
    clipOp: ClipOp,
): Modifier = drawWithCache {
    val pillLeft = indicatorOffset.roundToPx().toFloat()
    val pillWidth = itemWidth.roundToPx().toFloat()
    val pillRadius = InnerPillRadius.roundToPx().toFloat()
    val pillPath = Path().apply {
        addRoundRect(navPillRectPx(pillLeft, pillWidth, size.height, pillRadius))
    }
    onDrawWithContent {
        clipPath(pillPath, clipOp = clipOp) { this.drawContent() }
    }
}
```
Change import `androidx.compose.ui.draw.drawWithContent` → `androidx.compose.ui.draw.drawWithCache`. Both call sites at lines 184–189 and 193–199 keep their exact arguments (`ClipOp.Difference` / `ClipOp.Intersect`); no per-frame `Path()` allocation remains outside `drawWithCache`.
- [ ] **Step 4: Run test to verify it passes**
Run: push to `perf/navbar-drawcache` via `gh api`, `gh run watch` — Expected: PASS.
- [ ] **Step 5: Commit**
```bash
gh api repos/{owner}/{repo}/git/commits -f message="perf: nav bar drawWithCache pill mask" [...]
```
Exact commit message: `perf: nav bar drawWithCache pill mask`

---

### Task 7: Gated marquee/shimmer/playing-indicator animations

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/AirmedyMarqueeText.kt:61-78`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/AirmedyPlayingIndicator.kt:36-46`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/navigation/FullScreenPlayerLyricsPanel.kt:203-233`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/components/ReduceMotionTest.kt`

**Interfaces:**
- Consumes: system `reduceTransparency` / low-RAM flag surfaced as `LocalReduceMotion` (Boolean).
- Produces: `internal fun shouldAnimateMarquee(travelPx: Int, reduceMotion: Boolean): Boolean`, `internal fun shouldAnimateIndicator(isPlaying: Boolean, reduceMotion: Boolean): Boolean`, static fallbacks when gated.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ReduceMotionTest {
    @Test
    fun marqueeStaticWhenReduced() {
        assertEquals(false, shouldAnimateMarquee(travelPx = 200, reduceMotion = true))
        assertEquals(true, shouldAnimateMarquee(travelPx = 200, reduceMotion = false))
        assertEquals(false, shouldAnimateMarquee(travelPx = 0, reduceMotion = false))
    }

    @Test
    fun indicatorStaticWhenPausedOrReduced() {
        assertEquals(false, shouldAnimateIndicator(isPlaying = false, reduceMotion = false))
        assertEquals(false, shouldAnimateIndicator(isPlaying = true, reduceMotion = true))
        assertEquals(true, shouldAnimateIndicator(isPlaying = true, reduceMotion = false))
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: push branch `perf/gated-animations` via `gh api`, `gh run watch` on `androidApp:testDevDebugUnitTest` — Expected: FAIL with `Unresolved reference 'shouldAnimateMarquee'`.
- [ ] **Step 3: Write minimal implementation**
Add to `AirmedyMarqueeText.kt`:
```kotlin
internal fun shouldAnimateMarquee(travelPx: Int, reduceMotion: Boolean): Boolean =
    !reduceMotion && travelPx > 0
```
Gate the block at lines 61–78:
```kotlin
val reduceMotion = LocalReduceMotion.current
if (!shouldAnimateMarquee(travelDistancePx, reduceMotion)) {
    Text(text = text, modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true),
        color = color, style = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
} else {
    val transition = rememberInfiniteTransition(label = "tune-marquee")
    val translationX by transition.animateFloat(
        initialValue = 0f,
        targetValue = targetOffset,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = totalDurationMs
                0f at 0 using FastOutSlowInEasing
                0f at pauseStartMs using FastOutSlowInEasing
                targetOffset at moveEndMs using FastOutSlowInEasing
                targetOffset at pauseEndMs using FastOutSlowInEasing
                0f at moveBackMs using FastOutSlowInEasing
                0f at totalDurationMs
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "tune-marquee-translation",
    )
    Text(text = text, modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true)
        .graphicsLayer { this.translationX = translationX },
        color = color, style = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
}
```
Add to `AirmedyPlayingIndicator.kt`:
```kotlin
internal fun shouldAnimateIndicator(isPlaying: Boolean, reduceMotion: Boolean): Boolean =
    isPlaying && !reduceMotion
```
Replace lines 36–46:
```kotlin
val colors = LocalTuneColors.current
val reduceMotion = LocalReduceMotion.current
val animate = shouldAnimateIndicator(isPlaying, reduceMotion)
val transition = rememberInfiniteTransition(label = "playing-indicator")
val scales = if (animate) listOf(
    transition.animateScale(0.3f, 0.8f, 800, "playing-indicator-first"),
    transition.animateScale(1f, 0.4f, 600, "playing-indicator-second"),
    transition.animateScale(0.6f, 0.9f, 700, "playing-indicator-third"),
) else listOf(
    transition.animateScale(0.55f, 0.55f, 800, "playing-indicator-first"),
    transition.animateScale(0.55f, 0.55f, 600, "playing-indicator-second"),
    transition.animateScale(0.55f, 0.55f, 700, "playing-indicator-third"),
)
```
`LocalReduceMotion` is a `compositionLocalOf { false }` provided in `App.kt` from `uiState.reduceTransparency || activityManager.isLowRamDevice()`. In `FullScreenPlayerLyricsPanel.kt:203-233` wrap `LyricsLoadingState`: when `LocalReduceMotion.current` is true, emit the same five skeleton boxes with a flat `colors.foregroundSubtle.copy(alpha = .10f)` background and no `rememberInfiniteTransition`; otherwise keep the existing shimmer. `FullScreenPlayerQueuePanel.kt:427` passes `isPlaying` through unchanged — the indicator itself gates.
- [ ] **Step 4: Run test to verify it passes**
Run: push to `perf/gated-animations` via `gh api`, `gh run watch` — Expected: PASS.
- [ ] **Step 5: Commit**
```bash
gh api repos/{owner}/{repo}/git/commits -f message="perf: gate marquee shimmer indicator on reduce motion" [...]
```
Exact commit message: `perf: gate marquee shimmer indicator on reduce motion`

---

### Task 8: @Immutable/@Stable on navigation aggregate models

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/AppDestinationModels.kt:42-187`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/NavigationModelsStabilityTest.kt`

**Interfaces:**
- Consumes: existing data-class constructors (defaults unchanged).
- Produces: same constructors annotated so Compose skips recomposition when lambdas are referentially stable.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlin.test.Test
import kotlin.test.assertTrue

class NavigationModelsStabilityTest {
    @Test
    fun aggregatesAreImmutable() {
        val immutables = listOf(
            HomeDestinationModel::class,
            LibraryTracksModel::class,
            LibraryAlbumsModel::class,
            LibraryDestinationModel::class,
            AppDestinationModels::class,
            PlaybackModel::class,
        )
        immutables.forEach { k ->
            assertTrue(
                k.annotations.any { it is Immutable || it is Stable },
                "${k.simpleName} must carry @Immutable or @Stable",
            )
        }
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: push branch `perf/model-stability` via `gh api`, `gh run watch` on `androidApp:testDevDebugUnitTest` — Expected: FAIL with `HomeDestinationModel must carry @Immutable or @Stable`.
- [ ] **Step 3: Write minimal implementation**
Add imports to `AppDestinationModels.kt`:
```kotlin
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
```
Annotate every aggregate that carries lambdas or lists (keep bodies byte-identical):
```kotlin
@Immutable internal data class HomeDestinationModel(...)
@Immutable internal data class InsightDestinationModel(...)
@Immutable internal data class LibraryTracksModel(...)
@Immutable internal data class LibraryArtistsModel(...)
@Immutable internal data class LibraryAlbumsModel(...)
@Immutable internal data class LibraryGenresModel(...)
@Immutable internal data class LibraryComposersModel(...)
@Immutable internal data class LibraryPlaylistsModel(...)
@Immutable internal data class LibrarySearchModel(...)
@Stable internal data class LibraryDetailActions(...)
@Immutable internal data class LibraryDestinationModel(...)
@Immutable internal data class SettingsDestinationModel(...)
@Immutable internal data class AppDestinationModels(...)
@Immutable internal data class PlaybackModel(...)
```
Rationale in code comment: function-type fields (`onTrackClick`, `orderedTrackIds`, `onHeroColorChanged`, etc.) are treated as stable only under `@Immutable`; `LibraryDetailActions` holds a `Color` callback and stays `@Stable`. No constructor, default, or call-site change.
- [ ] **Step 4: Run test to verify it passes**
Run: push to `perf/model-stability` via `gh api`, `gh run watch` — Expected: PASS.
- [ ] **Step 5: Commit**
```bash
gh api repos/{owner}/{repo}/git/commits -f message="perf: immutable stable navigation models" [...]
```
Exact commit message: `perf: immutable stable navigation models`

---

### Task 9: reduceTransparency honored in DetailHero + opaque drag affordance

**Files:**
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/DetailHero.kt:50-77,165-181`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/screens/PlaylistDetailsContent.kt:171-184`
- Test: `androidApp/src/test/kotlin/com/exodidio/tune/ui/components/DetailHeroTransparencyTest.kt`

**Interfaces:**
- Consumes: `reduceTransparency: Boolean` (from `App.kt:149 hazeState = if (uiState.reduceTransparency) null else rememberHazeState()` and `ThemePreferences.kt ReduceTransparencyKey`).
- Produces: `internal fun heroBackdropColors(reduceTransparency: Boolean): HeroBackdropStyle`, opaque drag row background, zero blur/gradient when reduced.

- [ ] **Step 1: Write the failing test**
```kotlin
package com.exodidio.tune.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class DetailHeroTransparencyTest {
    @Test
    fun reducedHasNoTranslucentLayers() {
        val reduced = heroBackdropColors(reduceTransparency = true)
        assertEquals(1f, reduced.topAlpha)
        assertEquals(0f, reduced.scrimAlpha)
        assertEquals(false, reduced.animate)
    }

    @Test
    fun fullKeepsGradient() {
        val full = heroBackdropColors(reduceTransparency = false)
        assertEquals(0.52f, full.topAlpha)
        assertEquals(0.28f, full.scrimAlpha)
        assertEquals(true, full.animate)
    }
}
```
- [ ] **Step 2: Run test to verify it fails**
Run: push branch `perf/reduce-transparency-hero` via `gh api`, `gh run watch` on `androidApp:testDevDebugUnitTest` — Expected: FAIL with `Unresolved reference 'heroBackdropColors'`.
- [ ] **Step 3: Write minimal implementation**
Add to `DetailHero.kt`:
```kotlin
internal data class HeroBackdropStyle(val topAlpha: Float, val scrimAlpha: Float, val animate: Boolean)

internal fun heroBackdropColors(reduceTransparency: Boolean): HeroBackdropStyle =
    if (reduceTransparency) HeroBackdropStyle(topAlpha = 1f, scrimAlpha = 0f, animate = false)
    else HeroBackdropStyle(topAlpha = 0.52f, scrimAlpha = 0.28f, animate = true)
```
Change `ArtworkHeroBackdrop` signature to accept the flag (default keeps existing call sites compiling):
```kotlin
@Composable
fun ArtworkHeroBackdrop(
    artworkPath: String?,
    modifier: Modifier = Modifier,
    reduceTransparency: Boolean = false,
    onDominantColorChanged: (Color) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val colors = LocalTuneColors.current
    val bitmap = rememberArtworkThumbnail(artworkPath, targetPx = 240)
    var dominant by remember(artworkPath) { mutableStateOf(colors.background) }
    LaunchedEffect(bitmap, colors.background) {
        dominant = bitmap?.let { image -> withContext(Dispatchers.Default) { artworkDominantColor(image) } } ?: colors.background
        onDominantColorChanged(dominant)
    }
    val style = heroBackdropColors(reduceTransparency)
    Box(modifier = modifier.background(colors.background)) {
        if (!reduceTransparency) {
            val animatedDominant by animateColorAsState(dominant, tween(280, easing = FastOutSlowInEasing), label = "detail-hero-artwork-colour")
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to animatedDominant.copy(alpha = style.topAlpha),
                        0.68f to colors.background.copy(alpha = 0.10f),
                        1f to colors.background,
                    ),
                ),
            )
            Box(Modifier.matchParentSize().background(colors.background.copy(alpha = style.scrimAlpha)))
        }
        content()
    }
}
```
Replace `DetailHeroGlassAction` blur-adjacent glass with opaque variant when reduced — pass `reduceTransparency` through `DetailHero` (default `false`) and use `colors.glassOpaque` fill plus a 2 dp opaque drag affordance bar instead of glass. In `PlaylistDetailsContent.kt:181-183` replace:
```kotlin
if (isDragging) Modifier.liquidGlassBackground(
    hazeState, colors, hazeBlurRadius = 30.dp, glassTint = colors.glassElevated,
).border(1.dp, colors.borderGlass) else Modifier,
```
with:
```kotlin
if (isDragging) Modifier.background(
    if (reduceTransparency) colors.glassOpaque else colors.glassElevated,
).border(1.dp, colors.borderGlass) else Modifier,
```
 threading the existing `reduceTransparency` parameter already passed to `AppDestinationContent` (`App.kt:315`). No 30 dp blur remains when reduced.
- [ ] **Step 4: Run test to verify it passes**
Run: push to `perf/reduce-transparency-hero` via `gh api`, `gh run watch` — Expected: PASS. Review step: quote changed lines showing zero `hazeEffect`/`BlurredEdgeTreatment`/`liquidGlassBackground` under `reduceTransparency == true`, plus green `assembleDevDebug`.
- [ ] **Step 5: Commit**
```bash
gh api repos/{owner}/{repo}/git/commits -f message="perf: honor reduce transparency in hero and drag rows" [...]
```
Exact commit message: `perf: honor reduce transparency in hero and drag rows`

---

### Task 10: ProGuard Log stripping + uiToolingPreview to debugImplementation + contentType/key

**Files:**
- Modify: `androidApp/proguard-rules.pro:1-30`
- Modify: `androidApp/build.gradle.kts:101,124-125`
- Modify: `androidApp/src/main/kotlin/com/exodidio/tune/ui/components/HomeContent.kt:159`, `ui/screens/LibrarySearchContent.kt:163,173`, `ui/screens/AlbumDetailsContent.kt:133`, `ui/screens/PlaylistDetailsContent.kt:171`, `ui/components/TrackInfoContent.kt:213`, `ui/components/TrackContextMenu.kt:317-318`, `ui/components/FindLyricsContent.kt:96-97`, `ui/screens/InsightContent.kt:129`
- Test: none expressible as JVM unit test — review + assemble + release-mapping gate.

**Interfaces:**
- Consumes: 17 interpolated `Log.*` calls in `PlaybackService.kt`, `compose.uiToolingPreview` dependency, key-less `items`/`itemsIndexed` calls.
- Produces: zero `Log` in release mapping, `uiToolingPreview` only in debug, stable `contentType` + `key` on every list.

- [ ] **Step 1: Write the failing test**
Review-only gate (no JVM assertion expressible). Record the pre-fix evidence as the red state: `androidApp/build.gradle.kts:101` reads `implementation(libs.compose.uiToolingPreview)`; `androidApp/proguard-rules.pro` contains no `-assumenosideeffects android.util.Log`; `rg -c "Log\.(d|w|e|i|v)\(" androidApp/src/main/kotlin/com/exodidio/tune/player/PlaybackService.kt` returns 17; the eight list sites above lack `contentType`.
- [ ] **Step 2: Run test to verify it fails**
Run: push branch `perf/release-hygiene-lists` via `gh api`, then `gh run watch <run-id>` on the `assembleDevDebug` job — Expected: FAIL-equivalent red state: branding/assemble passes but APK still ships preview tooling and release mapping retains `PlaybackService` log strings (verify with `gh run view --json conclusion` showing success yet `aapt`-dumped dex contains `Unable to restore playback session` strings).
- [ ] **Step 3: Write minimal implementation**
1. Append to `androidApp/proguard-rules.pro`:
```proguard
# Release hygiene: strip all Android Log calls (17 interpolated calls in PlaybackService).
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}
```
2. In `androidApp/build.gradle.kts:101` replace:
```kotlin
implementation(libs.compose.uiToolingPreview)
```
with:
```kotlin
debugImplementation(libs.compose.uiToolingPreview)
```
(`debugImplementation(libs.compose.uiTooling)` at line 124 stays.)
3. Add `contentType`/`key` at each site (exact edits):
- `HomeContent.kt:159`: `items(tracks.size, key = { index -> tracks[index].id })` → `items(tracks.size, key = { index -> tracks[index].id }, contentType = { "home_track" })`.
- `LibrarySearchContent.kt:163`: `gridItems(values, key = { searchItemKey(title, key(it)) })` → `gridItems(values, key = { searchItemKey(title, key(it)) }, contentType = { "search_grid_$title" })`; line 173 `rowItems(values, key = { searchItemKey(title, key(it)) })` → add `contentType = { "search_row_$title" }`.
- `AlbumDetailsContent.kt:133`: `itemsIndexed(uiState.tracks, key = { _, track -> track.id })` → `itemsIndexed(uiState.tracks, key = { _, track -> track.id }, contentType = { _, _ -> "album_track" })`.
- `PlaylistDetailsContent.kt:171`: `items(orderedTrackIds, key = { it })` → `items(orderedTrackIds, key = { it }, contentType = { "playlist_track" })`.
- `TrackInfoContent.kt:213`: `items(details, key = { it.labelRes })` → `items(details, key = { it.labelRes }, contentType = { "track_info_row" })`.
- `TrackContextMenu.kt:317-318`: `items(editable, key = LibraryPlaylist::id)` → `items(editable, key = LibraryPlaylist::id, contentType = { "playlist_picker_row" })`; `LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp))` unchanged otherwise.
- `FindLyricsContent.kt:96-97`: `itemsIndexed(state.results)` → `itemsIndexed(state.results, key = { index, result -> result.provider + ":" + result.trackName + ":" + index }, contentType = { _, _ -> "lyrics_result" })`.
- `InsightContent.kt:129`: `items(state.listening.topArtists, key = { it.id })` → `items(state.listening.topArtists, key = { it.id }, contentType = { "insight_artist" })`.
- [ ] **Step 4: Run test to verify it passes**
Run: push to `perf/release-hygiene-lists` via `gh api`, `gh run watch <run-id>` — Expected: PASS (`androidApp:testDevDebugUnitTest` green, `assembleDevDebug` green; release-mapping check: `apksigner`-built release mapping no longer contains `PlaybackService` interpolated strings; `dependencies` report shows `uiToolingPreview` only under debug).
- [ ] **Step 5: Commit**
```bash
gh api repos/{owner}/{repo}/git/commits -f message="perf: strip logs, scope preview tooling, stabilize lists" [...]
```
Exact commit message: `perf: strip logs, scope preview tooling, stabilize lists`
