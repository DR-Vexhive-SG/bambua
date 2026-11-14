# Bambua Base v0.1

<p align="center"><b>Bambua</b> — tu dinero, claro y en orden.</p>

**Bambua Base** es un fork de **[Cryo](https://github.com/H3yK0/cryo)** de Hanry Franco (H3yK0),
renombrado y ajustado a la marca Bambua. Finanzas personales en Android: simple, completo,
**offline** y de código abierto.

- **Atribución obligatoria:** basado en Cryo de Hanry Franco (H3yK0). Ver [`NOTICE`](NOTICE).
- **Licencia:** AGPL-3.0 (ver [`LICENSE`](LICENSE)). El código fuente de Bambua se publica en un
  repositorio público conforme a la AGPL-3.0.
- **Privacidad:** sin permiso de internet, sin analytics, sin trackers. Todos los datos viven en el
  teléfono. El consentimiento (Ley 1581 de habeas data, Colombia) se pide en el onboarding.
- **V0.1:** registro de movimientos, cuentas y transferencias (caja), export CSV, onboarding con
  consentimiento y placeholder de la futura función "Saldito".

## Marca

- Nombre: **Bambua** (sin tilde; "bambua" = riqueza en jerga colombiana).
- Package: `com.bambua.app`.
- Colores: `#0A2540` (azul profundo), `#1FA36B` (verde bambú), `#6A4C93` (orquídea), `#F2A900` (ámbar).

## Build

```bash
./gradlew assembleDebug      # APK debug
./gradlew assembleRelease    # APK release
./gradlew testDebugUnitTest  # pruebas unitarias
```

Requisitos: JDK 21, Android SDK (compileSdk 36, build-tools 35/36), Gradle wrapper 8.14.3.

## Export CSV

El export CSV (`Ajustes › Exportar CSV`) usa un formato fijo, interfaz oficial con el sistema
Coach. Cabecera exacta:

```
Fecha;Tipo;Descripción;Categoría;Cuenta;Destino;Tarjeta;Cuota;Importe;Nota;Deuda
```

Separador `;`, UTF-8 sin BOM, decimales con coma, fechas `YYYY-MM-DD`. Las transferencias se
exportan como dos filas (Egreso en la cuenta origen, Ingreso en la destino).

---

La funcionalidad heredada (registro por frase/voz, importación de extractos OFX/CSV/XLSX/PDF,
tarjetas de crédito, deudas, presupuestos, metas, inversiones, backup automático, 10 idiomas) es
la de Cryo 1.4; su documentación completa está en [`docs/`](docs) y en el historial del upstream.
Créditos del proyecto original: ver [`CHANGELOG.md`](CHANGELOG.md) y la historia de commits de
Cryo. Este fork conserva la licencia AGPL-3.0 sin excepciones.
