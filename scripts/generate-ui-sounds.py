#!/usr/bin/env python3
"""Generate original short interface sounds. Requires ffmpeg with libvorbis."""
import math, random, struct, subprocess, tempfile, wave
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/riserecode'
for name,freq,length in [('tick',1800,.045),('tap',520,.085),('mechanical',1300,.065),('wood',310,.11),('glass',2600,.16)]:
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
