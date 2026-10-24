package com.oldschoolnetwork.services.classicinstaller;
import com.oldschoolnetwork.certificates.RootCertificates;
import android.content.Context;
import java.io.File;
import java.util.*;
/** Disposable emulator only; invoked by adb shell su through app_process. */
public final class CertificateRootSmoke {
    private static Set<String> names(){return new HashSet<String>(Arrays.asList(new File("/system/etc/security/cacerts").list()));}
    public static void main(String[] args) throws Exception {
        if(!Arrays.asList("goldfish","ranchu").contains(android.os.Build.HARDWARE))throw new Exception("Emulator only");
        android.os.Looper.prepareMainLooper();
        Class<?> thread=Class.forName("android.app.ActivityThread");Object activity=thread.getMethod("systemMain").invoke(null);
        Context system=(Context)thread.getMethod("getSystemContext").invoke(activity);
        Context context=system.createPackageContext("com.oldschoolnetwork.services.classicinstaller",Context.CONTEXT_IGNORE_SECURITY);
        Set<String> before=names();
        RootCertificates.Log log=new RootCertificates.Log(){public void write(String s){System.out.println(s);}};
        try {
            String recovery=RootCertificates.install(context,log);
            if(recovery==null||!names().containsAll(before)||names().size()<=before.size())throw new Exception("Installation assertion failed");
            RootCertificates.restore(context,log);
            if(!before.equals(names()))throw new Exception("Original system certificates were not restored");
            System.out.println("PASS: install and recovery preserve the original certificate store");
            java.io.FileWriter result=new java.io.FileWriter("/data/local/tmp/on-certificate-test-result.txt");
            result.write("PASS: install and recovery preserve "+before.size()+" original certificate files\n");result.close();
        }finally{if(RootCertificates.canRestore(context))RootCertificates.restore(context,log);}
    }
}
