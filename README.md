# Longhand — Android shell

A WebView wrapper around `app/src/main/assets/longhand.html`. The HTML is the
whole game; this project exists only to give it an icon, a share sheet and a
place on the home screen.

## Getting an APK without installing anything

1. Create a new repository on GitHub. Private is fine.
2. Upload the contents of this folder to it — **Add file → Upload files**, then
   drag everything in. Keep the folder structure intact.
3. Go to the **Actions** tab and enable workflows if GitHub asks.
4. The build starts on its own. If it does not, open **Build APK** in the
   sidebar and press **Run workflow**.
5. Wait about three minutes. Open the finished run and download
   **longhand-apk** from the Artifacts section at the bottom.
6. Unzip it. **`longhand-release.apk`** is the one to put on your phone.
       `longhand-debug.apk` installs alongside it as a separate app for testing.

## Installing it

Transfer the APK to the phone and open it. Android will warn you, because it
warns about anything not from the Play Store:

- *"For your security, your phone is not allowed to install unknown apps from
  this source"* → **Settings** → allow the app you are installing from
  (usually Files or Chrome).
- Play Protect: **More details** → **Install anyway**. It says this about every
  unsigned app; it has not detected anything.

## Version pinning, and why

| Component | Version |
|---|---|
| Android Gradle Plugin | 8.10.1 |
| Gradle | 8.11.1 |
| JDK | 17 |
| compileSdk / targetSdk | 36 |
| minSdk | 26 (Android 8.0) |

AGP is held on the 8.x line on purpose. AGP 9 requires Gradle 9 and removes the
old DSL interfaces outright — that is the change that broke the Unpick build.
AGP 8.10 already supports API 36, so there is nothing to gain by moving. If
Android Studio offers to upgrade AGP for you, decline.

targetSdk 36 matters for a different reason: from 31 August 2026, Google Play
rejects new submissions and updates below API 36.

## Signing it properly

The debug-signed APK installs and plays, but the Play Store will not take it.
For a release build, make a keystore once on your own machine:

```
keytool -genkeypair -v -keystore release.keystore -alias longhand \
  -keyalg RSA -keysize 2048 -validity 10000
```

Then base64 it and add four repository secrets under
**Settings → Secrets and variables → Actions**:

```
base64 -w0 release.keystore        # macOS: base64 -i release.keystore
```

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | the base64 output |
| `KEYSTORE_PASSWORD` | store password you chose |
| `KEY_ALIAS` | `longhand` |
| `KEY_PASSWORD` | key password you chose |

Back the keystore up somewhere you will not lose it. Lose it and you can never
update the app on Play under the same identity.

## Updating the game

Replace `app/src/main/assets/longhand.html`, bump `versionCode` in
`app/build.gradle.kts`, commit. A new APK builds automatically.

## Known follow-ups

- **Fonts.** The HTML pulls Bricolage Grotesque, Inter Tight and Martian Mono
  from Google Fonts. Online this is fine; offline the app falls back to system
  faces and looks noticeably plainer. Subset them to A–Z plus digits, drop the
  `.woff2` files into `assets/`, and swap the `@font-face` rules to local paths.
- **Orientation on tablets.** API 36 ignores `screenOrientation="portrait"` at
  600dp and wider. The layout survives landscape because tile size is computed
  against both axes, but it has not been looked at properly.
