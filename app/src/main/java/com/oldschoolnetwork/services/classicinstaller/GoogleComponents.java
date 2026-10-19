package com.oldschoolnetwork.services.classicinstaller;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.File;
import java.util.*;

/** Exact, reviewable legacy service list. No wildcard removal of Google-labelled apps. */
public final class GoogleComponents {
    private static final String[] PACKAGES={"com.google.android.gms","com.google.android.gsf","com.google.android.gsf.login","com.android.vending","com.google.android.syncadapters.contacts","com.google.android.syncadapters.calendar"};
    public static final class Entry {public final String packageName,label,path;Entry(String name,String label,String path){this.packageName=name;this.label=label;this.path=path;}}
    public static List<Entry> installed(Context context){
        List<Entry> result=new ArrayList<>();PackageManager manager=context.getPackageManager();
        for(String name:PACKAGES)try{ApplicationInfo info=manager.getApplicationInfo(name,0);result.add(new Entry(name,manager.getApplicationLabel(info).toString(),info.sourceDir));}catch(PackageManager.NameNotFoundException ignored){}
        return result;
    }
    public static void validate(List<Entry> entries)throws Exception{
        for(Entry entry:entries){
            if(!Arrays.asList(PACKAGES).contains(entry.packageName))throw new Exception("Unsupported removal target.");
            String path=new File(entry.path).getCanonicalPath();
            if(!path.startsWith("/system/app/")&&!path.startsWith("/system/priv-app/"))throw new Exception("Google component "+entry.packageName+" has a user-installed update or another system location. Remove its update in Android Settings first, or keep Google services for this installation.");
            if(!path.endsWith(".apk"))throw new Exception("Unsupported Google component location.");
        }
    }
}
