#!/usr/bin/env python3
"""
Genera los sonidos del mod (src/main/resources/assets/desafioalert/sounds/*.ogg).

Requisitos: python3, numpy, scipy y ffmpeg (con libvorbis) en el PATH.
Uso:        python3 tools/generate_sounds.py            (genera todos)
            python3 tools/generate_sounds.py boom msg   (solo los que contengan esas palabras)
"""
import os
import subprocess
import sys
import tempfile

import numpy as np
from scipy.io import wavfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from synth import *  # noqa: E402,F401,F403

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                   "src", "main", "resources", "assets", "desafioalert", "sounds")


def save(name, x, peak=0.70):
    x = softclip(x, 1.1)
    x = normalize(x, peak)
    x = fade_out(x, 0.3 if len(x) / SR >= 1.9 else 0.06)
    os.makedirs(OUT, exist_ok=True)
    wav = tempfile.mktemp(suffix=".wav")
    wavfile.write(wav, SR, (x * 32767).astype(np.int16))
    dst = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis",
                    "-q:a", "4", "-ac", "1", "-ar", str(SR), dst], check=True)
    os.remove(wav)
    print(f"  {name}.ogg  {len(x) / SR:.2f}s")


# =============================================================== FUEGOS ARTIFICIALES
def firework(seed, kind):
    d = 1.7
    buf = silent(d)
    r = rng_for(seed)
    put(buf, thump(r.uniform(130, 170), 42, 0.5, 0.06, 0.10), 0.0, 1.0)
    put(buf, noise_burst(0.12, 0.012, seed + 1, lo=1500), 0.0, 0.8)
    put(buf, noise_burst(0.6, 0.22, seed + 2, hi=2800), 0.0, 0.35)
    put(buf, crackles(1.5, 55, seed + 3), 0.05, 1.0)
    put(buf, glitter(1.4, 12, seed + 4), 0.05, 1.0)
    if kind == 1:   # silbido descendente
        n = N(0.55)
        f = np.linspace(3200, 900, n)
        put(buf, np.sin(2 * np.pi * np.cumsum(f) / SR) * expo(n, 0.25, 0.01), 0.03, 0.12)
    if kind == 2:   # doble estallido
        put(buf, thump(110, 38, 0.4, 0.05, 0.09), 0.13, 0.7)
        put(buf, noise_burst(0.08, 0.01, seed + 5, lo=2000), 0.13, 0.6)
    if kind == 3:   # destello de campanitas
        for k, m in enumerate([88, 93, 97, 100]):
            put(buf, fm_bell(hz(m), 0.6, 3.0, 1.5, 0.15), 0.06 + k * 0.045, 0.16)
    return reverb(buf, 0.7, 0.2, seed + 9)


# =============================================================== NOTA DE CARGA
def charge():
    f = hz(62)  # D4; el juego la sube/baja con el pitch
    buf = silent(1.1)
    put(buf, fm_bell(f, 1.0, 2.0, 3.2, 0.28), 0, 1.0)
    put(buf, fm_bell(f * 2, 0.8, 3.01, 2.0, 0.12), 0, 0.5)
    put(buf, fm_bell(f * 4, 0.5, 1.41, 1.2, 0.07), 0, 0.25)
    put(buf, noise_burst(0.01, 0.003, 11, lo=5000), 0, 0.35)
    buf = echo(buf, 0.11, 0.45, 0.4, 4)
    return reverb(buf, 0.9, 0.28, 3)


# =============================================================== EXPLOSION FINAL (generica)
def boom():
    d = 3.0
    buf = silent(d)
    put(buf, sub_drop(95, 26, d, 0.22, 0.9), 0, 1.0)
    put(buf, thump(220, 50, 1.0, 0.07, 0.3), 0, 0.7)
    put(buf, noise_burst(0.09, 0.02, 21, lo=800), 0, 1.0)
    blast = noise(2.0, 22)
    blast = sweep(blast, 5000, 180, "lowpass") * expo(len(blast), 0.45)
    put(buf, blast, 0, 0.9)
    rum = lp(noise(2.8, 23), 130, 2) * expo(N(2.8), 1.1, 0.02)
    put(buf, rum, 0.02, 2.2)
    put(buf, crackles(2.2, 26, 24, 600, 3500, 1.2, 0.5), 0.3, 0.8)
    # ola de choque ascendente muy breve antes del golpe
    return reverb(buf, 2.2, 0.32, 25, 3500)


def sparkle_blast():
    d = 2.4
    buf = silent(d)
    r = rng_for(31)
    pent = [0, 2, 4, 7, 9]
    for k in range(34):
        t0 = (k / 34) ** 1.4 * 1.1
        m = 84 + pent[r.integers(5)] + 12 * r.integers(0, 2)
        put(buf, fm_bell(hz(m), 0.9, 3.5, 2.0, 0.2), t0, r.uniform(0.15, 0.4))
    n = N(0.18)
    f = np.linspace(500, 3400, n)
    put(buf, np.sin(2 * np.pi * np.cumsum(f) / SR) * np.hanning(n), 0, 0.35)
    air = sweep(noise(1.2, 32), 9000, 2500, "highpass") * expo(N(1.2), 0.35, 0.01)
    put(buf, air, 0, 0.4)
    put(buf, glitter(2.0, 40, 33, 3000, 9000, 0.12), 0.1, 1.0)
    return reverb(buf, 2.0, 0.4, 34)


# =============================================================== FUEGO DE PANTALLA
def fire():
    d = 2.0
    buf = silent(d)
    w = sweep(noise(1.6, 41), 250, 2400, "bandpass", 1.2)
    w = sweep(w, 2400, 700, "bandpass", 1.2) if False else w
    env = np.concatenate([np.linspace(0, 1, N(0.45)) ** 2, np.exp(-np.arange(N(1.15)) / SR / 0.45)])
    put(buf, w * env[: len(w)] * 0.9, 0)
    roar = lp(noise(2.0, 42), 420, 2)
    flutter = 0.7 + 0.3 * np.sin(2 * np.pi * 7.3 * np.arange(len(roar)) / SR) * np.sin(2 * np.pi * 2.1 * np.arange(len(roar)) / SR)
    put(buf, roar * flutter * adsr(len(roar), 0.5, 0.3, 0.6, 1.0), 0, 1.2)
    put(buf, crackles(1.9, 70, 43, 1500, 7000, 1.0, 0.5), 0.1, 1.0)
    put(buf, thump(80, 40, 0.5, 0.1, 0.25), 0.3, 0.5)
    return reverb(buf, 1.1, 0.22, 44)


# =============================================================== MENSAJE DEL CHAT
def message():
    d = 1.6
    buf = silent(d)
    n = N(0.2)
    f = np.linspace(300, 2600, n)
    put(buf, np.sin(2 * np.pi * np.cumsum(f) / SR) * np.hanning(n), 0, 0.22)
    put(buf, thump(75, 48, 0.4, 0.08, 0.14), 0.0, 0.55)
    for k, m in enumerate([69, 72, 76, 81]):          # La menor: A C E A
        p = pluck(hz(m), 0.45, 0.8, 0.003, 0.18, "square")
        put(buf, p, 0.12 + k * 0.105, 0.55)
    put(buf, fm_bell(hz(88), 1.2, 3.5, 1.8, 0.35), 0.5, 0.28)      # E6 brillante
    put(buf, fm_bell(hz(81), 1.2, 2.0, 1.2, 0.30), 0.5, 0.2)
    buf = bitcrush(buf, 9, 2) * 0.6 + buf * 0.4
    buf = echo(buf, 0.17, 0.4, 0.4, 4)
    return reverb(buf, 1.0, 0.25, 52)


# =============================================================== MUSICA DE FONDO (14.5 s)
def music():
    d = 14.5
    buf = silent(d)
    step = 0.125  # semicorchea a 120 bpm
    pent = [0, 3, 5, 7, 10]
    notes = [45 + 12 * o + p for o in range(3) for p in pent] + [81]  # A2 .. A5
    total = int(7.3 / step)
    r = rng_for(61)
    arp = silent(d)
    for s in range(total):
        u = s / total
        base = int(u * 13)
        idx = min(len(notes) - 1, base + [0, 2, 1, 3][s % 4])
        g = 0.35 + 0.65 * u
        p = pluck(hz(notes[idx]), 0.4, 0.5 + 0.8 * u, 0.004, 0.16)
        put(arp, p, s * step, g)
    put(buf, arp, 0, 0.9)
    # bajo: pulso grave que se acelera
    bass = silent(d)
    pat = [33, 33, 40, 33]
    t, k = 0.0, 0
    while t < 7.3:
        u = t / 7.3
        iv = 0.5 if t < 3.5 else 0.25
        b = sub_drop(hz(pat[k % 4]) * 2, hz(pat[k % 4]), 0.45, 0.02, 0.18)
        put(bass, b, t, 0.4 + 0.6 * u)
        t += iv
        k += 1
    put(buf, bass, 0, 0.9)
    # subida de ruido
    n = N(4.3)
    ris = sweep(noise(4.3, 62), 300, 9000, "bandpass", 1.5)
    ris *= (np.linspace(0, 1, n) ** 2.2)
    put(buf, ris, 3.0, 0.7)
    # hi-hat que se acelera
    t, iv = 4.5, 0.25
    while t < 7.3:
        u = (t - 4.5) / 2.8
        put(buf, noise_burst(0.04, 0.01, int(t * 1000), lo=7000), t, 0.12 + 0.3 * u)
        iv = max(0.06, iv * 0.93)
        t += iv
    # silencio breve antes de la explosion
    cut = np.ones(len(buf))
    a, b = N(7.28), N(7.48)
    cut[a:b] = np.linspace(1, 0, b - a)
    cut[b:N(7.7)] = 0
    buf *= cut
    # despues de la explosion: colchon calmado + notas agudas que se apagan
    chords = [[57, 64, 71, 72], [53, 60, 64, 69], [60, 67, 76], [55, 62, 71, 74]]
    for i, ch in enumerate(chords):
        t0 = 7.7 + i * 1.75
        for m in ch:
            put(buf, pad(hz(m), 2.6, 0.5, 0.9, 1800), t0, 0.16)
    hi = [81, 76, 72, 76]
    for i in range(11):
        put(buf, pluck(hz(hi[i % 4]), 0.5, 0.7, 0.004, 0.3), 8.2 + i * 0.5, 0.45 * (1 - i / 12))
    buf = echo(buf, 0.375, 0.35, 0.3, 4)
    buf = reverb(buf, 1.8, 0.22, 63)
    return fade_out(buf, 0.8)


# =============================================================== REMATES (un sonido por tipo)
def final_info():
    d = 2.6
    buf = silent(d)
    for k, m in enumerate([72, 76, 79, 84, 88]):                 # Do mayor subiendo, alegre
        put(buf, fm_bell(hz(m), 0.9, 4.0, 1.6, 0.18), k * 0.085, 0.5)
        put(buf, pluck(hz(m - 12), 0.4, 0.8, 0.004, 0.15, "tri"), k * 0.085, 0.3)
    for m in [60, 64, 67, 74]:
        put(buf, pad(hz(m), 2.3, 0.04, 1.4, 3600), 0.38, 0.22)
    put(buf, fm_bell(hz(96), 2.0, 3.5, 1.2, 0.6), 0.45, 0.22)
    put(buf, noise_burst(1.0, 0.35, 71, lo=6000), 0.3, 0.25)
    put(buf, glitter(2.2, 30, 72, 3000, 9000, 0.14), 0.3, 1.0)
    put(buf, thump(110, 45, 0.4, 0.06, 0.11), 0, 0.6)
    return reverb(buf, 1.6, 0.3, 73)


def final_mob():
    d = 3.0
    buf = silent(d)
    hits = [(0.00, 41), (0.55, 41), (1.10, 40), (1.65, 39)]      # "chan, chan, chan, chaaan"
    for i, (t0, m) in enumerate(hits):
        g = 0.7 + 0.1 * i
        last = i == 3
        dur = 1.4 if last else 0.45
        put(buf, brass(hz(m), dur, 3200, 0.02, None if last else 0.22), t0, g)
        put(buf, brass(hz(m + 12), dur, 2600, 0.02, None if last else 0.2), t0, g * 0.6)
        put(buf, sub_drop(hz(m) * 1.5, hz(m) * 0.5, 0.8, 0.04, 0.35), t0, 0.9)
        put(buf, noise_burst(0.15, 0.03, 81 + i, lo=500, hi=4000), t0, 0.5)
        put(buf, gong(hz(m + 24), 1.0, 0.3, i), t0, 0.35)
    # acorde disonante final (tritono)
    for m in [39, 45, 51]:
        put(buf, pad(hz(m), 1.5, 0.05, 1.0, 1400), 1.65, 0.3)
    return reverb(buf, 1.4, 0.28, 85, 4000)


def final_dificultad():
    d = 3.0
    buf = silent(d)
    n = N(1.8)
    bend = np.linspace(0, 1, n) ** 1.5
    for m0, g in [(52, 0.5), (58, 0.5), (64, 0.3)]:               # tritono con glissando hacia arriba
        f = hz(m0) * (1 + 0.06 * bend)
        x = osc(f, 1.8, "saw") * 0.5 + osc(f * 1.003, 1.8, "saw") * 0.5
        put(buf, lp(x, 1500, 2) * (np.linspace(0, 1, n) ** 2), 0, g * 0.6)
    for t0 in [0.2, 0.55, 0.95, 1.30]:                            # latidos
        put(buf, thump(95, 38, 0.4, 0.04, 0.12), t0, 0.9 if int(t0 * 100) % 2 == 0 else 0.7)
    scr = sweep(noise(1.4, 91), 700, 5500, "bandpass", 3.0) * np.linspace(0, 1, N(1.4)) ** 2
    put(buf, scr, 0.3, 0.25)
    put(buf, gong(hz(40), 1.8, 1.1, 5), 1.7, 1.0)                 # gong
    put(buf, sub_drop(80, 30, 1.2, 0.15, 0.6), 1.7, 0.9)
    put(buf, noise_burst(0.2, 0.05, 92, lo=1500), 1.7, 0.5)
    return reverb(buf, 1.8, 0.3, 93, 3500)


def final_jugador():
    d = 3.0
    buf = silent(d)
    motif = [(0.00, 62), (0.17, 65), (0.34, 64), (0.62, 69), (0.79, 68), (0.96, 69)]   # Re menor armonico
    for t0, m in motif:
        put(buf, pluck(hz(m), 0.5, 0.9, 0.002, 0.22, "tri"), t0, 0.7)
        put(buf, fm_bell(hz(m + 12), 0.5, 3.0, 1.0, 0.12), t0, 0.15)
    put(buf, pad(hz(38), 3.0, 0.8, 1.0, 600), 0, 0.5)                    # pedal grave misterioso
    n = N(2.2)
    t = np.arange(n) / SR
    vib = hz(93) * (1 + 0.004 * np.sin(2 * np.pi * 5.5 * t))
    put(buf, np.sin(2 * np.pi * np.cumsum(vib) / SR) * adsr(n, 1.0, 0.1, 0.8, 0.6), 0.8, 0.18)   # nota aguda inquietante
    wh = bp(noise(1.5, 101), 2500, 4500) * (0.6 + 0.4 * np.sin(2 * np.pi * 9 * np.arange(N(1.5)) / SR))
    put(buf, wh * np.hanning(len(wh)), 1.0, 0.18)
    put(buf, thump(70, 40, 0.5, 0.1, 0.2), 1.55, 0.7)
    put(buf, pluck(hz(64), 0.6, 0.9, 0.002, 0.3, "tri"), 1.95, 0.6)
    put(buf, pluck(hz(69), 0.7, 0.9, 0.002, 0.35, "tri"), 2.15, 0.6)
    return reverb(buf, 1.9, 0.33, 103, 4500)


def final_mision():
    d = 1.9
    buf = silent(d)
    for k, m in enumerate([79, 83, 86]):                                  # Sol Si Re (glockenspiel)
        put(buf, fm_bell(hz(m), 1.2, 3.5, 1.4, 0.4), k * 0.13, 0.55)
    put(buf, fm_bell(hz(98), 1.6, 3.5, 1.0, 0.7), 0.4, 0.3)
    n = N(0.5)
    f = np.linspace(500, 2200, n)
    put(buf, np.sin(2 * np.pi * np.cumsum(f) / SR) * np.hanning(n), 0, 0.15)
    for m in [55, 62, 67]:
        put(buf, pad(hz(m), 1.8, 0.05, 1.0, 2400), 0.1, 0.18)
    put(buf, glitter(1.6, 18, 111, 3500, 9000, 0.12), 0.2, 1.0)
    buf = echo(buf, 0.2, 0.35, 0.35, 3)
    return reverb(buf, 1.3, 0.28, 112)


def final_expedicion():
    d = 3.0
    buf = silent(d)
    t, iv, k = 0.0, 0.13, 0           # redoble de timbales
    while t < 1.15:
        u = t / 1.15
        put(buf, thump(hz(38) * 2.2, hz(38), 0.5, 0.03, 0.2), t, 0.35 + 0.6 * u)
        put(buf, noise_burst(0.05, 0.015, 121 + k, lo=300, hi=2500), t, 0.15 + 0.3 * u)
        iv = max(0.045, iv * 0.93)
        t += iv
        k += 1
    cym = hp(noise(1.2, 122), 4000) * (np.linspace(0, 1, N(1.2)) ** 2)
    put(buf, cym, 0, 0.35)
    put(buf, thump(hz(38) * 2.4, hz(26), 1.2, 0.05, 0.5), 1.15, 1.0)              # golpe grande
    put(buf, noise_burst(1.6, 0.5, 123, lo=5000), 1.15, 0.6)                      # platillo
    for t0, m in [(1.15, 62), (1.45, 69), (1.75, 74)]:                            # fanfarria Re-La-Re
        put(buf, brass(hz(m), 1.25, 4200, 0.03), t0, 0.5)
        put(buf, brass(hz(m + 7), 1.25, 3800, 0.03), t0, 0.3)
    for m in [50, 62, 66, 69, 74]:
        put(buf, pad(hz(m), 1.4, 0.05, 1.0, 2800), 1.75, 0.25)
    put(buf, glitter(1.6, 20, 124, 3000, 8000, 0.1), 1.2, 1.0)
    return reverb(buf, 1.6, 0.27, 125, 5000)


def final_final():
    d = 3.0
    buf = silent(d)
    put(buf, sub_drop(70, 22, d, 0.3, 1.4), 0, 1.0)                               # impacto
    put(buf, thump(180, 40, 1.0, 0.08, 0.35), 0, 0.8)
    put(buf, noise_burst(0.25, 0.05, 131, lo=500), 0, 0.9)
    put(buf, gong(hz(38), 3.0, 1.8, 7), 0, 1.0)
    for m, g in [(26, 0.55), (38, 0.5), (45, 0.4), (50, 0.35), (53, 0.3)]:        # braaam Re menor
        put(buf, brass(hz(m), 2.6, 3500, 0.12), 0.0, g)
    for m in [62, 65, 69, 74, 77]:                                                # coro
        put(buf, choir(hz(m), 2.8, 0.5, 0.7, 5.0, m), 0.0, 0.3)
    for k, m in enumerate([86, 93, 89, 98, 101, 105]):
        put(buf, fm_bell(hz(m), 1.4, 3.5, 1.5, 0.45), 0.12 + k * 0.17, 0.22)
    rev = hp(noise(1.1, 132), 3000) * (np.linspace(0, 1, N(1.1)) ** 3)
    put(buf, rev, 0, 0.3)
    put(buf, glitter(2.8, 55, 133, 2500, 9500, 0.12), 0.2, 1.0)
    buf = echo(buf, 0.3, 0.3, 0.25, 3)
    return reverb(buf, 2.6, 0.38, 134, 4500)


SOUNDS = {
    "firework_1": lambda: firework(1000, 0),
    "firework_2": lambda: firework(2000, 1),
    "firework_3": lambda: firework(3000, 2),
    "firework_4": lambda: firework(4000, 3),
    "charge": charge,
    "boom": boom,
    "sparkle_blast": sparkle_blast,
    "fire": fire,
    "message": message,
    "music": music,
    "final_info": final_info,
    "final_dificultad": final_dificultad,
    "final_mob": final_mob,
    "final_jugador": final_jugador,
    "final_mision": final_mision,
    "final_expedicion": final_expedicion,
    "final_final": final_final,
}

if __name__ == "__main__":
    only = sys.argv[1:]
    print("Generando sonidos en", os.path.normpath(OUT))
    for name, fn in SOUNDS.items():
        if only and not any(o in name for o in only):
            continue
        save(name, fn(), 0.7 if name == "music" else 0.70)
