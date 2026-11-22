# CallFleet — consent-based call-log fleet manager

CallFleet is a **transparent, consent-first** call-history manager for phones you
own or manage. Each enrolled phone shows a disclosure screen, requires explicit
agreement, and runs a **permanent notification** while it is being monitored. Its
call history syncs to a Firebase backend, and a web **admin dashboard** lets you
view stats, call logs and (where the device permits) recordings across all your
devices, with CSV / HTML / PDF export.

It is deliberately **not** covert monitoring software. If you need to watch a phone
without the user's knowledge, this is the wrong tool — and on modern Android that
capability is both blocked by the OS and, for call recording, often illegal.

---

## What works, and what doesn't

| Feature | Status |
|---|---|
| Read this device's own call log (number, name, direction, time, duration) | ✅ Reliable |
| Sync call log to Firebase, view per-device in dashboard | ✅ Reliable |
| Stats + CSV / HTML / PDF export from dashboard | ✅ Reliable |
| Permanent "this device is monitored" notification | ✅ Reliable |
| **Call audio recording** | ⚠️ **Device-dependent.** Android 10+ blocks third-party apps from capturing call audio. Works on some OEM builds (older/Xiaomi/Samsung), returns an honest "not supported" state elsewhere. The app never tries to bypass the OS block. |

---

## 1. Create the Firebase project

1. Go to <https://console.firebase.google.com> → **Add project**.
2. In the project, open **Build → Firestore Database** → *Create database* (production mode).
3. Open **Build → Authentication** → *Get started* → enable **Anonymous**
   (every phone and the dashboard sign in anonymously; the fleet code gates access).
4. Open **Build → Storage** → *Get started* (used later if you enable recording upload).

## 2. Wire the Android app to Firebase

1. In Firebase Console → **Project settings → Your apps → Add app → Android**.
2. Package name: `com.callfleet.app`.
3. Download the generated **`google-services.json`** and drop it into the `app/`
   folder (replacing `app/google-services.json.template`).

## 3. Deploy the security rules

1. **Firestore → Rules**: paste `firestore.rules`, Publish.
2. **Storage → Rules**: paste `storage.rules`, Publish.
   (Or `firebase deploy --only firestore:rules,storage` with the Firebase CLI.)

No admin UID to set — access is gated by the **fleet code** (see "How login works").

## 4. Build the APK

You need **Android Studio** (Hedgehog or newer). The sandbox that generated this
project can't compile a signed APK — Android Studio does that in one click.

1. **File → Open** → select this `callfleet/` folder. Let Gradle sync.
2. Build a test APK: **Build → Build Bundle(s)/APK(s) → Build APK(s)**.
   The APK lands in `app/build/outputs/apk/debug/`.
3. For a shareable/release APK, create a signing key
   (**Build → Generate Signed Bundle / APK**) and follow the wizard.
4. Install on each phone (enable "Install unknown apps" for the file manager).

## 5. Enrol a phone

Open the app → enter your **fleet code** (the same code on every phone you want to
share one dashboard) → tap **Enrol this device** → read the disclosure → name the
device → **Agree** → grant permissions. The persistent notification appears and the
call log starts syncing within ~1 minute.

## 6. Open the shared dashboard

Same dashboard, **two ways**:

- **Inside the app.** On any phone with the app, enter the fleet code and tap
  *Open shared dashboard*. It loads the embedded panel (bundled at
  `app/src/main/assets/admin-dashboard.html`) and **auto-connects** — the app hands it
  the Firebase config and fleet code natively, so there's no login or pasting.
  Exports save into the phone's Downloads folder; recordings play inline.
- **In a browser.** Open the standalone `admin-dashboard.html`, paste your Firebase
  **web config** (Project settings → Your apps → Web app) and the fleet code.

Both show the same pooled data: an **All devices** view at the top combines stats and
logs across every phone in the fleet, and you can drill into any single device. A
**Demo mode** runs with sample data until you connect.

## How login works

There is **no username/password**. Access is keyed to a shared **fleet code**:

- Every phone (whether it's being monitored or just viewing the dashboard) signs in to
  Firebase **anonymously** and reads/writes under `fleets/{yourCode}`.
- Put the **same fleet code** on every device you want to share one dashboard. That's
  what makes two phones show the same pooled stats.
- **Security trade-off:** because there's no login, the fleet code *is* the password.
  Anyone who knows it (and is signed in) can read that fleet's data, so **choose a
  long, non-obvious code**. If you need stronger control, switch back to an admin
  email/password login and gate reads by UID — the previous rules did this, and the
  code structure still supports it.

## Does recording actually work?

The recorder is now wired to the phone's call state: when a call goes active it
starts (`CallStateMonitor` → `CallRecorder`), and on hang-up it stops, uploads to
Firebase Storage, and links the file on that call in the dashboard.

**But**: Android 10+ blocks ordinary apps from capturing call audio. On most modern
phones `MediaRecorder.start()` for a call source either fails or records silence, and
the app records nothing and marks the device *recording not supported* — this is the
OS protecting call privacy, and the app does not try to defeat it. It genuinely works
only on some older or OEM builds. Test on your actual target phones before relying on
it. Deploy `storage.rules` (set your admin UID) to secure uploaded recordings.

---

## Firestore data shape

```
fleets/{fleetId}/devices/{deviceId}
  label, model, androidVersion, recordingSupported,
  consentAcceptedAt, lastSeen, totalCalls

fleets/{fleetId}/devices/{deviceId}/calls/{callId}
  number, name, type, timestamp, durationSec, recordingUrl

Storage: recordings/{fleetId}/{deviceId}/rec_*.m4a
```

## Legal

Recording or monitoring calls may require the consent of everyone on the call,
depending on your country/state. You are responsible for lawful use. CallFleet is
built to keep the monitored user informed; keep it that way.

## Project layout

```
callfleet/
  app/
    src/main/
      AndroidManifest.xml
      java/com/callfleet/app/
        CallFleetApp.kt           app-wide state, device id, consent flag
        data/                     models, call-log reader, Firebase sync
        service/                  foreground service + recorder (best-effort)
        ui/                       consent screen, main screen, list adapter
      res/                        layouts, strings, icons
    google-services.json.template rename after download
  firestore.rules                 security rules (set your admin UID)
  README.md
```
