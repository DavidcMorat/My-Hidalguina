# My Hidalguina

**My Hidalguina** es una plataforma educativa integral diseñada para conectar a estudiantes y docentes. La aplicación facilita la comunicación, el seguimiento del aprendizaje y provee potentes herramientas impulsadas por Inteligencia Artificial para mejorar la experiencia académica. Todo construido con tecnologías modernas de desarrollo nativo en Android (Kotlin y Jetpack Compose).

## 🚀 Características Principales

### Para Estudiantes 👨‍🎓
*   **Dashboard Académico**: Visualiza anuncios y tareas organizadas por grado, sección y curso.
*   **Tutor IA**: Asistente inteligente integrado para resolver dudas académicas 24/7.
*   **Plan de Estudio con IA**: Genera planes de estudio personalizados e interactivos impulsados por Google Gemini para dominar cualquier tema de forma estructurada.
*   **Progreso y Gamificación**: Sistema de seguimiento y puntos (logros) para mantener la motivación.

### Para Docentes 👩‍🏫
*   **Gestión de Contenido**: Crea y asigna tareas o material de estudio para diferentes grados y secciones.
*   **Comunicados Oficiales**: Publica anuncios importantes en tiempo real para mantener informada a la clase.

### Sistema de Comunicación (Chat) 💬
*   **Mensajería Directa**: Chat en tiempo real entre la comunidad educativa.
*   **Soporte Offline**: El historial de chat se almacena localmente usando Room Database, permitiendo revisar mensajes sin conexión a internet.
*   **Stickers y Giphy**: Integración nativa de Giphy para enviar stickers y GIFs, enriqueciendo la conversación.

## 🛠️ Tecnologías Utilizadas

*   **Lenguaje**: [Kotlin](https://kotlinlang.org/)
*   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) con directrices de Material Design 3.
*   **Arquitectura**: Clean Architecture y patrón MVVM (Model-View-ViewModel) garantizando escalabilidad.
*   **Backend as a Service (BaaS)**: [Firebase](https://firebase.google.com/) (Firestore para la base de datos en tiempo real y Authentication).
*   **Almacenamiento Local**: [Room Database](https://developer.android.com/training/data-storage/room) para caché persistente de mensajes y perfilado.
*   **Inteligencia Artificial**: [Gemini API](https://ai.google.dev/) (Para el Tutor y generación de planes de estudio).
*   **Recursos Multimedia**: [Giphy API](https://developers.giphy.com/) (Para animaciones en el chat).
*   **Asincronía**: Kotlin Coroutines & StateFlow.

## 🔒 Manejo de Claves y Seguridad (Secrets)

Para mantener la seguridad del repositorio, las claves de las APIs (`GEMINI_API_KEY`, `GIPHY_API_KEY`) **no se encuentran en el código fuente**.
La aplicación utiliza el [Secrets Gradle Plugin for Android](https://github.com/google/secrets-gradle-plugin), que lee las claves desde un archivo `.env` en la raíz del proyecto y las inyecta de forma segura a través de la clase `BuildConfig` durante la compilación.

### ¿Cómo compilar localmente?
1. Clona este repositorio.
2. Crea un archivo `.env` en la raíz (puedes basarte en `.env.example`).
3. Agrega tus claves:
   ```env
   GEMINI_API_KEY=tu_clave_de_gemini
   GIPHY_API_KEY=tu_clave_de_giphy
   ```
4. Compila el proyecto desde Android Studio.

---
✨ *Una herramienta diseñada para llevar la educación al siguiente nivel tecnológico.*
