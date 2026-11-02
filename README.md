# TecnoMovil Data – Procesamiento funcional de datos de transporte urbano

Actividad **EA2. Creación de repositorio** – Institución Universitaria Digital de Antioquia.
Autora: Ana Valentina Correcha.

Módulo en **Java puro (sin frameworks)** que procesa registros simulados del sistema de
transporte de TecnoValle usando programación funcional: funciones puras, inmutabilidad,
lambdas, funciones de orden superior, composición y Streams (secuenciales y paralelos).

## Requerimientos resueltos
| Req. | Descripción | Método |
|------|-------------|--------|
| a | Afluencia por estación | `AnalizadorTransporte.afluenciaPorEstacion()` |
| b | Horas pico | `flujoPorHora()`, `horasPico(n)` |
| c | Rutas más utilizadas | `rutasMasUtilizadas()` |
| d | Patrones de viaje por usuario | `patronesDeViaje()`, `patronesMasFrecuentes(n)` |
| e | Tiempo promedio entre estaciones | `tramos()`, `tiempoPromedioPorTramo()`, `tiempoPromedioGeneral()` |
| f | Sobrecarga de rutas | `detectarSobrecarga(catalogo, umbral)`, `rutasCriticas(...)` |

## Estructura
```
src/co/edu/iudigital/tecnomovil/
├── Main.java                  # Única capa con efectos (consola y medición de tiempo)
├── PruebasFuncionales.java    # Pruebas con datos conocidos (sin JUnit)
├── modelo/                    # Records inmutables: Registro, Ruta, Tramo, EstadoRuta, Accion
├── datos/                     # CatalogoRutas y GeneradorDatos (generación pura y reproducible)
├── servicio/                  # AnalizadorTransporte (requerimientos a–f) y Funciones (HOF)
└── util/                      # Formato: funciones puras que convierten resultados en texto
```

## Requisitos
JDK 17 o superior.

## Compilar y ejecutar
Windows: `compilar_y_ejecutar.bat`  ·  Linux/macOS: `./compilar_y_ejecutar.sh`

O manualmente:
```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out co.edu.iudigital.tecnomovil.PruebasFuncionales
java -cp out co.edu.iudigital.tecnomovil.Main            # 200.000 usuarios, umbral 85 %
java -cp out co.edu.iudigital.tecnomovil.Main 500000 0.9 # parámetros opcionales
```
En IntelliJ / NetBeans / Eclipse: abrir la carpeta, marcar `src` como carpeta de fuentes
y ejecutar `Main`.
