# HtmlPad

A simple **HTML-only** notepad for Android.

Made for one person who wants a calm place to write HTML — not a full IDE.

## Download

Grab the latest signed APK from the [Releases page](https://github.com/wized2/HtmlPad/releases/latest).

## Features

- Notepad-like paper UI
- Symbol bar above the keyboard: `!` `<` `>` `</` `"` `/` and common tags (the cursor lands between the open and close tags)
- **Auto-close tags** — type `<h2>` and get `<h2></h2>` with the cursor in the middle
- **Autosave** — your text is restored when you come back
- **Open / Save / Save as** `.html` files with the system file picker
- **Share** the HTML as text
- **Preview** the page in a built-in WebView (offline; no network permission)
- Landscape toggle for a wide notepad feel
- Works offline, no account, no tracking

## Build

```bash
./gradlew assembleDebug
./gradlew test
```

## Releases (CI)

Every push to `main` runs `.github/workflows/release.yml`, which runs the tests,
builds a **signed** release APK and publishes it as a GitHub release
(`v1.0.<run number>`; the run number is also the Android `versionCode`, so each
APK can update the previous one).

The workflow needs these repository secrets
(Settings → Secrets and variables → Actions):

| Secret | What it is |
| --- | --- |
| `KEYSTORE_BASE64` | the release keystore, base64 encoded (`base64 -w0 htmlpad-release.jks`) |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias inside the keystore |
| `KEY_PASSWORD` | key password |

Keep a backup of the keystore: future updates must be signed with the same key.
It is never committed to the repo. Without the secrets, local release builds are
simply left unsigned.

## License

MIT
