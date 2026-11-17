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

- 0.0 s – 7.3 s: **musiquita de fondo** (arpegio que sube de grave a agudo) y fuegos artificiales de intro, cada vez más seguidos. Cada tanda de fuegos suena con una **nota de "carga"** que también sube de grave a agudo.
- 7.5 s: explosión + destello del color del tipo + **"tan tan tan taaan"** electrónico (3 golpes y uno largo)
- 8.5 s: sube el fuego de pantalla (textura `fire_1.png` recoloreada) y aparece un **difuminado sutil del color del aviso en el borde de la pantalla**
- 9.2 s: el mensaje aparece en el chat con una **notificación retro** de 3 notas (~1 s)
- ~19 s: el borde se desvanece cuando el chat oculta el mensaje

## Configuración

- **Servidor/común** (`desafioalert-common.toml`): invencibilidad del aviso `final` (segundos y retraso) y, por cada uno de los 9 sonidos (`chispas`, `explosion`, `explosion_capa2`, `fuego`, `notificacion`, `carga`, `musica`, `suspenso_golpe`, `suspenso_largo`), el id del sonido y su volumen.
- **Cliente** (`desafioalert-client.toml`): `tamanoFuego` (A/B/C/D), `alturaFuego`, `bajarFuego`, `volumen` general y `bordeIntensidad` (0 = sin borde, 0.25 = sutil).

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
