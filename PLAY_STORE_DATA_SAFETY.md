# Data Safety Declaration for Google Play Console

Este documento contiene toda la información que necesitas para completar la sección **Data Safety** en Google Play Console para TurnosHospi.

---

## Resumen de Datos Recopilados

### 1. Información Personal

| Tipo de Dato | Recopilado | Compartido | Propósito |
|--------------|------------|------------|-----------|
| **Nombre** | Sí | Sí (con usuarios de la misma planta) | Funcionalidad de la app, Gestión de cuenta |
| **Email** | Sí | No | Funcionalidad de la app, Gestión de cuenta |
| **Género** | Sí (opcional) | No | Personalización (Enfermero/a) |

### 2. Identificadores

| Tipo de Dato | Recopilado | Compartido | Propósito |
|--------------|------------|------------|-----------|
| **User ID** | Sí | Sí (Firebase) | Funcionalidad de la app |
| **Device ID** | Sí | Sí (Firebase) | Analytics, Notificaciones |

### 3. Actividad de la App

| Tipo de Dato | Recopilado | Compartido | Propósito |
|--------------|------------|------------|-----------|
| **Interacciones con la app** | Sí | Sí (Firebase Analytics) | Analytics |
| **Historial de búsqueda en la app** | No | No | - |
| **Contenido generado por el usuario** | Sí | Sí (con usuarios de la misma planta) | Funcionalidad de la app |

### 4. Info del dispositivo

| Tipo de Dato | Recopilado | Compartido | Propósito |
|--------------|------------|------------|-----------|
| **Registros de fallos** | Sí | Sí (Firebase Crashlytics) | Analytics, Diagnóstico |
| **Diagnósticos** | Sí | Sí (Firebase Crashlytics) | Analytics |

---

## Preguntas del Formulario Data Safety (Respuestas)

### Sección 1: Recopilación de Datos

**¿Tu app recopila o comparte algún tipo de dato requerido del usuario?**
- [x] **Sí**

**¿Todos los datos del usuario recopilados por tu app están cifrados en tránsito?**
- [x] **Sí** (Firebase usa TLS/SSL)

**¿Ofreces una manera para que los usuarios soliciten la eliminación de sus datos?**
- [x] **Sí** (Botón "Eliminar cuenta" en Ajustes)

---

### Sección 2: Tipos de Datos

#### Personal Info
- [x] **Name** - Nombre y apellidos del usuario
  - Recopilado: Sí
  - Compartido: Sí (con otros usuarios de la misma planta hospitalaria)
  - Procesamiento: Transferido fuera del dispositivo
  - Propósito: Funcionalidad de la app, Gestión de cuenta
  - Opcional: No

- [x] **Email address**
  - Recopilado: Sí
  - Compartido: No
  - Procesamiento: Transferido fuera del dispositivo
  - Propósito: Funcionalidad de la app, Gestión de cuenta
  - Opcional: No

#### App activity
- [x] **App interactions**
  - Recopilado: Sí
  - Compartido: Sí (Firebase Analytics - anonimizado)
  - Propósito: Analytics
  - Opcional: Sí

- [x] **Other user-generated content** (Mensajes de chat, turnos)
  - Recopilado: Sí
  - Compartido: Sí (con usuarios de la misma planta)
  - Propósito: Funcionalidad de la app
  - Opcional: No

#### Device or other IDs
- [x] **Device or other IDs**
  - Recopilado: Sí (FCM Token)
  - Compartido: Sí (Firebase)
  - Propósito: Funcionalidad de la app (notificaciones push)
  - Opcional: No

#### App info and performance
- [x] **Crash logs**
  - Recopilado: Sí
  - Compartido: Sí (Firebase Crashlytics)
  - Propósito: Analytics
  - Opcional: Sí

- [x] **Diagnostics**
  - Recopilado: Sí
  - Compartido: Sí (Firebase Crashlytics)
  - Propósito: Analytics
  - Opcional: Sí

---

### Sección 3: Propósitos de los Datos

| Propósito | Aplica | Datos |
|-----------|--------|-------|
| **Funcionalidad de la app** | Sí | Nombre, Email, Mensajes, Turnos, FCM Token |
| **Analytics** | Sí | Interacciones, Crash logs, Diagnósticos |
| **Comunicación con el desarrollador** | Sí | Email (para soporte) |
| **Publicidad o marketing** | No | - |
| **Prevención de fraude y seguridad** | No | - |
| **Personalización** | Sí | Género (para términos Enfermero/a) |
| **Gestión de cuenta** | Sí | Nombre, Email |

---

### Sección 4: Seguridad

**¿Los datos se cifran en tránsito?**
- [x] **Sí** - Firebase usa HTTPS/TLS

**¿Los datos se cifran en reposo?**
- [x] **Sí** - Firebase cifra los datos almacenados

**¿Los usuarios pueden solicitar la eliminación de sus datos?**
- [x] **Sí** - Botón "Eliminar cuenta" en Ajustes > Gestionar Cuenta

---

## Proveedores de Terceros

### Firebase (Google)

| Servicio | Datos Procesados | Política de Privacidad |
|----------|------------------|------------------------|
| Firebase Authentication | Email, Password (hash), UID | https://firebase.google.com/support/privacy |
| Firebase Realtime Database | Nombre, Email, Turnos, Mensajes | https://firebase.google.com/support/privacy |
| Firebase Cloud Messaging | FCM Token, Device ID | https://firebase.google.com/support/privacy |
| Firebase Crashlytics | Crash logs, Device info | https://firebase.google.com/support/privacy |
| Firebase Analytics | App interactions (anonimizado) | https://firebase.google.com/support/privacy |

---

## Checklist Final para Play Console

Antes de publicar, asegúrate de:

- [ ] Completar el formulario Data Safety en Play Console
- [ ] Subir la URL de tu política de privacidad
- [ ] Verificar que el botón "Eliminar cuenta" funcione correctamente
- [ ] Configurar el email de contacto para soporte: turnoshospi@gmail.com
- [ ] Verificar que las reglas de Firebase Database requieran autenticación
- [ ] Probar el flujo de eliminación de cuenta end-to-end

---

## URLs Importantes

| Item | URL/Valor |
|------|-----------|
| **Política de Privacidad** | (Debes crear una web y poner la URL aquí) |
| **Email de Soporte** | turnoshospi@gmail.com |
| **Desarrollador** | Andrés Mendoza |

---

## Notas sobre AD_ID

Tu app tiene el permiso `com.google.android.gms.permission.AD_ID` declarado en el manifest.

**En el formulario de Data Safety, deberás declarar:**
- ¿Usas el Advertising ID? **No** (si no muestras publicidad)
- Si lo usas solo para Analytics, marca: "Analytics"

Si NO usas publicidad, considera remover este permiso del AndroidManifest.xml:
```xml
<!-- Puedes eliminar esta línea si no usas publicidad -->
<uses-permission android:name="com.google.android.gms.permission.AD_ID"/>
```

---

*Documento generado para TurnosHospi v3.1*
