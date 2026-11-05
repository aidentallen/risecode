#!/usr/bin/env python3
"""Generate original short interface sounds. Requires ffmpeg with libvorbis."""
import argparse, math, random, struct, subprocess, tempfile, wave
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/riserecode'
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--new-only', action='store_true', help='Preserve the original five generated assets')
args = parser.parse_args()
for name,freq,length in [('tick',1800,.045),('tap',520,.085),('mechanical',1300,.065),('wood',310,.11),('glass',2600,.16)]:
    if args.new_only: continue
    rng=random.Random(1700+freq);rate=44100;data=[];filtered=0
    for i in range(int(rate*length)):
        t=i/rate;noise=rng.uniform(-1,1);filtered=.65*filtered+.35*noise
        tone=math.sin(math.tau*freq*t)+.3*math.sin(math.tau*freq*1.51*t)
        signal=(.65*filtered+.35*tone) if name in ('tick','mechanical') else (.15*filtered+.85*tone)
        envelope=min(1,t/.001)*math.exp(-t/(length/5))
        data.append(struct.pack('<h',int(max(-1,min(1,signal*envelope*.42))*32767)))
    with tempfile.TemporaryDirectory() as folder:
        wav=Path(folder)/'sound.wav'
        with wave.open(str(wav),'wb') as f:f.setnchannels(1);f.setsampwidth(2);f.setframerate(rate);f.writeframes(b''.join(data))
        subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','4',str(root/'sounds/ui'/f'{name}.ogg')],check=True)
import json
p=root/'sounds.json';d=json.loads(p.read_text())
for name in ['tick','tap','mechanical','wood','glass']:d['ui.'+name]={'sounds':['riserecode:ui/'+name]}
p.write_text(json.dumps(d,indent=2)+'\n')

# Original layered transients: a short attack, body and release, with no samples.
# Fixed seeds keep waveform generation reproducible. Leave a quiet tail so the
# Vorbis decoder never cuts off a resonant body at the end of the asset.
profiles = {
    'thock': (0.14, 190, 0.038, 0.20, 0.003),
    'creamy': (0.13, 290, 0.026, 0.12, 0.006),
    'marble': (0.12, 780, 0.024, 0.16, 0.002),
    'bubble': (0.15, 650, 0.032, 0.02, 0.003),
    'snap': (0.085, 1650, 0.012, 0.75, 0.001),
    'ceramic': (0.17, 1450, 0.035, 0.07, 0.002),
    'rubber': (0.13, 360, 0.025, 0.06, 0.004),
    'chime': (0.24, 1800, 0.047, 0.01, 0.002),
}
for index, (name, (length, freq, decay, texture, attack)) in enumerate(profiles.items()):
    rate = 44100
    rng = random.Random(6100 + index)
    samples = []
    filtered = 0.0
    phase = 0.0
    for i in range(int(rate * length)):
        t = i / rate
        noise = rng.uniform(-1, 1)
        filtered = 0.78 * filtered + 0.22 * noise
        # Downward pitch sweeps give bubble/rubber their rounded tactile body.
        sweep = (1 + 1.8 * math.exp(-t / 0.018)) if name in ('bubble', 'rubber') else 1
        phase += math.tau * freq * sweep / rate
        ratio = 2.76 if name in ('marble', 'ceramic', 'chime') else 1.52
        tone = math.sin(phase) + 0.28 * math.sin(ratio * phase) * math.exp(-t / 0.018)
        body = tone * math.exp(-t / decay)
        transient = (noise if name == 'snap' else filtered) * math.exp(-t / 0.006)
        # Two closely spaced impacts make the low keyboard sounds feel seated.
        release = 0.0
        if name in ('thock', 'creamy') and t >= 0.014:
            release = 0.28 * math.sin(math.tau * freq * 0.72 * (t - 0.014)) * math.exp(-(t - 0.014) / 0.015)
        envelope = min(1.0, t / attack) * min(1.0, (length - t) / 0.012)
        samples.append(((1 - texture) * body + texture * transient + release) * envelope)
    peak = max(abs(sample) for sample in samples)
    pcm = b''.join(struct.pack('<h', round(sample / peak * 0.60 * 32767)) for sample in samples)
    with tempfile.TemporaryDirectory() as folder:
        wav = Path(folder) / 'sound.wav'
        with wave.open(str(wav), 'wb') as f:
            f.setnchannels(1)
            f.setsampwidth(2)
            f.setframerate(rate)
            f.writeframes(pcm)
        subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y', '-i', str(wav),
                        '-c:a', 'libvorbis', '-q:a', '4', str(root / 'sounds/ui' / f'{name}.ogg')], check=True)
        # Decode the actual shipped asset and reject corrupt or clipped audio.
        decoded = subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-i',
                                  str(root / 'sounds/ui' / f'{name}.ogg'), '-f', 's16le', '-'],
                                 check=True, capture_output=True).stdout
        values = struct.unpack('<' + 'h' * (len(decoded) // 2), decoded)
        if not values or max(abs(value) for value in values) >= 32760:
            raise ValueError(f'Empty or clipped decoded sound: {name}')
    d['ui.' + name] = {'sounds': ['riserecode:ui/' + name]}
p.write_text(json.dumps(d, indent=2) + '\n')
