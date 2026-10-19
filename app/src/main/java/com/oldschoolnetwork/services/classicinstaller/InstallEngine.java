package com.oldschoolnetwork.services.classicinstaller;

import android.content.Context;
import android.content.pm.*;
import android.os.Build;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** All work is off the UI thread. System writes use owned paths and retain rollback data. */
public final class InstallEngine {
    public interface Log { void write(String text); }
    public static File prepare(Context context,ServiceCatalog.Release release) throws Exception {
        File directory=new File(context.getFilesDir(),"packages");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create package directory.");
        File file=new File(directory,release.asset);MessageDigest hash=MessageDigest.getInstance("SHA-256");
        try(InputStream input=context.getAssets().open("apks/"+release.asset);OutputStream output=new FileOutputStream(file)) {
            byte[] buffer=new byte[32768];int count;while((count=input.read(buffer))!=-1){output.write(buffer,0,count);hash.update(buffer,0,count);}
        }
        StringBuilder digest=new StringBuilder();for(byte value:hash.digest())digest.append(String.format(Locale.US,"%02x",value&255));
        if(!digest.toString().equalsIgnoreCase(release.sha256)){file.delete();throw new IOException("Checksum mismatch: "+release.name);}
        PackageManager manager=context.getPackageManager();PackageInfo archive=manager.getPackageArchiveInfo(file.getAbsolutePath(),PackageManager.GET_SIGNATURES);
        if(archive==null||!release.packageName.equals(archive.packageName)||archive.versionCode!=release.versionCode)throw new IOException("Package identity mismatch: "+release.name);
        try {
            PackageInfo existing=manager.getPackageInfo(release.packageName,PackageManager.GET_SIGNATURES);
            if(existing.versionCode>release.versionCode)throw new IOException("A newer version of "+release.name+" is already installed.");
            if(!Arrays.equals(existing.signatures,archive.signatures))throw new IOException("Signing certificate differs for "+release.name+". Use an APK signed by the same publisher.");
        } catch(PackageManager.NameNotFoundException ignored) {}
        return file;
    }
    public static String quote(String text){return "'"+text.replace("'","'\\''")+"'";}
    static String root(String script) throws Exception {
        final Process process=new ProcessBuilder("su","-c",script).redirectErrorStream(true).start();
        Timer timer=new Timer(true);timer.schedule(new TimerTask(){public void run(){process.destroy();}},120000);
        StringBuilder output=new StringBuilder();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;while((line=reader.readLine())!=null){if(output.length()<24000)output.append(line).append('\n');}
            int code=process.waitFor();if(code!=0)throw new IOException("Root operation failed ("+code+"). "+output);
        } finally {timer.cancel();}
        return output.toString();
    }
    public static String installSystem(Context context,List<ServiceCatalog.Release> releases,List<File> files,Log log) throws Exception {
        String mode=root("test \"$(id -u)\" = 0 || exit 1; if command -v magisk >/dev/null 2>&1 && test -d /data/adb/modules; then echo MAGISK; else echo LEGACY; fi").trim();
        if(!mode.endsWith("MAGISK")&&Build.VERSION.SDK_INT>28)throw new IOException("This Android requires a supported Magisk systemless installation. Direct writes to modern system partitions are not supported.");
        boolean magisk=mode.endsWith("MAGISK");
        log.write(magisk?"Preparing a Magisk system module…":"Preparing the classic system application directories…");
        String nonce=String.valueOf(System.currentTimeMillis());
        String backup=magisk?"/data/adb/on-classic-backup-"+nonce:"/data/local/tmp/on-classic-backup-"+nonce;
        String target=magisk?"/data/adb/modules/on_classic_services":"/system";
        String stage=magisk?"/data/adb/modules/.on-classic-stage-"+nonce:target;
        File permissionFile=new File(context.getFilesDir(),"privapp-permissions-on-classic.xml");
        if(Build.VERSION.SDK_INT>=26) {
            StringBuilder xml=new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?><permissions>");
            for(int i=0;i<releases.size();i++) {
                PackageInfo archive=context.getPackageManager().getPackageArchiveInfo(files.get(i).getAbsolutePath(),PackageManager.GET_PERMISSIONS);
                xml.append("<privapp-permissions package=\"").append(releases.get(i).packageName).append("\">");
                if(archive!=null&&archive.requestedPermissions!=null)for(String permission:archive.requestedPermissions) {
                    try {
                        PermissionInfo info=context.getPackageManager().getPermissionInfo(permission,0);
                        if((info.protectionLevel&0x10)!=0&&permission.matches("[a-zA-Z0-9_.]+"))xml.append("<permission name=\"").append(permission).append("\"/>");
                    }catch(PackageManager.NameNotFoundException ignored){}
                }
                xml.append("</privapp-permissions>");
            }
            xml.append("</permissions>");try(Writer writer=new OutputStreamWriter(new FileOutputStream(permissionFile),"UTF-8")){writer.write(xml.toString());}
        }
        StringBuilder script=new StringBuilder("set -e\ntest \"$(id -u)\" = 0\numask 022\n");
        if(!magisk) {
            script.append("was_ro=0\ntrap 'if test $was_ro -eq 1; then mount -o remount,ro /system || true; fi' EXIT\nif ! test -w /system; then was_ro=1; mount -o remount,rw /system; fi\n");
            script.append("test -w /system || { echo 'System partition is read-only. Use a supported Magisk setup.'; exit 1; }\n");
        }
        script.append("mkdir -p ").append(quote(backup)).append("\nchmod 700 ").append(quote(backup)).append("\n");
        StringBuilder rollback=new StringBuilder("#!/system/bin/sh\nset -e\n");
        if(!magisk)rollback.append("mount -o remount,rw /system || true\n");
        String permissionTarget=magisk?stage+"/system/etc/permissions/privapp-permissions-on-classic.xml":"/system/etc/permissions/privapp-permissions-on-classic.xml";
        if(Build.VERSION.SDK_INT>=26&&!magisk) {
            script.append("if test -e ").append(quote(permissionTarget)).append("; then cp ").append(quote(permissionTarget)).append(" ").append(quote(backup+"/permissions.xml")).append("; fi\n");
            rollback.append("rm -f ").append(quote(permissionTarget)).append("\nif test -e ").append(quote(backup+"/permissions.xml")).append("; then cp ").append(quote(backup+"/permissions.xml")).append(" ").append(quote(permissionTarget)).append("; fi\n");
        }
        List<String> destinations=new ArrayList<>();
        if(magisk) {
            rollback.append("rm -rf ").append(quote(target)).append("\nif test -d ").append(quote(backup+"/module")).append("; then cp -R ").append(quote(backup+"/module")).append(" ").append(quote(target)).append("; fi\n");
            script.append("if test -d ").append(quote(target)).append("; then cp -R ").append(quote(target)).append(" ").append(quote(backup+"/module")).append("; fi\n");
        }
        for(int i=0;i<releases.size();i++) {
            ServiceCatalog.Release release=releases.get(i);
            String relative=(Build.VERSION.SDK_INT>=19?"priv-app/":"app/")+"ON_"+release.id+(Build.VERSION.SDK_INT<19?".apk":"");
            String destination=magisk?stage+"/system/"+relative:target+"/"+relative;
            destinations.add(destination);
            if(!magisk) {
                ApplicationInfo existing=null;try{existing=context.getPackageManager().getApplicationInfo(release.packageName,0);}catch(PackageManager.NameNotFoundException ignored){}
                if(existing!=null&&(existing.flags&ApplicationInfo.FLAG_SYSTEM)!=0&&!existing.sourceDir.startsWith(destination+"/")&&!existing.sourceDir.equals(destination)) {
                    try { root("test -e "+quote(destination)); }
                    catch(Exception conflict) { throw new IOException("An existing system copy uses another directory: "+release.packageName+". Remove or migrate that copy using recovery first."); }
                }
                script.append("if test -e ").append(quote(destination)).append("; then cp -R ").append(quote(destination)).append(" ").append(quote(backup+"/ON_"+release.id)).append("; fi\n");
                rollback.append("rm -rf ").append(quote(destination)).append("\nif test -e ").append(quote(backup+"/ON_"+release.id)).append("; then cp -R ").append(quote(backup+"/ON_"+release.id)).append(" ").append(quote(destination)).append("; fi\n");
            }
        }
        if(!magisk)rollback.append("mount -o remount,ro /system || true\n");
        File rollbackFile=new File(context.getFilesDir(),"rollback.sh");try(Writer writer=new OutputStreamWriter(new FileOutputStream(rollbackFile),"UTF-8")){writer.write(rollback.toString());}
        script.append("cat ").append(quote(rollbackFile.getAbsolutePath())).append(" > ").append(quote(backup+"/rollback.sh")).append("\nchmod 700 ").append(quote(backup+"/rollback.sh")).append("\n");
        script.append("rollback() { result=$?; trap - EXIT HUP INT TERM; if test $result -ne 0; then sh ").append(quote(backup+"/rollback.sh")).append(" || true; fi; ");
        if(magisk)script.append("rm -rf ").append(quote(stage)).append("; ");
        else script.append("if test $was_ro -eq 1; then mount -o remount,ro /system || true; fi; ");
        script.append("exit $result; }\ntrap rollback EXIT HUP INT TERM\n");
        if(Build.VERSION.SDK_INT>=26) {
            script.append("mkdir -p ").append(quote(new File(permissionTarget).getParent())).append("\ncat ").append(quote(permissionFile.getAbsolutePath())).append(" > ").append(quote(permissionTarget)).append("\nchmod 644 ").append(quote(permissionTarget)).append("\nchown 0:0 ").append(quote(permissionTarget)).append("\n");
            if(!magisk)script.append("if command -v restorecon >/dev/null 2>&1; then restorecon ").append(quote(permissionTarget)).append("; fi\n");
        }
        for(int i=0;i<releases.size();i++) {
            String destination=destinations.get(i);
            // Pre-4.4 uses flat /system/app APK files; a dedicated directory is supported from 4.4.
            String apk=Build.VERSION.SDK_INT<19?destination:destination+"/base.apk";
            if(Build.VERSION.SDK_INT>=19)script.append("mkdir -p ").append(quote(destination)).append("\nchmod 755 ").append(quote(destination)).append("\nchown 0:0 ").append(quote(destination)).append("\n");
            script.append("cat ").append(quote(files.get(i).getAbsolutePath())).append(" > ").append(quote(apk+".new")).append("\nchown 0:0 ").append(quote(apk+".new")).append("\nchmod 644 ").append(quote(apk+".new")).append("\nmv ").append(quote(apk+".new")).append(" ").append(quote(apk)).append("\n");
            if(!magisk)script.append("if command -v restorecon >/dev/null 2>&1; then restorecon -R ").append(quote(destination)).append("; fi\n");
        }
        if(magisk) {
            script.append("printf '%s\\n' 'id=on_classic_services' 'name=Oldschool Network Services' 'version=2026.10.19' 'versionCode=20261019' 'author=Oldschool Network' 'description=Selected ON services as system apps' > ").append(quote(stage+"/module.prop")).append("\nchmod 644 ").append(quote(stage+"/module.prop")).append("\nrm -rf ").append(quote(target)).append("\nmv ").append(quote(stage)).append(" ").append(quote(target)).append("\n");
        }
        script.append("echo ").append(quote("Staged successfully. Rollback: "+backup+"/rollback.sh")).append("\n");
        log.write(root(script.toString()));
        return backup+"/rollback.sh";
    }
}
