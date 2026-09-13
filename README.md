# Auto-Polish Pipeline

Dieses Repository bündelt das geprüfte Android-Studio-Projekt von Auto‑Polish und baut bei jeder Änderung automatisch eine Debug-APK.

## Lokaler Aufbau

Das Android-Projekt liegt unter `android/`. Voraussetzungen: Java 17, Gradle 8.9 und Android SDK 35.

## GitHub Actions

Der Workflow `Android CI` prüft die Java-Quellen, baut die APK und stellt sie als Workflow-Artefakt bereit. Der Build verwendet ausschließlich den Debug-Schlüssel und ist nicht für eine Veröffentlichung signiert.
