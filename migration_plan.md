# Plan de Migración a Firebase y Persistencia Local

Este plan detalla los pasos necesarios para transformar el proyecto de un backend basado en Spring Boot a uno basado íntegramente en Firebase, con almacenamiento local para chats y contactos.

## 1. Configuración de Dependencias (Gradle)
Añadiremos las librerías necesarias para Firebase (KMP) y almacenamiento local (SQLDelight).

### Dependencias Firebase KMP:
Utilizaremos `GitLive Firebase SDK` que soporta Android y Desktop (vía JVM).
- `dev.gitlive:firebase-auth`
- `dev.gitlive:firebase-firestore`
- `dev.gitlive:firebase-storage` (si se requiere para media)

### Dependencias de Base de Datos Local:
- `app.cash.sqldelight:gradle-plugin`
- `app.cash.sqldelight:android-driver`
- `app.cash.sqldelight:sqlite-driver` (para Desktop)

## 2. Autenticación con Google (Firebase Auth)
- **Android:** Integración nativa con Google Sign-In SDK + Firebase Auth.
- **Desktop:** Implementación de flujo OAuth2 vía navegador para obtener las credenciales de Firebase.
- **Shared Logic:** Un `AuthRepository` que maneje el estado de la sesión usando Firebase Auth.

## 3. Descubrimiento de Usuarios y Conexión
- **Firestore:** Se usará una colección `users` para el descubrimiento.
- Cada usuario publicará su perfil (nombre, email, foto) al loguearse.
- Búsqueda de contactos mediante consultas a Firestore.

## 4. Chats y Mensajería (Tiempo Real)
- **Firestore:** Colección `chats` y subcolección `messages`.
- **Estrategia Sincronizada:**
  - Los mensajes se envían a Firestore.
  - El cliente escucha cambios en Firestore.
  - Al recibir un mensaje, se guarda inmediatamente en la base de datos local (SQLDelight).
  - La UI solo lee de la base de datos local para garantizar el funcionamiento offline y rapidez.

## 5. Estructura de Datos Local (SQLDelight)
Definiremos tablas para:
- `Contact`: id, name, email, photoUrl.
- [Chat](file:///C:/Users/Niko/Desktop/chat-master/composeApp/src/commonMain/kotlin/org/nko/chat/services/ChatService.kt#15-68): id, participantId, lastMessage.
- [Message](file:///C:/Users/Niko/Desktop/chat-master/composeApp/src/commonMain/kotlin/org/nko/chat/services/ChatService.kt#37-65): id, chatId, senderId, text, timestamp, status.

## 6. Pasos de Implementación
1.  **Limpieza:** Eliminar [AuthService.kt](file:///C:/Users/Niko/Desktop/chat-master/composeApp/src/commonMain/kotlin/org/nko/chat/services/AuthService.kt), [ChatService.kt](file:///C:/Users/Niko/Desktop/chat-master/composeApp/src/commonMain/kotlin/org/nko/chat/services/ChatService.kt) y [ChatWebSocketService.kt](file:///C:/Users/Niko/Desktop/chat-master/composeApp/src/commonMain/kotlin/org/nko/chat/services/ChatWebSocketService.kt) que apuntan al backend antiguo.
2.  **Configuración de Firebase:** Añadir `google-services.json` (Android) y configuración manual para Desktop.
3.  **Implementación de SQLDelight:** Crear el esquema `.sq` y generar los drivers.
4.  **Refactor de Repositorios:**
    *   `UserRepository`: Maneja Auth y perfil en Firestore.
    *   `ContactRepository`: Busca en Firestore y guarda en local.
    *   `ChatRepository`: Sincroniza Firestore con la DB local.
5.  **Actualización de UI:** Adaptar los ViewModels para observar los flujos de datos de los nuevos repositorios.
