# Bambua Base — guía del fork (para cualquier asistente de código)

> **Fuente de verdad de alcance:** `PROMPT-FORK.md` en el repo raíz del operador
> (`/home/padmin/Bambua/PROMPT-FORK.md`) y `AGENTS.md` junto a él. Este archivo describe el
> código; el alcance y las prohibiciones viven en PROMPT-FORK. Desviación → reportar, no improvisar.

## 1. Qué es

Fork de **Cryo 1.4** (Hanry Franco, H3yK0, https://github.com/H3yK0/cryo, AGPL-3.0) renombrado a
**Bambua**. App Android de finanzas personales, Kotlin + Jetpack Compose (Material 3), Room,
WorkManager, 10 idiomas (base pt-BR del upstream, es incluido), todo offline.

- Package: `com.bambua.app` (debug: `com.bambua.app.debug`).
- Remote `upstream` = https://github.com/H3yK0/cryo (conservado).
- Licencia: **AGPL-3.0-or-later, intacta**. Headers SPDX `AGPL-3.0-or-later` + copyright en cada
  `.kt`: **no borrar el copyright de Hanry Franco** al modificar archivos; agregar la línea de
  modificación debajo si se desea.
- Restos intencionales de "cryo" en el código: atribución (URL del upstream), compatibilidad de
  migración (backups JSON "Cryo" aceptados en `Backup.import`, campo `cryoImport` del importador).

## 2. Addiciones propias del fork (v0.1)

| Pieza | Dónde |
|---|---|
| Onboarding con consentimiento Ley 1581 + toggle de tono | `ui/screens/Onboarding.kt` (paso CONSENT), `data/Settings.kt` (`consentAccepted`, `colombianTone`) |
| Export CSV formato Coach | `domain/CoachCsv.kt` (puro, testeable) + botón en `ui/screens/SettingsScreens.kt` |
| Pantalla Saldito (placeholder, sin lógica ni red) | `ui/screens/SalditoScreen.kt`, ruta `Routes.SALDITO` |
| Acerca de con atribución y no-asesoría | sección "Sobre" de `SettingsScreens.kt` |

## 3. Build y tests

```bash
./gradlew assembleDebug        # JDK 21, compileSdk 36
./gradlew assembleRelease      # sin keystore: APK unsigned
./gradlew testDebugUnitTest    # Robolectric + Roborazzi en modo record (regenera screenshots)
```

`PrdConformanceTest` recorre el código y exige: mismas keys de strings en los 10 idiomas, mismos
marcadores, headers de licencia, sin permisos extra. Al tocar strings o permisos, correrlo.

## 4. Reglas del fork

1. Feature branch (`feature/bambua-*`); nada directo a la rama principal.
2. Prohibido: red, analytics, trackers, gamificación, paywall, facturación (app personal).
3. El export CSV es interfaz oficial con el Coach: **no variar un carácter** del formato.
4. Español en comunicación y en los strings del fork; los otros 9 idiomas siguen el catálogo del
   upstream (claves idénticas).
5. El repo raíz del operador (`/home/padmin/Bambua`) guarda PROMPT-FORK.md y AGENTS.md; este
   subdirectorio `bambua/` es el repo git del fork.
