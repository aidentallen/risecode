#!/usr/bin/env python3
"""Prepare pinned CC0 sources and a self-contained risecode catalog. No Blender or runtime conversion."""
import base64, concurrent.futures, hashlib, io, json, math, pathlib, struct, urllib.parse, urllib.request
from PIL import Image, ImageDraw
ROOT=pathlib.Path(__file__).resolve().parents[1]
OUT=ROOT/'build/cosmetics-catalog/v1'
BUNDLE=ROOT/'src/main/resources/assets/riserecode/cosmetics'
MODELS='db3df04d1e4714298a09510b26fb6de6645138a2'
PARTICLES='ab7086639ee73be31abd87feb21bf1402d4e8144'
SOURCE='https://quaternius.com/packs/cutemonsters.html'
BASE='https://raw.githubusercontent.com/agentkaerf/FreeModels/'+MODELS+'/'
NAMES=['Alien','Alien_Tall','Bat','Bee','Cactus','Chicken','Crab','Cthulhu','Cyclops','Deer','Demon','Ghost','GreenDemon','Mushroom','Panda','Penguin','Pig','Skull','Tree','YellowDragon','Yeti']
entries=[]
def fetch(url):
    request=urllib.request.Request(url,headers={'User-Agent':'risecode-asset-preparation/1'})
    return urllib.request.urlopen(request,timeout=45).read()
def store(relative,data,bundled=True):
    path=OUT/relative;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
    if bundled:
        path=BUNDLE/relative;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
    return hashlib.sha256(data).hexdigest()
def png(image):
    f=io.BytesIO();image.save(f,format='PNG');return f.getvalue()
def entry(id,name,category,asset='none',data=b'',creator='risecode',source='https://github.com/aidentallen/risecode',license='MIT',bundled=True,preview=''):
    h=store(asset,data,bundled) if data else '-'
    return dict(id=id,name=name,category=category,kind=category.lower(),asset=asset,sha256=h,bytes=len(data),creator=creator,source=source,license=license,preview=preview)
def model(name):
    path='Cute Animated Monsters - Aug 2020/glTF/'+name+'.gltf';raw=fetch(BASE+urllib.parse.quote(path));g=json.loads(raw)
    binary=base64.b64decode(g['buffers'][0]['uri'].split(',',1)[1]);g['buffers']=[{'byteLength':len(binary)}]
    g['asset']['extras']={'creator':'Quaternius','license':'CC0-1.0','source':SOURCE,'originalSha256':hashlib.sha256(raw).hexdigest()}
    js=json.dumps(g,separators=(',',':')).encode();js+=b' '*((-len(js))%4);binary+=b'\0'*((-len(binary))%4)
    glb=struct.pack('<III',0x46546c67,2,28+len(js)+len(binary))+struct.pack('<II',len(js),0x4e4f534a)+js+struct.pack('<II',len(binary),0x004e4942)+binary
    id='pet-'+name.lower().replace('_','-');asset='objects/'+hashlib.sha256(glb).hexdigest()+'.glb'
    bundled=name in ['Bee','Ghost','YellowDragon']
    # Preview uses the actual source mesh and texture, projected with painter-sorted triangles.
    primitive=g['meshes'][0]['primitives'][0]
    def acc(index):
        a=g['accessors'][index];v=g['bufferViews'][a['bufferView']];fmt={5126:'f',5125:'I',5123:'H',5121:'B'}[a['componentType']];n={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[a['type']];stride=v.get('byteStride',struct.calcsize(fmt)*n);offset=v.get('byteOffset',0)+a.get('byteOffset',0)
        return [struct.unpack_from('<'+fmt*n,binary,offset+i*stride) for i in range(a['count'])]
    positions=acc(primitive['attributes']['POSITION']);uv=acc(primitive['attributes']['TEXCOORD_0']);indices=[x[0] for x in acc(primitive['indices'])]
    im=g['images'][0];v=g['bufferViews'][im['bufferView']];texture=Image.open(io.BytesIO(binary[v.get('byteOffset',0):v.get('byteOffset',0)+v['byteLength']])).convert('RGB')
    projected=[(x*.86-z*.5,-y+(x*.5+z*.86)*.2,x*.5+z*.86) for x,y,z in positions];xs=[p[0] for p in projected];ys=[p[1] for p in projected];scale=108/max(max(xs)-min(xs),max(ys)-min(ys),.001)
    pic=Image.new('RGBA',(128,128),(0,0,0,0));draw=ImageDraw.Draw(pic)
    tris=[indices[i:i+3] for i in range(0,len(indices),3)]
    for tri in sorted(tris,key=lambda t:sum(projected[i][2] for i in t),reverse=True):
        u=sum(uv[i][0] for i in tri)/3;vv=sum(uv[i][1] for i in tri)/3;color=texture.getpixel((int(u*(texture.width-1))%texture.width,int(vv*(texture.height-1))%texture.height))
        draw.polygon([(64+(projected[i][0]-(min(xs)+max(xs))/2)*scale,64+(projected[i][1]-(min(ys)+max(ys))/2)*scale) for i in tri],fill=(*color,255))
    preview='previews/'+id+'.png';store(preview,png(pic),True)
    return entry(id,'Mini Dragon' if name=='YellowDragon' else name.replace('_',' '),'COMPANION',asset,glb,'Quaternius',SOURCE,'CC0-1.0',bundled,preview)
def main():
    OUT.mkdir(parents=True,exist_ok=True);BUNDLE.mkdir(parents=True,exist_ok=True)
    with concurrent.futures.ThreadPoolExecutor(4) as pool:entries.extend(pool.map(model,NAMES))
    for name in ['circle_01','light_01','flame_01','magic_01','smoke_03','spark_01','star_01','trace_01']:
        data=fetch('https://raw.githubusercontent.com/Calinou/kenney-particle-pack/'+PARTICLES+'/addons/kenney_particle_pack/'+name+'.png');store('sprites/'+name+'.png',data)
    store('KENNEY-LICENSE.txt',fetch('https://raw.githubusercontent.com/Calinou/kenney-particle-pack/'+PARTICLES+'/LICENSE.txt'))
    store('QUATERNIUS-LICENSE.txt',('Cute Animated Monsters Pack by Quaternius. CC0-1.0.\nSource: '+SOURCE+'\nPinned glTF mirror: '+BASE+'\nConverted to GLB and fitted as cosmetic companions; textures and animation retained.\n').encode())
    for name in ['solid','gradient','chevron','constellation']:
        im=Image.new('RGBA',(64,32));pixels=im.load()
        for y in range(32):
            for x in range(64):
                v=1-y/40 if name=='gradient' else .85
                if name=='chevron':v=.95 if (x+y//2)%12<6 else .35
                if name=='constellation':v=1 if (x*13+y*7)%43<2 else .2
                pixels[x,y]=(int(255*v),int(255*v),int(255*v),255)
        data=png(im);entries.append(entry('cape-'+name,name.title(),'CAPE','capes/'+name+'.png',data))
    for category,names in [('WINGS',['Dragon','Butterfly','Mechanical']),('HEAD',['Halo','Horns','Cat Ears','Crown']),('TAIL',['Cat','Fox','Dragon']),('TRAIL',['Ribbon','Sparks','Stars']),('AURA',['Orbiting Motes','Magic Ring','Wisps']),('PARTICLES',['Hearts','Snowflakes','Embers'])]:
        for name in names:entries.append(entry(category.lower()+'-'+name.lower().replace(' ','-'),name,category))
    store('CC0-1.0.txt',(ROOT/'src/main/resources/licenses/CC0-1.0.txt').read_bytes())
    store('LICENSE-MIT.txt',(ROOT/'LICENSE').read_bytes())
    index=json.dumps({'version':1,'entries':entries},indent=2).encode()+b'\n';store('index.json',index)
    (ROOT/'build/cosmetics-source-manifest.json').write_text(json.dumps({'modelsCommit':MODELS,'particleCommit':PARTICLES,'catalogSha256':hashlib.sha256(index).hexdigest()},indent=2)+'\n')
    print('Prepared',len(entries),'cosmetics; 18 downloadable companions, 26 bundled items.')
if __name__=='__main__':main()
