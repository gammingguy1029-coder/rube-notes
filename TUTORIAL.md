# Rube Note Pad — Full Beginner Tutorial

Turn the code in this folder into an installable APK on your phone.
Two ways are explained. **Pick ONE:**

- **Way 1 — GitHub (recommended):** no installation, everything done in your browser. ~15 minutes.
- **Way 2 — Android Studio:** build on your own PC. ~30-45 minutes the first time (big downloads).

---

## WAY 1 — Build online with GitHub (no installation)

### Step 1 — Create a GitHub account
1. Go to **https://github.com** and click **Sign up**.
2. Enter your email, create a password, pick a username, verify your email.

### Step 2 — Create a repository
1. Log in, click the **+** icon (top-right) → **New repository**.
2. Repository name: `rube-note-pad`
3. Visibility: **Private** (recommended) or Public — either works.
4. Do **NOT** tick "Add a README" (the folder already has one).
5. Click **Create repository**.

### Step 3 — Upload the project
**Easiest way (browser upload):**
1. On the new empty repo page, click the link **"uploading an existing file"**.
2. Open your `rube note pad` folder in File Explorer.
3. Select **everything inside it EXCEPT the `.git` folder** (if you see one) and drag
   it into the browser upload area. Hidden folders like `.github` must be included —
   enable "Hidden items" in File Explorer's View menu to make sure `.github` gets copied.
4. Wait for all files to upload, then click **Commit changes**.

> Tip: if dragging the `.github` folder is troublesome, zip nothing — GitHub's web
> uploader accepts folders dragged in from Windows Explorer directly.

### Step 4 — Let GitHub build the APK
1. In your repo, click the **Actions** tab.
2. You should see a run called **"Build APK"** already started (it triggers on every push).
   If not, click **Build APK → Run workflow → Run workflow**.
3. Wait ~4–6 minutes. The circle turns from yellow ⟳ to green ✔.

### Step 5 — Download & install the APK on your phone
1. Click the finished green run → scroll to the **Artifacts** section at the bottom.
2. Download **RubeNotePad-debug-apk** (it downloads as a `.zip`).
3. Extract the zip on your PC → you get **`app-debug.apk`**.
4. Send it to your phone (WhatsApp to yourself, USB cable, Google Drive — any way).
5. On the phone, tap the APK. If asked, allow **"Install unknown apps"** for the app
   you used to open it (this is normal for apps outside the Play Store).
6. Tap **Install** → **Open**. Done! 🎉

### Step 6 — Rebuild after changes
Any time you edit the code (for example the video or channel link in
`OwnerConfig.kt`), just upload the changed file again:
1. In GitHub, open the file → **pencil (Edit) icon** → paste new content → **Commit**.
   (or drag-replace the file via "Add file ▸ Upload files")
2. Actions automatically rebuilds → download the fresh APK from the new run.

---

## WAY 2 — Build on your PC with Android Studio

### Step 1 — Install Android Studio
1. Go to **https://developer.android.com/studio**.
2. Download and run the installer (about 1 GB). Keep all default options.
3. First launch: choose **Standard** setup and let it download the Android SDK
   (another ~2-3 GB, one time only).

### Step 2 — Open the project
1. **File ▸ Open…** → select your `rube note pad` folder → **OK**.
   (If Windows SmartScreen warns, click More info ▸ Run anyway.)
2. Wait for **"Gradle sync"** to finish (progress bar at the bottom).
   The first sync downloads dependencies — be patient, it can take 5-15 minutes.
   If it asks about a missing Gradle wrapper or SDK, accept the suggested fix.

### Step 3 — Run it (try it instantly)
1. Plug in your phone with a USB cable, enable **USB debugging**
   (Settings ▸ About phone ▸ tap "Build number" 7 times ▸ back ▸ Developer options ▸ USB debugging ON).
   Or use **Device Manager** to create a free virtual phone (emulator).
2. Press the green **Run ▶** button. The app installs and opens.

### Step 4 — Export the APK file
1. Menu: **Build ▸ Build App Bundle(s) / APK(s) ▸ Build APK(s)**.
2. When done, a popup appears → click **locate**.
   The APK is at: `app/build/outputs/apk/debug/app-debug.apk`.
3. Copy it to your phone and install (same as Way 1, Step 5).

---

## Make it YOURS (owner customization)

All in one file: `app/src/main/java/com/rubenotepad/app/OwnerConfig.kt`

| What you want | What to change |
|---|---|
| Different promo video (offline, recommended) | Put your edited video at `app/src/main/assets/promo/promo_video.mp4`, then set `PROMO_SOURCE = PromoSource.LOCAL_ASSET` |
| Different YouTube video | Change `PROMO_YOUTUBE_ID` (the part after `shorts/` in the link) and `PROMO_WATCH_URL` |
| Different channel link | Change `CHANNEL_URL` (currently `https://www.youtube.com/@RubeCoder`) |
| App version number | `versionName` in `app/build.gradle.kts` |

After any change: rebuild (Way 1 Step 6 or Way 2 Step 4).

## Quick user guide (the app itself)

- **+ button** — new note. Type a title and content; it saves itself
  ("Saving… → Saved" appears at the top).
- **Tap a note** — open and edit it. Changes save automatically.
- **Magnifier** — search titles and content.
- **Grid icon** — switch list ↔ grid layout.
- **⋮ on a note (editor)** — Pin (keeps it on top), Share, Archive, Delete
  (asks for confirmation).
- **Long-press a note in the list** — same quick actions.
- **Hamburger ▸ Show archived** — view archived notes.
- Everything is stored **only on your phone**. Uninstalling removes all notes.

## Troubleshooting

| Problem | Fix |
|---|---|
| Actions build failed | Open the failed run, read the red step. Most common cause: a file wasn't uploaded. Re-upload the full folder. |
| "App not installed" on phone | The APK may be for a newer Android than your phone (needs Android 8.0+). Also make sure the file ends in `.apk` after extracting. |
| Gradle sync fails in Studio | File ▸ Sync Project with Gradle Files; check internet; update Android Studio. |
| Promo video doesn't play | It streams from YouTube — needs internet once. For offline, bundle the video (see table above); the app still works and Continue always works. |
| Where are my notes stored? | In the app's private storage on the device. Clearing app data or uninstalling deletes them. |
