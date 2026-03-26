# Dynamic Call Blocker (Android)

An Android call-screening app focused on advanced blocking rules.

## Features
- Block list with both **exact match** and **starts with** entries.
- Allow list with both **exact match** and **starts with** entries.
- Allow-list entries are exceptions to block-list entries.
- Toggle to allow contacts through block rules (default: enabled).
- Uses Android's call-screening role; does **not** provide caller ID overlays or replace the dialer UI.

## Blocking behavior
Incoming number is blocked only when:
1. It matches a block rule (exact or prefix), and
2. It does not match an allow rule (exact or prefix), and
3. It is not in contacts when "Allow contacts" is enabled.

## Build
```bash
./gradlew assembleDebug
```

## Notes
- Minimum SDK is 29 (Android 10) because this app relies on `ROLE_CALL_SCREENING` role APIs.
- The app prompts for call-screening role and contacts permission on first launch.


## Automated releases
- Pushing a tag that matches `v*` (for example `v1.0.0`) triggers GitHub Actions to build a release APK and attach it to a GitHub Release.
- If signing secrets are configured (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), the workflow signs the APK before attaching it.
