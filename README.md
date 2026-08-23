# Rube Note Pad

A polished, **local-first** Android sticky-note application, styled after the classic
yellow notepad reference design: brown app bar, yellow note cards, lined-paper editor,
pin/archive/share/delete overflow menu, and a floating "+" button.

**Privacy-first:** notes are stored in a private on-device SQLite database. No cloud,
no account, no analytics, no tracking. The only network use is the *optional*
promotional video (if configured to stream) and the owner's channel link.

---

## Features

- **Onboarding screen** shown at every launch with the owner's promotional video
  - First launch ever: video + **Subscribe popup card** + Continue
  - Later launches: video + simple Continue (subscribe popup never repeats)
  - Graceful fallback message + "Watch on YouTube" if the video is missing/unavailable
- Create, open, edit, autosave, delete, pin, archive, and share notes
- Debounced **autosave** with a visible `Saving… / Saved` indicator; pending edits are
  flushed when leaving the editor or closing the app
- Search across titles **and** content, fully offline
- Sort by recency or A–Z; list or 2-column grid layout; archived-notes filter
  (all persisted across launches)
- Pinned notes float to the top of the list
- Empty states for no notes, no search results, and empty archive
- Defensive data layer: a corrupt row is skipped, not fatal; storage errors surface
  as toasts/messages instead of crashes
- New notes left completely blank are cleaned up automatically (no junk drafts)
- **Every screen size & rotation**: single column on phones, extra columns on
  tablets/landscape, search query + scroll position survive rotation, and the promo
  video keeps playing through orientation changes

## Project structure

```
rube-note-pad/
├── build.gradle.kts                  # AGP/Kotlin plugin versions
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts              # module config & dependencies
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/promo/             # ← OWNER: place promo_video.mp4 here
        ├── java/com/rubenotepad/app/
        │   ├── OwnerConfig.kt        # ← OWNER: video source + channel URL
        │   ├── OnboardingActivity.kt # first-launch promo + subscribe popup
        │   ├── NotesActivity.kt      # notes list, search, sort, layout toggle
        │   ├── NoteAdapter.kt        # yellow sticky-note cards
        │   ├── EditorActivity.kt     # title/content editor + autosave
        │   └── data/
        │       ├── Note.kt           # model + relative time formatting
        │       ├── NotesDbHelper.kt  # SQLite schema
        │       ├── NoteRepository.kt # CRUD + search (crash-safe)
        │       └── AppPrefs.kt       # onboarding flag + UI preferences
        └── res/
            ├── layout/               # activity_notes, activity_editor,
            │                         # activity_onboarding, item_note
            ├── menu/menu_notes.xml
            ├── drawable/             # icons, lined paper background, launcher art
            ├── mipmap-anydpi-v26/    # adaptive launcher icon
            └── values/               # colors.xml, strings.xml, themes.xml
```

---

## Requirements

- **Android Studio** (Koala/2024.1 or newer recommended) — includes JDK 17
- Android SDK Platform **34** (Android Studio installs it automatically on sync)
- Minimum supported device: Android **8.0** (API 26)

## Build & run

1. Open Android Studio → **File ▸ Open…** → select the `rube note pad` folder.
2. Let Gradle sync finish (first sync downloads dependencies).
3. Press **Run ▶** with a device/emulator selected.

### Command-line build (APK)

```bash
# From Android Studio's terminal (or any shell with the SDK configured)
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Build the APK online (no Android Studio needed)

This repo includes a GitHub Actions workflow (`.github/workflows/build-apk.yml`):

1. Create a free GitHub account and a new repository (e.g. `rube-note-pad`).
2. Upload/push this whole folder to the repository.
3. Open the repo → **Actions** tab → wait ~5 minutes for **Build APK** to finish.
4. Open the finished run → **Artifacts** → download `RubeNotePad-debug-apk.zip`
   → extract `app-debug.apk` and install it on your phone.

You can also trigger a fresh build anytime via **Actions ▸ Build APK ▸ Run workflow**.

> If `./gradlew` reports the wrapper JAR is missing, run once from Android Studio
> (**File ▸ Sync Project with Gradle Files**) or execute `gradle wrapper --gradle-version 8.7`
> with a local Gradle install; Android Studio can also build the APK via
> **Build ▸ Build Bundle(s) / APK(s) ▸ Build APK(s)** without the wrapper.

### Release APK

```bash
./gradlew assembleRelease
```

Unsigned release APKs won't install; add your own signing config in
`app/build.gradle.kts` (`signingConfigs`) with your keystore before distribution.

---

## Owner configuration (one file)

Everything you may want to change lives in
**`app/src/main/java/com/rubenotepad/app/OwnerConfig.kt`**:

| Setting | Purpose |
|---|---|
| `PROMO_SOURCE` | `LOCAL_ASSET` (bundled video, works offline) or `YOUTUBE` (streams embed) |
| `PROMO_LOCAL_FILE` | Asset path of the bundled video |
| `PROMO_YOUTUBE_ID` | YouTube video ID used by the embedded player |
| `PROMO_WATCH_URL` | Fallback "Watch on YouTube" link |
| `CHANNEL_URL` | Channel opened by the Subscribe button |

Currently configured to your short: `youtube.com/shorts/zmgrXIhsBzU`
(`PROMO_SOURCE = YOUTUBE`, ID `zmgrXIhsBzU`).

### To replace the promotional video

**Option A – bundled (recommended, works offline):**
1. Put your edited video at `app/src/main/assets/promo/promo_video.mp4`.
2. In `OwnerConfig.kt` set `PROMO_SOURCE = PromoSource.LOCAL_ASSET`.
3. Rebuild the APK. If the file is missing, the app shows a graceful fallback and
   Continue still works.

**Option B – YouTube streaming:** just update `PROMO_YOUTUBE_ID` and
`PROMO_WATCH_URL` in `OwnerConfig.kt`. Viewers need internet once for this screen;
note-taking itself stays fully offline.

### To change the channel link

Edit `CHANNEL_URL` in `OwnerConfig.kt` (currently: `https://www.youtube.com/@RubeCoder`).

> The subscribe popup is **never faked**: it simply opens your channel page, and the
> user proceeds independently via Continue. Onboarding completion is stored in
> SharedPreferences (`onboarding_completed`), so it happens exactly once per install.

---

## Data storage

- Notes: private SQLite database `rube_note_pad.db` (app-internal storage).
- Preferences/onboarding state: `rube_note_pad_prefs` (SharedPreferences).
- Uninstalling the app (or clearing its data) removes all local data.
- The `INTERNET` permission exists solely for the optional remote promo video and the
  channel link — remove it from `AndroidManifest.xml` if you switch to a bundled video.

## Manual test checklist

- [ ] Fresh install → onboarding with video + subscribe popup → Continue → notes list
- [ ] Relaunch → onboarding shows again but **without** subscribe popup
- [ ] Create note → type title/content → "Saving…"/"Saved" indicator appears
- [ ] Leave editor mid-typing → reopen note → text preserved
- [ ] Force-close app → reopen → note still present
- [ ] Search matches titles and content
- [ ] Pin floats note to top; archive hides from main list, visible under "Show archived"
- [ ] Delete asks for confirmation and persists after restart
- [ ] Rotate while editing → same note, same content, no duplicate rows
- [ ] Rotate on notes list → search query and scroll position preserved
- [ ] Tablet / landscape → extra columns appear; promo video plays without restart
- [ ] Empty new note abandoned → does not appear as an "(Untitled)" leftover
- [ ] Missing/broken video asset → fallback message, Continue still works
