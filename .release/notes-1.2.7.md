# Rezka TV 1.2.7 — BETA update

Signed production update from main for Android Phone and Android TV.
Application ID: dev.alaev.rezkatv (no .qa suffix). VersionCode: 22 (previous production: 21).
Requires Android 9 / API 28 or newer; target SDK 35 and supported ABIs unchanged.

## Changes

- TV / Touch mode selection and persisted preference.
- TV poster focus, OK to open details, voice search and D-pad Up from the keyboard to the microphone.
- Server-backed favorite collections and explicit add-to-collection action, including portrait details.
- Replace the absent IMDb rating with «Лучшие», the yearless /films/best/ catalog, using existing cards and pagination.
- Restore the production application label and validate release/update compatibility before publication.

## Validation and limitations

Quality gates, Kotlin compilation, unit tests, Detekt, Android Lint and signed release build are required by the release workflow.
Package ID, increasing versionCode, production certificate, minimum/target SDK and ABI are compared against the previous production APK before publication.
Signed preflight: certificate matched v1.2.6; versionCode 21 -> 22.
DEVICE QA NOT TESTED: an actual in-place installation, preserved login/settings/data and TV/Phone runtime were not tested in this session.
Live Best HTML parsing and favorites loading/adding remain unverified; the account was not modified during development.
No playback, cookie storage or local data-format migration is introduced. This does not substitute for an on-device preservation test.

## Update

Use the application's update check or download the attached production APK and install it over the existing production app.
No uninstall is required by this update procedure. QA builds are separate applications and are not converted into production installations.
If Android reports a signing/package conflict, stop; do not uninstall or clear application data.
Keep the existing installation and report the error. v1.2.6 remains available, but Android may reject a downgrade and data rollback has not been tested.
