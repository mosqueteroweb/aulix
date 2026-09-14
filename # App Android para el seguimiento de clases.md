# App Android para el seguimiento de clases  
  
## Resumen  
  
Una app minimalista, en español y sin conexión, que al abrir muestre las clases del día, por dónde te quedaste en cada asignatura y tus ideas para futuras sesiones. Sin cuentas ni sincronización.  
  
Cada sigla corresponde a una asignatura con un historial compartido entre todos sus días.  
  
## Pantallas y funcionamiento  
  
**Hoy**  
  
- Fecha y navegación al día anterior, siguiente y vuelta a hoy.  
- Clases ordenadas según el horario.  
- Cada clase muestra la última anotación de una sesión anterior, con su fecha; permite desplegar las tres últimas y abrir el historial completo.  
- Campo «Lo visto en clase» para la fecha seleccionada, editable y con guardado automático local.  
- Acceso a las ideas pendientes de esa asignatura.  
- Los días sin clases muestran «No hay clases programadas» y acceso a las asignaturas.  
  
**Asignatura**  
  
- Dos apartados: «Registro» e «Ideas».  
- Registro por fecha, del más reciente al más antiguo. Se pueden añadir anotaciones para fechas pasadas, editarlas y eliminarlas con confirmación.  
- Ideas en texto libre, independientes del calendario. Se pueden crear, editar y marcar como utilizadas; las utilizadas quedan consultables y se pueden reactivar.  
- Las ideas no pasan automáticamente al registro: utilizarlas no implica haberlas impartido.  
  
**Ajustes**  
  
- Editar nombres de asignaturas y horario semanal, añadiendo, quitando y reordenando sesiones.  
- Exportar e importar una copia completa.  
- Archivar asignaturas sin borrar sus anotaciones.  
  
## Horario inicial y decisiones técnicas  
  
Se incluirán únicamente estos bloques de la imagen, considerando cada bloque continuo una sesión:  
  
| Día | Sesiones, en orden |  
|---|---|  
| Lunes | DI, AFH, SI |  
| Martes | SI, ASI, DI |  
| Miércoles | SI, DI |  
| Jueves | SI, ASI |  
| Viernes | AFH, SI |  
  
- Aplicación nativa con Kotlin, Jetpack Compose y base de datos local Room.  
- Datos separados en asignaturas, sesiones semanales, registros fechados e ideas con estado pendiente/utilizada.  
- Una anotación por asignatura y fecha, ampliable durante el día. Los cambios de horario no modificarán registros anteriores.  
- Fecha según el calendario local del móvil. Sin horas exactas en esta versión: basta el orden de las sesiones.  
- Exportación a un archivo JSON versionado mediante el selector de archivos de Android. Incluirá horario, asignaturas, registros e ideas.  
- Importación con validación previa y confirmación explícita: sustituirá los datos locales, ofreciendo exportarlos antes. Una importación fallida conservará los datos existentes.  
- Entrega inicial como APK instalable en tu Android; publicar en Google Play queda fuera de esta versión.  
  
## Validación y límites de la primera versión  
  
Se comprobará que:  
  
- Cada día muestra las asignaturas correctas y en orden.  
- SI comparte historial entre martes, miércoles, lunes y viernes.  
- Las anotaciones e ideas sobreviven al cierre y reapertura sin conexión.  
- Editar el horario o archivar asignaturas conserva el historial.  
- Marcar una idea como utilizada y reactivarla funciona.  
- Exportar y restaurar reproduce todos los datos; los archivos inválidos no alteran la información.  
- La pantalla permite leer y escribir cómodamente con el teclado abierto y tamaños de letra grandes.  
  
La primera versión usará texto libre, sin adjuntos, avisos, seguimiento de alumnos ni gestión de festivos. La copia de seguridad será manual: para protegerla frente a la pérdida del móvil, habrá que guardar el archivo fuera del dispositivo.  
