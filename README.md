# 💰 GestiónFácil

**App Android de finanzas personales** — Proyecto Final de Ciclo (TFC), Grado Superior en Desarrollo de Aplicaciones Multiplataforma (DAM), CESUR Cáceres.

GestiónFácil ayuda al usuario a controlar sus ingresos y gastos diarios, fijar presupuestos por categoría, definir metas de ahorro y visualizar su situación financiera de un vistazo, todo con una interfaz moderna basada en Material Design 3.

---

## ✨ Funcionalidades principales

- **Registro de movimientos**: ingresos y gastos organizados por categoría y fecha.
- **Presupuestos**: definición de límites de gasto mensuales por categoría, con seguimiento visual del consumo.
- **Metas de ahorro**: creación de objetivos financieros y seguimiento del progreso mediante aportaciones asociadas (`MovimientoMeta`).
- **Dashboard**: resumen visual del estado financiero del usuario (balance, gastos por categoría, evolución).
- **Bloqueo biométrico**: acceso a la app protegido con huella dactilar / reconocimiento facial.
- **Exportación a PDF**: generación de informes descargables con el histórico de movimientos.
- **Gamificación**: sistema de logros/recompensas para fomentar buenos hábitos financieros.

## 🛠️ Stack técnico

| Área | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Arquitectura | MVVM |
| Estado reactivo | Kotlin Flow / StateFlow |
| Persistencia | Room (base de datos local SQLite) |
| Seguridad | Biometric Authentication API |
| Exportación | Generación de PDF nativa |
| Gestión de dependencias | Gradle (Kotlin DSL) |

## 🗄️ Modelo de datos

Base de datos Room con las siguientes entidades principales:

- `Movimiento` — ingresos y gastos individuales.
- `Categoria` — categorías de clasificación de movimientos.
- `Presupuesto` — límites de gasto definidos por el usuario.
- `Meta` — objetivos de ahorro.
- `MovimientoMeta` — relación entre movimientos y metas de ahorro.

La base de datos incluye control de versiones y migraciones (`version = 2`) para la evolución del esquema sin pérdida de datos.

## 🏗️ Arquitectura

El proyecto sigue el patrón **MVVM (Model-View-ViewModel)**:

```
UI (Jetpack Compose Screens)
      ↓ observa
ViewModel (StateFlow)
      ↓ consume
Repository
      ↓ accede a
Room Database (DAOs)
```

Cada pantalla (`Screen`) tiene asociado su propio `ViewModel`, que expone el estado mediante `StateFlow` y gestiona la lógica de negocio desacoplada de la UI.

## 📱 Capturas de pantalla

_(Añadir aquí capturas del Dashboard, registro de movimientos, presupuestos y metas)_

## 🚀 Cómo ejecutar el proyecto

1. Clona el repositorio:
   ```bash
   git clone https://github.com/valennbg23/GestionFacil.git
   ```
2. Ábrelo con **Android Studio** (versión recomendada: la más reciente estable).
3. Sincroniza las dependencias de Gradle.
4. Ejecuta en un emulador o dispositivo físico con Android 8.0 (API 26) o superior.

## 👤 Autor

**Valentín Borreguero González**
Desarrollador DAM — Android (Kotlin/Jetpack Compose) · Web (Next.js/TypeScript)

- GitHub: [@valennbg23](https://github.com/valennbg23)
- Email: valentinborreguerogonzalez@gmail.com

---

*Proyecto desarrollado como Trabajo de Fin de Ciclo del Grado Superior en Desarrollo de Aplicaciones Multiplataforma (DAM) — CESUR Cáceres, 2024-2026.*

