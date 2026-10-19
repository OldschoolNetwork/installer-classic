"""Prepare the installer payload from signed Core and Store APKs."""
import argparse,hashlib,json,re,shutil,subprocess
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--core',type=Path,required=True);parser.add_argument('--store',type=Path,required=True);parser.add_argument('--sdk',type=Path,required=True);parser.add_argument('--jdk',type=Path,required=True);args=parser.parse_args()
project=Path(__file__).resolve().parents[1];assets=project/'app/src/main/assets';(assets/'apks').mkdir(parents=True,exist_ok=True);releases=[];certificates=set()
for id,name,expected,source,deps in [('core','ON Core Services','com.oldschoolnetwork.core',args.core,[]),('store','ON Store','com.oldschoolnetwork.store',args.store,['core'])]:
    tools=args.sdk/'build-tools/28.0.3'
    badging=subprocess.check_output([str(tools/'aapt.exe'),'dump','badging',str(source)],text=True,encoding='utf-8')
    identity=re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",badging);sdk=re.search(r"sdkVersion:'(\d+)'",badging)
    if not identity or identity[1]!=expected:raise RuntimeError('Unexpected APK package: '+str(source))
    verification=subprocess.check_output([str(args.jdk/'bin/java.exe'),'-jar',str(tools/'lib/apksigner.jar'),'verify','--print-certs','--min-sdk-version','14',str(source)],text=True,encoding='utf-8')
    if 'CN=Android Debug' in verification:raise RuntimeError('Use the persistent ON release key, not the Android debug key.')
    fingerprint=re.search(r'Signer #1 certificate SHA-256 digest: (\w+)',verification)
    if not fingerprint:raise RuntimeError('Could not verify the signing certificate.')
    certificates.add(fingerprint[1]);version=re.sub(r'^(ONC-)?alpha-','',identity[3]);filename=('ON-Core-Services-' if id=='core' else 'ON-Store-')+version+'.apk'
    native=re.search(r"native-code:(.*)",badging);abis=re.findall(r"'([^']+)'",native[1]) if native else []
    if len(abis)>1:raise RuntimeError('Declare multiple ABI variants explicitly before bundling a multi-ABI native APK.')
    release=dict(id=id,name=name,package_name=identity[1],version_code=int(identity[2]),version=version,min_sdk=int(sdk[1]),abi=abis[0] if abis else 'universal',sha256=hashlib.sha256(source.read_bytes()).hexdigest(),asset=filename,dependencies=deps,signing='release',certificate_sha256=fingerprint[1])
    shutil.copyfile(source,assets/'apks'/filename);releases.append(release)
if len(certificates)!=1:raise RuntimeError('Core and Store must use the same ON signing key.')
(assets/'services.json').write_text(json.dumps({'schema':1,'releases':releases},indent=2),encoding='utf-8')
print('Installer payload prepared from verified signed APKs. No build or upload was started.')
