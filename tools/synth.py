"""
Utilidades de sintesis de audio para generar los sonidos del mod (numpy + scipy).
Todo es mono, 44.1 kHz.
"""
import numpy as np
from scipy import signal

SR = 44100


def hz(midi):
    return 440.0 * 2.0 ** ((np.asarray(midi, dtype=float) - 69.0) / 12.0)


def N(d):
    return int(round(d * SR))


def tt(d):
    return np.arange(int(round(d * SR))) / SR


def rng_for(seed):
    return np.random.default_rng(seed)


# ---------------------------------------------------------------- envolventes
def adsr(n, a=0.005, d=0.1, s=0.7, r=0.1):
    """n muestras. a/d/r en segundos, s = nivel de sostenido."""
    a_n, d_n, r_n = int(a * SR), int(d * SR), int(r * SR)
    s_n = max(0, n - a_n - d_n - r_n)
    env = np.concatenate([
        np.linspace(0, 1, max(a_n, 1), endpoint=False),
        np.linspace(1, s, max(d_n, 1), endpoint=False),
        np.full(s_n, s),
        np.linspace(s, 0, max(r_n, 1)),
    ])
    if len(env) < n:
        env = np.pad(env, (0, n - len(env)))
    return env[:n]


def expo(n, tau, attack=0.002):
    t = np.arange(n) / SR
    e = np.exp(-t / tau)
    a_n = max(1, int(attack * SR))
    e[:a_n] *= np.linspace(0, 1, a_n)
    return e


def fade_out(x, secs):
    n = min(len(x), int(secs * SR))
    if n > 0:
        x = x.copy()
        x[-n:] *= np.linspace(1, 0, n) ** 1.5
    return x


# ---------------------------------------------------------------- osciladores
def osc(freq, d, kind="sine", phase=0.0):
    """freq: escalar o arreglo por muestra."""
    n = int(round(d * SR))
    f = np.full(n, float(freq)) if np.isscalar(freq) else np.asarray(freq, dtype=float)[:n]
    ph = 2 * np.pi * np.cumsum(f) / SR + phase
    if kind == "sine":
        return np.sin(ph)
    p = (ph / (2 * np.pi)) % 1.0
    if kind == "saw":
        return 2 * p - 1
    if kind == "square":
        return np.where(p < 0.5, 1.0, -1.0)
    if kind == "tri":
        return 2 * np.abs(2 * p - 1) - 1
    raise ValueError(kind)


def noise(d, seed=0):
    return rng_for(seed).standard_normal(int(round(d * SR)))


# ---------------------------------------------------------------- filtros
def lp(x, fc, order=4):
    fc = min(fc, SR / 2 - 100)
    return signal.sosfilt(signal.butter(order, fc, "low", fs=SR, output="sos"), x)


def hp(x, fc, order=4):
    return signal.sosfilt(signal.butter(order, fc, "high", fs=SR, output="sos"), x)


def bp(x, lo, hi, order=2):
    hi = min(hi, SR / 2 - 100)
    return signal.sosfilt(signal.butter(order, [lo, hi], "band", fs=SR, output="sos"), x)


def sweep(x, f0, f1, kind="bandpass", q=2.0, chunk=512, exp=True):
    """Filtro de frecuencia variable (de f0 a f1) aplicado por bloques con estado continuo."""
    n = len(x)
    out = np.zeros(n)
    zi = None
    nchunks = (n + chunk - 1) // chunk
    for i in range(nchunks):
        a, b = i * chunk, min(n, (i + 1) * chunk)
        u = i / max(1, nchunks - 1)
        fc = f0 * (f1 / f0) ** u if exp else f0 + (f1 - f0) * u
        fc = float(np.clip(fc, 40, SR / 2 - 200))
        if kind == "bandpass":
            sos = signal.butter(2, [fc / (1 + 1 / q), min(fc * (1 + 1 / q), SR / 2 - 100)], "band", fs=SR, output="sos")
        elif kind == "lowpass":
            sos = signal.butter(2, fc, "low", fs=SR, output="sos")
        else:
            sos = signal.butter(2, fc, "high", fs=SR, output="sos")
        if zi is None:
            zi = np.zeros((sos.shape[0], 2))
        y, zi = signal.sosfilt(sos, x[a:b], zi=zi)
        out[a:b] = y
    return out


# ---------------------------------------------------------------- efectos
def reverb(x, rt60=1.5, mix=0.3, seed=7, damp=6000, predelay=0.012):
    n_ir = int(rt60 * SR)
    t = np.arange(n_ir) / SR
    ir = rng_for(seed).standard_normal(n_ir) * np.exp(-6.9 * t / rt60)
    ir = lp(ir, damp, 2)
    ir[: int(predelay * SR)] = 0
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    wet = signal.fftconvolve(x, ir)[: len(x)]
    wet *= np.sqrt(np.sum(x ** 2) / (np.sum(wet ** 2) + 1e-9))
    return x * (1 - mix) + wet * mix


def echo(x, delay=0.2, fb=0.4, mix=0.35, taps=5):
    out = x.copy()
    d = int(delay * SR)
    for k in range(1, taps + 1):
        if k * d >= len(x):
            break
        out[k * d:] += x[: len(x) - k * d] * (fb ** k) * mix / (1 - 0 * fb)
    return out


def softclip(x, drive=1.2):
    return np.tanh(x * drive) / np.tanh(drive)


def bitcrush(x, bits=6, hold=3):
    q = 2 ** bits
    y = np.round(x * q) / q
    return np.repeat(y[::hold], hold)[: len(x)]


def normalize(x, peak=0.89):
    m = np.max(np.abs(x)) + 1e-9
    return x / m * peak


# ---------------------------------------------------------------- mezcla
def put(buf, x, t0, gain=1.0):
    i = int(round(t0 * SR))
    if i >= len(buf):
        return
    j = min(len(buf), i + len(x))
    buf[i:j] += x[: j - i] * gain


def silent(d):
    return np.zeros(int(round(d * SR)))


# ---------------------------------------------------------------- instrumentos
def fm_bell(f, d=1.0, ratio=3.5, index=4.0, tau=0.3, amp_tau=None, attack=0.002):
    n = N(d)
    t = np.arange(n) / SR
    amp_tau = amp_tau or tau * 1.6
    mod_env = np.exp(-t / tau)
    ph = 2 * np.pi * f * t + index * mod_env * np.sin(2 * np.pi * f * ratio * t)
    return np.sin(ph) * expo(n, amp_tau, attack)


def pluck(f, d=0.5, bright=0.6, detune=0.004, tau=0.25, kind="saw"):
    """Pluck sintetico: saws desafinados + filtro que se cierra rapido."""
    n = N(d)
    a = osc(f * (1 - detune), d, kind) + osc(f * (1 + detune), d, kind)
    lo = lp(a, max(200, f * 1.5), 2)
    hi = lp(a, min(9000, f * 14 * bright + 600), 2)
    e = np.exp(-np.arange(n) / SR / (tau * 0.5))
    y = lo * (1 - e) + hi * e
    return y * expo(n, tau, 0.002) * 0.5


def pad(f, d=2.0, a=0.4, r=0.6, cutoff=2200, detune=0.006, kind="saw"):
    n = N(d)
    x = (osc(f * (1 - detune), d, kind) + osc(f, d, kind) + osc(f * (1 + detune), d, kind)) / 3
    x = lp(x, cutoff, 2)
    return x * adsr(n, a, 0.2, 0.8, r)


def brass(f, d=0.5, open_to=3500, a=0.03, tau=None, detune=0.007):
    n = N(d)
    x = sum(osc(f * (1 + k * detune), d, "saw") for k in (-1, 0, 1)) / 3
    t = np.arange(n) / SR
    mix = np.clip(t / max(a * 3, 1e-3), 0, 1)
    dark = lp(x, max(300, f * 2), 2)
    bright = lp(x, open_to, 2)
    y = dark * (1 - mix) + bright * mix
    env = adsr(n, a, 0.1, 0.8, 0.12) if tau is None else expo(n, tau, a)
    return y * env


def sub_drop(f0=120, f1=35, d=1.0, tau=0.12, decay=0.7):
    n = N(d)
    t = np.arange(n) / SR
    f = f1 + (f0 - f1) * np.exp(-t / tau)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * expo(n, decay, 0.003)


def thump(f0=90, f1=40, d=0.4, tau=0.05, decay=0.12):
    return sub_drop(f0, f1, d, tau, decay)


def noise_burst(d, tau, seed, lo=None, hi=None, amp=1.0):
    x = noise(d, seed)
    if lo and hi:
        x = bp(x, lo, hi)
    elif lo:
        x = hp(x, lo)
    elif hi:
        x = lp(x, hi)
    return x * expo(len(x), tau, 0.0005) * amp


def crackles(d, count, seed, lo=2500, hi=9000, decay_pow=1.6, amp=0.4):
    r = rng_for(seed)
    buf = silent(d)
    for _ in range(count):
        t0 = d * (r.random() ** decay_pow)
        c = r.uniform(lo, hi)
        click = noise_burst(0.008, 0.002, int(r.integers(1 << 30)), c * 0.7, min(c * 1.4, SR / 2 - 200))
        put(buf, click, t0, amp * r.uniform(0.3, 1.0) * (1 - t0 / d * 0.7))
    return buf


def glitter(d, count, seed, lo=2500, hi=7500, amp=0.12, blip=0.09):
    r = rng_for(seed)
    buf = silent(d)
    for _ in range(count):
        t0 = r.uniform(0, d - blip)
        f = r.uniform(lo, hi)
        n = int(blip * SR)
        s = np.sin(2 * np.pi * f * np.arange(n) / SR) * expo(n, blip / 3, 0.001)
        put(buf, s, t0, amp * r.uniform(0.4, 1.0))
    return buf


def gong(f, d=3.0, tau=1.2, seed=0):
    """Gong/campana inarmonica."""
    n = N(d)
    t = np.arange(n) / SR
    partials = [1.0, 1.59, 2.14, 2.30, 2.65, 2.92, 3.51, 4.18]
    gains = [1.0, 0.8, 0.6, 0.55, 0.5, 0.4, 0.3, 0.2]
    out = np.zeros(n)
    r = rng_for(seed)
    for p, g in zip(partials, gains):
        out += g * np.sin(2 * np.pi * f * p * t + r.random() * 6.28) * np.exp(-t / (tau / (0.6 + p * 0.4)))
    return out * expo(n, tau * 1.4, 0.002) * 0.3


def choir(f, d=2.5, a=0.5, r=0.6, vib=5.2, seed=0):
    """Coro 'aah' sintetico: saw con vibrato filtrado por formantes."""
    n = N(d)
    t = np.arange(n) / SR
    vibr = 1 + 0.006 * np.sin(2 * np.pi * vib * t + seed)
    src = osc(f * vibr, d, "saw")
    src += 0.6 * osc(f * 1.004 * vibr, d, "saw")
    y = bp(src, 650, 900, 2) * 1.0 + bp(src, 1050, 1300, 2) * 0.7 + bp(src, 2400, 2900, 2) * 0.35
    return y * adsr(n, a, 0.2, 0.85, r)
