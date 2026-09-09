# NovaMotion Ultra-Advanced Motion Graphics & Video Editor

An ultra-powerful mobile motion design, VFX, and multi-layer video editing application for Android designed to surpass Alight Motion.

## 🚀 Key Features

* **Ergonomic 3-Zone Studio Workspace**:
  - **Zone 1: Canvas Viewport** - OpenGL ES 3.2 real-time rendering, touch transform gizmos (bounding box, corner scale pins, rotation dial).
  - **Zone 2: 1-Tap Quick Action Dock** - Persistent **Keyframe Diamond (`◇`)**, Cut/Split at playhead, Play/Pause, Undo/Redo, Curve Graph toggle.
  - **Zone 3: Context-Aware Lower Deck** - Magnetic multi-track timeline, Split Bézier curve graph, and Virtual Thumb Jog Wheel.
* **Analytical Bézier Math Engine**:
  - O(1) analytical cubic bezier evaluation with velocity derivatives for instantaneous speed calculation.
  - 1-tap professional easing presets (*Linear, Ease-In, Ease-Out, Easy Ease, Overshoot, Bounce, Elastic*).
* **OpenGL ES 3.2 Real-time Shader Pipeline**:
  - Velocity-based directional motion blur.
  - Dual-filter Bloom / Neon edge glow.
  - Chromatic Aberration & RGB split distortion.
* **Hardware 4K Export Engine**:
  - Hardware accelerated GPU blit to Android `MediaCodec` for high-speed H.264/HEVC encoding.
* **Automated GitHub Actions APK Builder**:
  - Automatically compiles Debug and Release APKs on every commit via `.github/workflows/build-apk.yml`.

## 📦 How to Build the APK on GitHub

1. Push this repository to GitHub:
   ```bash
   git remote add origin https://github.com/<your-username>/novamotion-android.git
   git push -u origin main
   ```
2. Navigate to the **Actions** tab on your GitHub repository.
3. The **"Build NovaMotion Android APK"** workflow will automatically run and compile the APK.
4. Download the compiled `novamotion-release.apk` or `novamotion-debug.apk` directly from the workflow Artifacts and install it on your Android smartphone!
