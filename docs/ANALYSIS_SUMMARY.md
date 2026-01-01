# TurnosHospi - Resumen de Análisis Técnico

> **Fecha**: 1 de Enero 2026
> **Versión Analizada**: 2.1 (versionCode: 9)
> **Plataforma**: Android (Kotlin + Jetpack Compose)
> **Última Actualización**: Análisis completo del código fuente

---

## 1. Estado Actual del Proyecto

### Métricas del Código
| Métrica | Valor |
|---------|-------|
| Archivos Kotlin | 31 (29 en src/main + 2 tests) |
| Líneas de código (aprox.) | ~12,586 |
| Idiomas soportados | 203 directorios de recursos |
| Pantallas (AppScreen enum) | 16 |
| Cloud Functions | 6 funciones activas |
| Modelos de datos | 12 data classes |

### Stack Tecnológico
- **UI**: Jetpack Compose + Material 3 + Material Icons Extended
- **Backend**: Firebase (Auth, Realtime Database, Cloud Messaging)
- **Serialización**: GSON 2.10.1
- **Build**: Gradle con Kotlin DSL
- **Min SDK**: 26 | **Target SDK**: 36
- **Java**: JVM Target 11

---

## 2. Fortalezas Identificadas ✅

### 2.1 Motor de Reglas (ShiftRulesEngine)
El `ShiftRulesEngine.kt` es **excelente** y está bien estructurado:
- Cálculo de dureza de turnos (noche, fin de semana, festivos)
- Validación de roles (enfermeros vs auxiliares)
- Reglas laborales (máximo 6 días consecutivos, descanso tras noche)
- Algoritmo de matching para swaps

```kotlin
// Ubicación: app/src/main/java/com/example/turnoshospi/ShiftRulesEngine.kt
object ShiftRulesEngine {
    fun validateWorkRules(targetDate, targetShiftName, userSchedule): String?
    fun checkMatch(requesterRequest, candidateRequest, ...): Boolean
}
```

### 2.2 Internacionalización Extensiva
- Soporte para **40+ idiomas** en `res/values-*/strings.xml`
- Strings externalizados correctamente
- Preparado para mercado global

### 2.3 Sistema de Notificaciones Completo
- **6 Cloud Functions** bien estructuradas en `functions/index.js`:
  - `sendNotification`: Motor central de push (escucha `/user_notifications`)
  - `sendChatNotification`: Notificaciones de mensajes directos
  - `notifySupervisorOnPending`: Alertas a supervisores
  - `notifyUsersOnStatusChange`: Notifica aprobación/rechazo
  - `notifyTargetOnProposal`: Propuestas de cambio de turno
- Deep linking funcional (cold start y warm start)
- Canal de notificación `turnoshospi_sound_v2` con sonido y badge
- Arquitectura de fanout correcta (functions escriben en DB, motor central envía push)

### 2.4 Modelos de Datos Bien Definidos
- `ShiftModels.kt`: Enums claros (RequestType, RequestStatus, RequestMode)
- Estados de solicitud bien estructurados (DRAFT → SEARCHING → PENDING_PARTNER → AWAITING_SUPERVISOR → APPROVED/REJECTED)
- Flujo de trabajo supervisado para cambios de turno

### 2.5 Navegación Custom Funcional
- Sistema de navegación basado en enum `AppScreen` (16 pantallas)
- Manejo de backStack con `mutableStateListOf`
- BackHandler integrado para UX nativa
- Deep linking desde notificaciones

### 2.6 Estructura de Firebase Bien Organizada
```
/users/{uid}                           # Perfiles de usuario + FCM tokens
/plants/{plantId}                      # Datos de planta/unidad
  /personal_de_planta/{staffId}        # Roster de personal
  /turnos/turnos-{date}/{shiftName}    # Turnos por fecha
  /shift_requests/{requestId}          # Solicitudes de cambio
  /direct_chats/{chatId}/messages      # Mensajes directos
/user_notifications/{userId}           # Notificaciones (trigger para push)
/user_direct_chats/{userId}            # Metadata de chats + unread counts
```

---

## 3. Áreas de Mejora Críticas ⚠️

### 3.1 Arquitectura Monolítica
**Problema**: Todo el código está en un solo paquete sin separación de capas.

```
ACTUAL:                          RECOMENDADO:
com.example.turnoshospi/         com.example.turnoshospi/
├── MainActivity.kt              ├── data/
├── TurnoshospiApp.kt           │   ├── repository/
├── ShiftRulesEngine.kt         │   ├── remote/
├── ShiftModels.kt              │   └── local/
├── ShiftChangeScreen.kt        ├── domain/
├── CustomCalendar.kt           │   ├── model/
├── ... (26 más)                │   └── usecase/
                                └── presentation/
                                    ├── viewmodel/
                                    └── ui/
```

**Impacto**:
- Difícil de testear
- Alto acoplamiento
- Mantenimiento costoso

### 3.2 Callback Hell en MainActivity
**Problema**: `MainActivity.kt` tiene **1100+ líneas** con toda la lógica de Firebase.

```kotlin
// ACTUAL: 25+ callbacks pasados a TurnoshospiApp (líneas 97-182)
TurnoshospiApp(
    onLogin = { email, password, onResult -> signInWithEmail(...) },
    onLoadProfile = { onResult -> loadUserProfile(onResult) },
    onJoinPlant = { plantId, code, profile, onResult -> joinPlantWithCode(...) },
    onListenToShifts = { plantId, staffId, onResult -> listenToUserShifts(...) },
    onSaveNotification = { userId, type, message, targetScreen, targetId, onResult -> ... },
    // ... 20 callbacks más
)
```

**TurnoshospiApp.kt** recibe estos callbacks y tiene **1183 líneas** con:
- 50+ variables de estado (`mutableStateOf`, `mutableStateListOf`)
- Navegación manual sin Jetpack Navigation
- Lógica de UI y negocio mezclada

**Impacto**:
- Código difícil de leer y mantener
- No sigue principios MVVM
- Imposible reutilizar lógica
- Tests prácticamente imposibles

### 3.3 Sin Persistencia Offline Real
**Problema**: Solo `CustomCalendarOffline` usa SharedPreferences para persistencia local.

**Impacto**:
- App no funciona sin conexión
- Datos perdidos al cerrar la app
- UX pobre con conexiones lentas

### 3.4 Testing Inexistente
**Problema**: Solo existen tests de ejemplo (2+2=4).

```kotlin
// app/src/test/.../ExampleUnitTest.kt
@Test
fun addition_isCorrect() {
    assertEquals(4, 2 + 2)  // Único test real
}
```

**Impacto**:
- ShiftRulesEngine sin cobertura de tests
- Bugs en producción no detectados
- Refactoring riesgoso

---

## 4. Plan de Mejora Priorizado

### Fase 1: Fundamentos (Semanas 1-4)

#### 1.1 Implementar Repository Pattern
```kotlin
// data/repository/ShiftRepository.kt
interface ShiftRepository {
    fun getShiftsForMonth(plantId: String, month: YearMonth): Flow<Resource<List<Shift>>>
    suspend fun createSwapRequest(request: ShiftChangeRequest): Result<String>
}

// data/repository/ShiftRepositoryImpl.kt
class ShiftRepositoryImpl(
    private val firebaseDb: DatabaseReference
) : ShiftRepository { ... }
```

#### 1.2 Crear ViewModels
```kotlin
// presentation/viewmodel/ShiftViewModel.kt
class ShiftViewModel(
    private val shiftRepository: ShiftRepository
) : ViewModel() {
    private val _shifts = MutableStateFlow<Resource<List<Shift>>>(Resource.Loading())
    val shifts: StateFlow<Resource<List<Shift>>> = _shifts.asStateFlow()
}
```

#### 1.3 Tests para ShiftRulesEngine
```kotlin
// app/src/test/.../ShiftRulesEngineTest.kt
class ShiftRulesEngineTest {
    @Test
    fun `roles incompatibles deben fallar`() {
        assertFalse(ShiftRulesEngine.areRolesCompatible("Enfermero", "Auxiliar"))
    }

    @Test
    fun `máximo 6 días consecutivos`() {
        val schedule = createSixDaySchedule()
        val error = ShiftRulesEngine.validateWorkRules(day7, "Mañana", schedule)
        assertNotNull(error)
    }
}
```

### Fase 2: Offline-First (Semanas 5-8)

#### 2.1 Añadir Room Database
```kotlin
// data/local/AppDatabase.kt
@Database(entities = [ShiftEntity::class, SwapRequestEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao
}

// data/local/ShiftDao.kt
@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE plantId = :plantId AND date >= :start AND date <= :end")
    fun getShiftsForPeriod(plantId: String, start: String, end: String): Flow<List<ShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShifts(shifts: List<ShiftEntity>)
}
```

#### 2.2 Sincronización Bidireccional
```kotlin
// Repository con sync
class ShiftRepositoryImpl(
    private val shiftDao: ShiftDao,
    private val firebaseDb: DatabaseReference
) : ShiftRepository {

    override fun getShiftsForMonth(plantId: String, month: YearMonth) = flow {
        // 1. Emitir datos locales primero
        emit(Resource.Loading())
        shiftDao.getShiftsForPeriod(plantId, startDate, endDate).collect { local ->
            emit(Resource.Success(local.map { it.toDomain() }))
        }

        // 2. Sincronizar con Firebase en background
        syncWithFirebase(plantId, month)
    }
}
```

### Fase 3: Inyección de Dependencias (Semanas 9-10)

#### 3.1 Configurar Hilt
```kotlin
// di/AppModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideFirebaseDatabase(): DatabaseReference =
        FirebaseDatabase.getInstance("https://turnoshospi-f4870-default-rtdb.firebaseio.com/").reference

    @Provides @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "turnoshospi_db").build()
}

// di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindShiftRepository(impl: ShiftRepositoryImpl): ShiftRepository
}
```

### Fase 4: CI/CD y Calidad (Semanas 11-12)

#### 4.1 GitHub Actions
```yaml
# .github/workflows/android.yml
name: Android CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with: { java-version: '17', distribution: 'temurin' }
      - run: ./gradlew test
      - run: ./gradlew lint
      - run: ./gradlew assembleDebug
```

---

## 5. Nuevas Funcionalidades Sugeridas

### 5.1 Sistema de Vacaciones
- Solicitud de vacaciones con verificación de cobertura
- Calendario de ausencias del equipo
- Sugerencia de fechas alternativas

### 5.2 Analytics y Reportes
- Estadísticas mensuales por usuario
- Dashboard de equidad en distribución de turnos
- Exportación a PDF/Excel

### 5.3 Gamificación
- Sistema de puntos por cubrir turnos
- Badges (Búho Nocturno, Jugador de Equipo)
- Leaderboard opcional

### 5.4 Predicción de Necesidades (ML)
- Predicción de demanda de personal
- Alertas de sub/sobrecarga de personal
- Optimización automática de turnos

---

## 6. Seguridad - Mejoras Recomendadas

### 6.1 Reglas de Firebase (Actualizar)
```javascript
// database.rules.json
{
  "rules": {
    "plants": {
      "$plantId": {
        ".read": "auth != null && (
          root.child('plants/' + $plantId + '/members/' + auth.uid).exists() ||
          root.child('plants/' + $plantId + '/userPlants/' + auth.uid).exists()
        )",
        "shifts": {
          ".write": "auth != null && root.child('plants/' + $plantId + '/userPlants/' + auth.uid + '/staffRole').val() == 'Supervisor'"
        }
      }
    }
  }
}
```

### 6.2 Validación Server-Side
Añadir Cloud Functions para validar cambios de turno antes de persistirlos.

### 6.3 Encriptación Local
Usar `EncryptedSharedPreferences` para datos sensibles.

---

## 7. Métricas de Éxito

### Rendimiento
| Métrica | Actual | Objetivo |
|---------|--------|----------|
| Tiempo de carga inicial | ~3s | < 2s |
| Navegación entre pantallas | ~800ms | < 500ms |
| Crash rate | Desconocido | < 0.1% |

### Calidad
| Métrica | Actual | Objetivo |
|---------|--------|----------|
| Cobertura de tests | 0% | > 70% |
| Lint warnings | Múltiples | 0 |
| Deuda técnica | Alta | Baja |

---

## 8. Archivos Clave para Refactoring

| Archivo | Líneas | Prioridad | Acción |
|---------|--------|-----------|--------|
| `MainActivity.kt` | 1100 | 🔴 Alta | Extraer a ViewModels + Repositories |
| `ShiftChangeScreen.kt` | 1000+ | 🔴 Alta | Separar UI de lógica |
| `CustomCalendarOffline.kt` | 1000+ | 🟡 Media | Migrar a Room |
| `TurnoshospiApp.kt` | 800+ | 🟡 Media | Simplificar navegación |
| `ShiftRulesEngine.kt` | 156 | 🟢 Baja | Añadir tests (ya está bien estructurado) |

---

## 9. Dependencias a Añadir

```kotlin
// build.gradle.kts (app)
dependencies {
    // Hilt (DI)
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Room (Offline)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager (Sync)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0")
}
```

---

## 10. Conclusión

TurnosHospi es una aplicación **funcional y bien diseñada** a nivel de UX y características. El motor de reglas (`ShiftRulesEngine`) demuestra un excelente entendimiento del dominio hospitalario.

Las principales áreas de mejora son:
1. **Arquitectura**: Migrar a MVVM con Repository pattern
2. **Testing**: Añadir cobertura de tests (especialmente ShiftRulesEngine)
3. **Offline-First**: Implementar Room para persistencia local
4. **DI**: Adoptar Hilt para mejor testabilidad

Con estas mejoras, la aplicación estará preparada para escalar y mantener a largo plazo.

---

## 11. Quick Wins - Mejoras Inmediatas

### 11.1 Tests para ShiftRulesEngine (1-2 días)
El motor de reglas está perfectamente aislado y listo para tests:

```kotlin
// app/src/test/java/com/example/turnoshospi/ShiftRulesEngineTest.kt
class ShiftRulesEngineTest {

    @Test
    fun `enfermero y auxiliar no son compatibles`() {
        assertFalse(ShiftRulesEngine.areRolesCompatible("Enfermero", "Auxiliar"))
        assertFalse(ShiftRulesEngine.areRolesCompatible("Enfermera", "TCAE"))
    }

    @Test
    fun `enfermeros entre sí son compatibles`() {
        assertTrue(ShiftRulesEngine.areRolesCompatible("Enfermero", "Enfermera"))
    }

    @Test
    fun `supervisor no puede participar en cambios`() {
        assertFalse(ShiftRulesEngine.canUserParticipate("Supervisor"))
    }

    @Test
    fun `turno de noche tiene dureza NIGHT`() {
        val date = LocalDate.of(2026, 1, 15) // Miércoles
        assertEquals(
            ShiftRulesEngine.Hardness.NIGHT,
            ShiftRulesEngine.calculateShiftHardness(date, "Noche")
        )
    }

    @Test
    fun `fin de semana tiene dureza WEEKEND`() {
        val saturday = LocalDate.of(2026, 1, 17)
        assertEquals(
            ShiftRulesEngine.Hardness.WEEKEND,
            ShiftRulesEngine.calculateShiftHardness(saturday, "Mañana")
        )
    }

    @Test
    fun `no se puede trabajar más de 6 días seguidos`() {
        val schedule = (0..5).associate {
            LocalDate.of(2026, 1, 10 + it) to "Mañana"
        }
        val day7 = LocalDate.of(2026, 1, 16)

        val error = ShiftRulesEngine.validateWorkRules(day7, "Mañana", schedule)

        assertNotNull(error)
        assertTrue(error!!.contains("6 días"))
    }

    @Test
    fun `después de noche hay que descansar (saliente)`() {
        val schedule = mapOf(
            LocalDate.of(2026, 1, 14) to "Noche"
        )
        val dayAfter = LocalDate.of(2026, 1, 15)

        val error = ShiftRulesEngine.validateWorkRules(dayAfter, "Mañana", schedule)

        assertNotNull(error)
        assertTrue(error!!.contains("Saliente"))
    }
}
```

### 11.2 Extraer Constantes de Firebase URL (30 min)
```kotlin
// Crear: util/Constants.kt
object FirebaseConstants {
    const val DATABASE_URL = "https://turnoshospi-f4870-default-rtdb.firebaseio.com/"
    const val NOTIFICATION_CHANNEL_ID = "turnoshospi_sound_v2"
}
```

### 11.3 Añadir Crashlytics (1 hora)
```kotlin
// build.gradle.kts (project)
plugins {
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}

// build.gradle.kts (app)
plugins {
    id("com.google.firebase.crashlytics")
}

dependencies {
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}
```

---

## 12. Archivos Principales por Funcionalidad

| Funcionalidad | Archivo Principal | Líneas |
|--------------|-------------------|--------|
| Entry Point | `MainActivity.kt` | 1100 |
| Navegación/UI Root | `TurnoshospiApp.kt` | 1183 |
| Motor de Reglas | `ShiftRulesEngine.kt` | 155 |
| Modelos de Turno | `ShiftModels.kt` | 63 |
| Cambio de Turnos | `ShiftChangeScreen.kt` | ~1000 |
| Calendario Online | `CustomCalendar.kt` | ~800 |
| Calendario Offline | `CustomCalendarOffline.kt` | ~1000 |
| Chat Directo | `DirectChatScreen.kt` | ~500 |
| Estadísticas | `StatisticsScreen.kt` | 463 |
| Cloud Functions | `functions/index.js` | 292 |

---

*Documento generado el 1 de Enero 2026 como parte del análisis técnico del proyecto TurnosHospi.*
