<h1 align="center">Bambua</h1>

<p align="center"><b>Your money, clear and in order.</b><br>
A personal finance app for Android: simple to use, complete, offline and open source.
Bambua is a fork of <a href="https://github.com/H3yK0/cryo">Cryo</a> by Hanry Franco (H3yK0),
renamed and adapted to the Bambua brand.</p>

<p align="center">
  <b>English</b> ·
  <a href="docs/readme/README.pt-BR.md">Português</a> ·
  <a href="docs/readme/README.es.md">Español</a> ·
  <a href="docs/readme/README.fr.md">Français</a> ·
  <a href="docs/readme/README.de.md">Deutsch</a> ·
  <a href="docs/readme/README.it.md">Italiano</a> ·
  <a href="docs/readme/README.ja.md">日本語</a> ·
  <a href="docs/readme/README.ko.md">한국어</a> ·
  <a href="docs/readme/README.zh-CN.md">中文</a> ·
  <a href="docs/readme/README.hi.md">हिन्दी</a>
</p>

> The 9 language READMEs above are inherited from the Cryo upstream (kept as reference).
> Attribution: based on Cryo by Hanry Franco (H3yK0), licensed AGPL-3.0. See [NOTICE](NOTICE).

## What it does

- **Log by typing or speaking.** Type or say "lunch 12.50 on debit" or "tv 500 in 10 installments on credit card". Bambua understands the amount, the category, the account, the date and the installments. You just check and save.
- **End-of-month forecast.** See how much should be left at your current pace and how much you can spend per day.
- **Accounts and transfers.** Cash, banks and wallets in one place, with transfers between accounts (the "caja" of v0.1).
- **Export CSV** in a fixed format (the interface with the Coach system): `Fecha;Tipo;Descripción;Categoría;Cuenta;Destino;Tarjeta;Cuota;Importe;Nota;Deuda`.
- **Import bank statements.** OFX, CSV, spreadsheet (XLSX) or PDF: you review the list before anything goes in.
- **Consent on first launch** (Colombian Law 1581 habeas data): the onboarding explains that all data stays on the phone and asks for explicit consent, with a "colombian tone" toggle (close-neutral by default).
- **Saldito** (placeholder): coming soon, zero logic and zero network in v0.1.
- **Credit cards, debts, planning, budgets, goals and investments**, all inherited from Cryo 1.4.
- **10 languages and your currency.** English, Portuguese, Spanish, French, German, Italian, Japanese, Korean, Chinese and Hindi.
- **Material You**, light and dark themes and smooth animations.

## Privacy

- Your data stays on your phone. **The app has no internet permission.**
- No sign-up, no ads, no tracking, no analytics.
- Lock with fingerprint, face or your phone's screen lock.
- **Automatic backup** to a folder on your phone or to your cloud, daily, weekly or monthly.
- Bambua organizes, analyzes and recommends habits. **It does not grant credit and does not replace an accountant or financial advisor.**

## Build from source

Requirements: JDK 21 and the Android SDK (platform 36).

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK (unsigned without a keystore)
./gradlew testDebugUnitTest  # automated tests
```

Package: `com.bambua.app`. License: [AGPL-3.0](LICENSE) — the source code of Bambua must stay
public under AGPL-3.0. To sign with your own key, create `keystore/keystore.properties` with
`storeFile`, `storePassword`, `keyAlias` and `keyPassword` (never published).

### How the code is organized

| Folder | Contents |
|---|---|
| `domain/` | Rules without Android: balances, forecast, money flow, the phrase interpreter and the Coach CSV export (`CoachCsv.kt`) |
| `importer/` | Statement reading (OFX, CSV, XLSX, PDF) |
| `data/` | Database (Room), settings, backup, default categories |
| `ui/` | Screens in Jetpack Compose with Material 3 (onboarding consent, Saldito placeholder) |
| `notify/` | Reminders (WorkManager) |

The upstream project documents (PRD, visual guide, statement import) are in [docs/](docs), written in Portuguese.

## License

Copyright © 2026 Hanry Franco (original Cryo code). Modifications © 2026 the Bambua operator.

This program is free software: you can redistribute it and/or modify it under the terms of the
[GNU Affero General Public License](LICENSE) as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version. It is distributed in the hope
that it will be useful, but **without any warranty**.
