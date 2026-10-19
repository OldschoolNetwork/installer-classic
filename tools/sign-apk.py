"""Sign an APK with the persistent ON key. Passwords stay out of command lines."""
import argparse,ctypes,ctypes.wintypes as w,os,secrets,subprocess,sys
from pathlib import Path

TARGET='OldschoolNetwork/APKSigning'
class Credential(ctypes.Structure):
    _fields_=[('Flags',w.DWORD),('Type',w.DWORD),('TargetName',w.LPWSTR),('Comment',w.LPWSTR),('LastWritten',w.FILETIME),('CredentialBlobSize',w.DWORD),('CredentialBlob',ctypes.POINTER(ctypes.c_ubyte)),('Persist',w.DWORD),('AttributeCount',w.DWORD),('Attributes',ctypes.c_void_p),('TargetAlias',w.LPWSTR),('UserName',w.LPWSTR)]
def store_password(password):
    vault=ctypes.WinDLL('Advapi32.dll',use_last_error=True);vault.CredWriteW.argtypes=[ctypes.POINTER(Credential),w.DWORD]
    data=password.encode('utf-16-le');blob=(ctypes.c_ubyte*len(data)).from_buffer_copy(data)
    credential=Credential();credential.Type=1;credential.TargetName=TARGET;credential.UserName='on-release';credential.CredentialBlobSize=len(data);credential.CredentialBlob=blob;credential.Persist=2
    if not vault.CredWriteW(ctypes.byref(credential),0):raise ctypes.WinError(ctypes.get_last_error())
def read_password():
    if os.name!='nt':return os.environ['ON_SIGNING_PASSWORD']
    vault=ctypes.WinDLL('Advapi32.dll',use_last_error=True);pointer=ctypes.POINTER(Credential)();vault.CredReadW.argtypes=[w.LPCWSTR,w.DWORD,w.DWORD,ctypes.POINTER(ctypes.POINTER(Credential))];vault.CredFree.argtypes=[ctypes.c_void_p]
    if not vault.CredReadW(TARGET,1,0,ctypes.byref(pointer)):raise ctypes.WinError(ctypes.get_last_error())
    try:return ctypes.string_at(pointer.contents.CredentialBlob,pointer.contents.CredentialBlobSize).decode('utf-16-le')
    finally:vault.CredFree(pointer)
def call(arguments,environment):
    result=subprocess.run([str(arg) for arg in arguments],env=environment,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
    if result.returncode:raise RuntimeError('Signing tool failed: '+result.stderr[:1500])
    return result.stdout
def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('action',choices=['init','sign','verify']);parser.add_argument('--keystore',type=Path);parser.add_argument('--alias',default='on-release');parser.add_argument('--jdk',type=Path,required=True);parser.add_argument('--sdk',type=Path);parser.add_argument('--build-tools',default='28.0.3');parser.add_argument('--input',type=Path);parser.add_argument('--output',type=Path);args=parser.parse_args()
    environment=os.environ.copy();environment['JAVA_HOME']=str(args.jdk)
    if args.action=='init':
        if not args.keystore or args.keystore.exists():raise RuntimeError('Choose a new keystore path; existing keys are never overwritten.')
        password=secrets.token_urlsafe(40);environment['ON_SIGNING_PASSWORD']=password
        args.keystore.parent.mkdir(parents=True,exist_ok=True)
        call([args.jdk/'bin/keytool.exe','-genkeypair','-keystore',args.keystore,'-storetype','JKS','-alias',args.alias,'-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=Oldschool Network, O=Oldschool Network','-storepass:env','ON_SIGNING_PASSWORD','-keypass:env','ON_SIGNING_PASSWORD'],environment)
        store_password(password)
        recovery=args.keystore.parent/'ON_SIGNING_RECOVERY_PRIVATE.txt'
        recovery.write_text('PRIVATE — do not publish or commit.\nKeystore: '+str(args.keystore)+'\nAlias: '+args.alias+'\nKeystore and key password: '+password+'\n\nBack up this file and the keystore together in a private, secure location.\n',encoding='utf-8')
        certificate=call([args.jdk/'bin/keytool.exe','-exportcert','-rfc','-keystore',args.keystore,'-alias',args.alias,'-storepass:env','ON_SIGNING_PASSWORD'],environment)
        (args.keystore.parent/'on-release-public.pem').write_text(certificate,encoding='ascii')
        print('Persistent ON signing key created. Password stored in Windows Credential Manager and the private recovery file.');return
    if not args.sdk or not args.input:raise RuntimeError('--sdk and --input are required.')
    signer=args.sdk/'build-tools'/args.build_tools/'lib/apksigner.jar'
    if args.action=='sign':
        if not args.output or not args.keystore:raise RuntimeError('--output and --keystore are required for signing.')
        if args.input.resolve()==args.output.resolve():raise RuntimeError('The output must differ from the original APK.')
        args.output.parent.mkdir(parents=True,exist_ok=True);environment['ON_SIGNING_PASSWORD']=read_password()
        aligned=args.output.with_suffix('.aligned.apk')
        call([args.sdk/'build-tools'/args.build_tools/'zipalign.exe','-p','-f','4',args.input,aligned],environment)
        try:call([args.jdk/'bin/java.exe','-jar',signer,'sign','--ks',args.keystore,'--ks-key-alias',args.alias,'--ks-pass','env:ON_SIGNING_PASSWORD','--key-pass','env:ON_SIGNING_PASSWORD','--min-sdk-version','14','--v1-signing-enabled','true','--v2-signing-enabled','true','--out',args.output,aligned],environment)
        finally:aligned.unlink(missing_ok=True)
        target=args.output
    else:target=args.input
    print(call([args.jdk/'bin/java.exe','-jar',signer,'verify','--verbose','--print-certs','--min-sdk-version','14',target],environment))
if __name__=='__main__':main()
