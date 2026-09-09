# NovaMotion Android — Production Readiness Audit
**Generated:** 2026-09-09  
**Auditor:** Production Engineering Review

---

## 1. Architecture Status Overview

| Area | Status | Grade |
|------|--------|-------|
| Project Model (Layer/Transform/Keyframe) | Solid data classes, good immutability | B+ |
| Keyframe Evaluation (BezierCurve lerp) | Working math | B |
| OpenGL Renderer (SceneRenderer + ShaderProgram) | Renders TEXT/SHAPE/IMAGE, real GL calls | B |
| EGL Export Surface (EglSurfaceRenderer) | Correct EGL 1.4 lifecycle | B+ |
| MediaCodec Export (HardwareVideoEncoder) | AVC H.264 + MediaMuxer pipeline | C+ |
| Audio Playback (AudioPlaybackEngine) | Real MediaPlayer, basic seek | C |
| Playback Clock | delay(16) coroutine -- broken wall clock | FAIL |
| Video Frame Decode | MediaMetadataRetriever.getFrameAtTime -- not a player | FAIL |
| Project Persistence | In-memory only, lost on process kill | FAIL |
| State Architecture | mutableStateOf in composable, no ViewModel | D |
| Undo/Redo | History stack in ProjectManager works for explicit ops | C+ |
| Transform Hierarchy | Parent/child matrix chain implemented | B |
| Asset Serialization | JSON serialize/deserialize (posX keyframes only) | C |
| Waveform Extraction | Real MediaCodec audio decode pipeline | B |
| Release Build | Debug keystore, R8 disabled, no ProGuard | D |
| Manifest Permissions | READ_MEDIA_VIDEO/AUDIO/IMAGES present, WRITE missing | C |

---

## 2. P0 Bugs (Breaks core functionality)

### P0-001 — delay(16) Playback Clock [CRITICAL]
**File:** StudioWorkspace.kt  
Coroutine delay drifts, accumulates errors, desynchronizes from audio.
Fix: Replace with System.nanoTime() delta-time or Choreographer.FrameCallback

### P0-002 — MediaMetadataRetriever.getFrameAtTime for Video [CRITICAL]
**File:** ImageTextureLoader.kt  
getFrameAtTime is a thumbnail extractor, NOT a video player.
- Decodes JPEG at nearest keyframe (500ms accuracy)
- Caches per timeUs key -> infinite memory leak
- Takes 50-150ms per call, blocking GL thread
Fix: Media3 ExoPlayer -> SurfaceTexture (OES) for VIDEO layers

### P0-003 — WRITE_EXTERNAL_STORAGE missing
Export cannot save MP4 to MediaStore without correct permissions.

### P0-004 — Export saves to app-private dir
Output MP4 written to internal files dir - inaccessible to gallery.
Fix: Write to MediaStore RELATIVE_PATH = Environment.DIRECTORY_MOVIES

### P0-005 — AudioPlaybackEngine.seekTo() uses Int (overflow)
player.seekTo(fromMs.toInt()) truncates Long values.
Fix: player.seekTo(fromMs, MediaPlayer.SEEK_CLOSEST)

### P0-006 — No ViewModel / State hoisting
All project state in Compose mutableStateOf. Config changes lose entire session.
Fix: EditorViewModel with SavedStateHandle

---

## 3. P1 Bugs (Major quality/UX degradation)

- P1-001: ImageTextureLoader caches by uri+timeUs -> memory leak
- P1-002: Timeline drag-to-reorder/trim not implemented
- P1-003: ProjectSerializer only serializes posX keyframes (all others dropped)
- P1-004: Exported video not visible in gallery
- P1-005: GLSurfaceView.RENDERMODE_CONTINUOUSLY wastes GPU
- P1-006: EglSurfaceRenderer requests ES2 config but creates ES3 context
- P1-007: Release build uses debug keystore + R8 disabled
- P1-008: versionCode=1, versionName="1.0.0-ultra" (stale)
- P1-009: No android:largeHeap="true" in manifest
- P1-010: No FOREGROUND_SERVICE for long export jobs
- P1-011: ExportConfiguration data class defined redundantly

---

## 4. What Actually Works (Confirmed)

- App launches: SplashScreen -> HomeScreen -> StudioWorkspace
- OpenGL ES 3.0 surface renders TEXT + SHAPE layers
- Keyframe bezier interpolation math is correct
- Undo/Redo history stack (ProjectManager)
- MediaCodec H.264 export pipeline structure (EGL + Encoder + Muxer)
- WaveformExtractor uses real MediaCodec audio decode
- AssetImporter imports video/image/audio with metadata
- CI/CD builds and produces APKs (v2.0.0-production released)
- Media3 ExoPlayer dependency declared (not yet used)
- Transform hierarchy (parent/child matrix chain)
- BezierGraphEditor UI wired
- EffectsBrowserSheet (20 effect types defined)

---

## 5. Implementation Plan

### Phase 1 — Core Stability
1. EditorViewModel - hoist project state
2. Replace delay(16) with Choreographer delta-time clock
3. Fix AudioPlaybackEngine seekTo Int overflow
4. Fix EglSurfaceRenderer EGL config (ES2 bit -> ES3)
5. Fix release build config, versionCode, minify
6. Add largeHeap + permissions to manifest

### Phase 2 — Media Pipeline  
7. Replace ImageTextureLoader for VIDEO with ExoPlayer + SurfaceTexture
8. MediaStore export output (MP4 visible in gallery)
9. Foreground service for export

### Phase 3 — Rendering + Timeline
10. RENDERMODE_WHEN_DIRTY optimization
11. TextTextureGenerator cache by content hash
12. Timeline drag-to-reorder / trim handles

### Phase 4 — Serialization + Polish
13. Complete ProjectSerializer (all keyframe properties)
14. AudioSyncManager wire-up

