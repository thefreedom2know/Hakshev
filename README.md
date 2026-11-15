# Hakshev

פנקס מפקד דיגיטלי לעבודה בשטח: אפליקציית אנדרואיד (Java, XML, Room) שעובדת במצב לא מקוון.

Digital field notebook for squad / platoon commanders. Android app (Java + XML layouts, Room), offline-first.

## Package
`com.example.hakshev` · minSdk 26 · compileSdk 36

## Screens
| Screen | What it does |
|---|---|
| Dashboard | Headcount, average water, exceptions list, quick actions, WhatsApp report |
| Roster | Filter by squad / status, +/- water and status per soldier, bulk actions |
| Soldier profile | Personal, medical and exemptions (with expiry), equipment, discipline history |
| Hydration | Daily target, per-soldier progress, drink-parade log, automatic midnight reset |
| Discipline & duties | Accumulated exit hours, guard shifts with a 6h rest warning and fairness count |
| Inspections | Checklist templates, exception-based inspection, history |
| Settings | Squads, CSV import, day / tactical-night theme, PIN lock, auto-wipe |

## Field design
- Day theme is high contrast; night theme is pure black with dim red text.
- All action buttons are at least 56dp.
- No GPS, no background sync, no animations (battery).

## Security
- The database is encrypted at rest with SQLCipher (AES-256). The random passphrase is wrapped by an
  Android Keystore key, so the file is unreadable if copied off the device.
- Data is stored locally only (Room). Backups are disabled (`allowBackup=false`, no cloud transfer).
- PIN lock (salted SHA-256), re-lock after 30s in background, `FLAG_SECURE` blocks screenshots and recents preview.
- Optional auto-wipe after N days of inactivity, and a manual wipe button.
- Do not enter classified information (exact locations, training plans, classified equipment numbers).
  Store short descriptions only (e.g. "פטור עמידה"), never diagnoses or ID numbers.

## Not implemented yet
- Cloud sync and multi-commander roles (squad / sergeant / platoon commander) with merge-based conflict resolution.
- Biometric unlock.
- Excel (.xlsx) import: export to CSV UTF-8 first.

## Build
Open the folder in Android Studio, or:

```
./gradlew assembleDebug
```
