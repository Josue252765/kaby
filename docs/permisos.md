# Permisos incluidos en Kaby

Estado: [CONSTRUCTOR] base M1; cotejado contra el APK `debug` con `aapt` y `tools/verify-android-permissions.py` el 2026-10-21 18:20 America/Caracas. La lista debe volver a cotejarse al incorporar dependencias o hitos M4.

| Permiso | Origen | Por qué existe | Pantalla/función | Si se quita |
|---|---|---|---|---|
| `android.permission.INTERNET` | Kaby | Consultar espejos de tasa por HTTPS al volver a la app, entrar en Tasas y mediante refresco diario de mejor esfuerzo. | Tasas BCV | Las consultas externas fallan; la entrada manual sigue disponible. |
| `android.permission.ACCESS_NETWORK_STATE` | WorkManager | Evaluar restricciones de red como `NetworkType.CONNECTED` del refresco diario de tasas. | Refresco diario de Tasas | WorkManager no puede evaluar esa condición; la entrada manual y el refresco interactivo siguen disponibles. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | WorkManager | Reprogramar los trabajos persistentes de tasa y copia local después de reiniciar Android. | Refresco diario y copia local privada | Los trabajos pueden no reprogramarse tras reiniciar; los intentos al abrir siguen disponibles. |
| `android.permission.WAKE_LOCK` | WorkManager | Ejecutar sus workers breves mientras Android despacha el trabajo en segundo plano. | Refresco diario y copia local privada | WorkManager puede no completar el trabajo mientras el dispositivo duerme. |
| `com.mofoxx.kaby.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX Core | Permiso `signature` generado por AndroidX para restringir el uso de receptores dinámicos no exportados; no pide confirmación runtime. | Infraestructura AndroidX | AndroidX pierde esa barrera declarativa de sus receptores dinámicos. |

`FOREGROUND_SERVICE` lo agrega WorkManager aunque Kaby M1 no crea trabajos de primer plano; el manifest de la app lo quita y el APK final no lo declara. Tampoco se declaran aún permisos de notificaciones ni alarmas exactas (función M4), contactos, archivos compartidos ni cámara. `tools/verify-android-permissions.py` compara los usos de permisos del APK con esta tabla; el manifest final de cada variante se inspecciona de nuevo al cerrar el hito.
