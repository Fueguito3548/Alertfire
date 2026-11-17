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

- **0 s – 7.3 s:** música de fondo que crece de grave a agudo y fuegos artificiales cada vez más seguidos. Cada tanda suena con una **nota de "carga"** que sube de grave a agudo.
- **7.5 s:** explosión grande + destello del color del tipo + **remate sonoro propio de cada tipo de aviso** (máx. 3 s).
- **8.5 s:** sube el fuego de pantalla y aparece un **difuminado del color del aviso en el borde de la pantalla** (con un leve "latido").
- **9.2 s:** sonido de mensaje, el texto sale en el chat y aparece un **título grande en pantalla** (tipo de aviso + mensaje) durante 6 s.
- **~19 s:** el borde se desvanece cuando el chat oculta el mensaje.

### Remate de cada tipo (todos distintos)

| tipo | sonido |
|---|---|
| `info` | fanfarria brillante y alegre de campanitas |
| `dificultad` | tensión creciente con latidos y un gong grave |
| `mob` | "chan, chan, chan, chaaan": 4 golpes graves descendentes |
| `jugador` | intriga: pizzicato misterioso con nota aguda inquietante |
| `mision` | notificación de campanas tipo "misión aceptada" |
| `expedicion` | redoble de timbales que se acelera y fanfarria de metales |
| `final` | impacto épico con coro, metales y campanas |

## Sonidos

Todos los sonidos son **originales y propios del mod** (no son de Minecraft): están en
`src/main/resources/assets/desafioalert/sounds/` y se registran en `sounds.json`.
Se generan con un script de Python, así que se pueden modificar y volver a crear:

```
pip install numpy scipy        # y tener ffmpeg instalado
python3 tools/generate_sounds.py            # genera todos
python3 tools/generate_sounds.py boom msg   # solo los que contengan esas palabras
```

Cada sonido se puede cambiar desde la config del servidor por cualquier otro id
(`minecraft:...` o de otro mod/resource pack).

## Configuración

- **Servidor/común** (`desafioalert-common.toml`): invencibilidad del aviso `final` (`invencibleSegundos`, `retrasoSegundos`) y, por cada uno de los 14 sonidos, su id y volumen: `fuegos_artificiales`, `explosion_grande`, `explosion_destellos`, `fuego_pantalla`, `mensaje_chat`, `notas_carga`, `musica_fondo` y un `remate_<tipo>` por cada tipo.
- **Cliente** (`desafioalert-client.toml`): `tamanoFuego` (A/B/C/D), `alturaFuego`, `bajarFuego`, `volumen` general, `bordeIntensidad` (0 = sin borde, 0.25 = sutil) y `mostrarTitulo`.

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
├── client/           ClientAnim (cola, sonidos, dibujo, borde, titulo), FireTextures, eventos
└── net/              AlertPacket + NetworkHandler

tools/                generador de sonidos (Python)
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
