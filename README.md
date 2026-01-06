# TurnosHospi

Aplicacion Android para la gestion de turnos hospitalarios.

## Configuracion del proyecto

### Requisitos previos

- Android Studio
- JDK 11 o superior
- Cuenta de Firebase

### Configuracion de Firebase

Este proyecto utiliza Firebase. Para configurarlo correctamente:

1. **Crea un proyecto en Firebase Console**
   - Ve a [Firebase Console](https://console.firebase.google.com/)
   - Crea un nuevo proyecto o usa uno existente

2. **Registra tu aplicacion Android**
   - En Firebase Console, haz clic en "Agregar app" y selecciona Android
   - Usa el package name: `com.andresmendoza.turnoshospi`
   - Descarga el archivo `google-services.json`

3. **Configura el archivo google-services.json**
   - Copia el archivo `google-services.json` descargado a la carpeta `app/`
   - Tambien puedes copiarlo a `app/src/` si es necesario
   - **IMPORTANTE**: Este archivo contiene claves sensibles y NO debe ser commiteado al repositorio

4. **Archivo de ejemplo**
   - Puedes ver la estructura esperada en `app/google-services.json.example`
   - Este archivo solo contiene placeholders y no valores reales

### Compilacion

```bash
./gradlew build
```

### Ejecucion

Abre el proyecto en Android Studio y ejecuta en un emulador o dispositivo fisico.

## Seguridad

Los siguientes archivos contienen informacion sensible y estan excluidos del control de versiones:

- `app/google-services.json`
- `app/src/google-services.json`
- `local.properties`

**Nunca compartas estos archivos ni los subas a repositorios publicos.**
