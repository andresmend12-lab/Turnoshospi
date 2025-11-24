# Configuración de Firebase para Turnoshospi

Sigue estos pasos para que el flujo de alta funcione con tu proyecto de Firebase:

1. **google-services.json**: descarga el archivo desde Firebase Console (Configuración del proyecto → tus apps Android) y cópialo en `app/google-services.json`.
2. **Client ID de OAuth**: el archivo anterior genera automáticamente el valor `default_web_client_id`. Si quieres probar sin el plugin, también puedes remplazar el texto de ejemplo en `app/src/main/res/values/strings.xml` con el *client ID* web de tu proyecto.
3. **Proveedor Google habilitado**: asegúrate de que el método de inicio de sesión de Google esté activo en `Authentication → Sign-in method` y que el dominio de tu app esté autorizado.
4. **Firestore**: crea la base de datos en modo production. Se guardará cada perfil en `users/{uid}` usando el UID de Firebase Auth.

## Reglas de seguridad sugeridas para Firestore

Estas reglas permiten que cada usuario lea/escriba únicamente su propio perfil y mantienen en modo sólo lectura el resto de colecciones por defecto:

```rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /{document=**} {
      allow read: if false;
      allow write: if false;
    }
  }
}
```

Publica las reglas desde la consola o con la CLI de Firebase antes de distribuir la app.
