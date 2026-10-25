# aulix

App Android, sin conexión, para el seguimiento de clases: lo visto en cada sesión, ideas para las siguientes y horario semanal. La especificación está en `# App Android para el seguimiento de clases.md`.

## Cada curso, de cero

En **Ajustes → Empezar nuevo curso** un asistente guarda una copia del curso que termina, pide los módulos del curso nuevo y su horario de lunes a viernes (6 horas al día; las horas seguidas del mismo módulo forman una sola clase) y, al confirmar, borra todo lo anterior y crea el curso nuevo. Una instalación nueva empieza vacía y lleva directamente a ese asistente. El curso antiguo se puede consultar restaurando su copia.

## Compilar

Requiere JDK 17 y el SDK de Android (plataforma 34). La ruta del SDK va en `local.properties`, que no se sube al repositorio.

```bash
./gradlew testDebugUnitTest assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/aulix-<versión>-debug.apk`.

## Versiones

La versión es `MAJOR.MINOR.PATCH` y está en `version.properties`. Se cambia con:

```bash
./scripts/bump-version.sh patch   # actualización menor: 1.0.1 -> 1.0.2
./scripts/bump-version.sh minor   # actualización mayor: 1.0.2 -> 1.1.0
./scripts/bump-version.sh major   # paso a 2.x, solo cuando se decida
```

`VERSION_CODE` sube en 1 cada vez, como exige Android para actualizar.

Cada versión publicada:

1. Se desarrolla en una rama y entra en `main` mediante un PR con commit de merge, sin squash, para poder revertir cada cambio por separado.
2. Se etiqueta en `main` con `vX.Y.Z` y la etiqueta se sube a GitHub.

## Volver a una versión anterior

Las etiquetas `v*` marcan cada versión, tanto en local como en GitHub.

- Ver o compilar una versión antigua sin tocar `main`:

  ```bash
  git switch --detach v1.0.0
  ./gradlew assembleDebug
  git switch main
  ```

- Deshacer en `main` todos los cambios de una versión, dejando constancia en el historial: revertir su commit de merge.

  ```bash
  git revert -m 1 <commit-de-merge>
  ```

- Deshacer un solo arreglo: `git revert <commit>`.

Si el APK de una versión antigua tiene un `VERSION_CODE` menor que el instalado, Android no lo instala encima. Para volver a esa versión en el móvil: exportar la copia de seguridad desde Ajustes, desinstalar, instalar el APK antiguo y restaurar la copia.

## Credenciales

Ninguna credencial se sube al repositorio. El envío del APK por Telegram (`scripts/send-apk-telegram.sh`) lee el token de `~/.config/aulix/telegram.env`.
