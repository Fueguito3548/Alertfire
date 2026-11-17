# Desafio Alert

Mod de **Forge 1.20.1** (Java 17). Añade el comando `/aviso` que lanza en la pantalla de todos los jugadores una animación con fuegos artificiales, explosión, fuego teñido, sonidos y un mensaje en el chat, pensado para anunciar cambios de balance en un evento.

## Comando

```
/aviso <tipo> <mensaje>      (requiere permiso de operador, nivel 2)
```

| tipo | título en el chat | color |
|---|---|---|
| `info` | INFORMACIÓN | verde |
| `dificultad` | CAMBIO DE DIFICULTAD | naranja (fuego original) |
| `mob` | NUEVO MOB | rojo |
| `jugador` | AVISO | amarillo |
| `mision` | MISIÓN DIARIA | azul |
| `expedicion` | EXPEDICIÓN | rosa |
| `final` | EVENTO ESPECIAL | morado (+ invencibilidad) |

Mensaje máximo: 300 caracteres. Si llegan varios avisos, se encolan.

## Línea de tiempo de la animación (14.5 s)

- 0.3 s – 7.3 s: fuegos artificiales de intro, cada vez más seguidos
- 7.5 s: explosión + destello del color del tipo
- 8.5 s: sube el fuego de pantalla (textura `fire_1.png` de Minecraft recoloreada en tiempo de ejecución)
- 9.2 s: el mensaje aparece en el chat

## Configuración

- **Servidor/común** (`desafioalert-common.toml`): invencibilidad del aviso `final` (segundos y retraso) y, por cada uno de los 5 sonidos (`chispas`, `explosion`, `explosion_capa2`, `fuego`, `mensaje`), el id del sonido y su volumen.
- **Cliente** (`desafioalert-client.toml`): `tamanoFuego` (A/B/C/D), `alturaFuego`, `bajarFuego` y `volumen` general.

## Estructura del código

```
com.desafioalert
├── DesafioAlert      clase principal, registra configs y red
├── AlertType         tipos de aviso (id, título, color, tono)
├── AlertConfig       configuración (common + client)
├── AvisoCommand      comando /aviso
├── ServerTasks       programador de tareas por ticks
├── anim/             lógica de la animación SIN dependencias de Minecraft
│   ├── Anim          genera comandos de dibujo en cada instante
│   ├── Layout        distribuciones A/B/C/D del fuego
│   └── Tint          recoloreo HSV de la textura de fuego
├── client/           ClientAnim (cola, sonidos, dibujo), FireTextures, eventos
└── net/              AlertPacket + NetworkHandler
```

## Compilar

Requiere JDK 17.

```
./gradlew build
```

El `.jar` queda en `build/libs/`. Para probarlo en desarrollo: `./gradlew runClient`.

> **Gradle wrapper:** este repo no incluye `gradlew`, `gradlew.bat` ni `gradle/wrapper/`.
> Cópialos del MDK oficial de Forge 1.20.1 (https://files.minecraftforge.net) o genéralos con
> `gradle wrapper --gradle-version 8.1.1`.

## Origen de este código

El código fuente original se perdió y este repositorio se **reconstruyó a partir del `.jar` compilado** (v1.1.0).
La lógica de `anim/` se verificó contra el bytecode original: con las mismas semillas produce exactamente los mismos
dibujos. Los comentarios y nombres de variables son reconstruidos.
