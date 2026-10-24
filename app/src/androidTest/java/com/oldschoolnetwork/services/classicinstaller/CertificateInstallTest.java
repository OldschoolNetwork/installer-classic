package com.oldschoolnetwork.services.classicinstaller;

import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;
import com.oldschoolnetwork.certificates.RootCertificates;
import java.io.*;
import java.security.cert.*;
import java.util.*;

/** Runs only on the disposable rooted emulator, never on a user's device. */
public class CertificateInstallTest {
    private android.app.Instrumentation getInstrumentation(){return InstrumentationRegistry.getInstrumentation();}
    @Test
    public void testVerifiedBundleAndHash() throws Exception {
        List<X509Certificate> roots=RootCertificates.certificates(getInstrumentation().getTargetContext());
        assertEquals(118,roots.size());
        assertTrue(RootCertificates.subjectHash(roots.get(0)).matches("[0-9a-f]{8}"));
    }
    private Set<String> names(){return new HashSet<String>(Arrays.asList(new File("/system/etc/security/cacerts").list()));}
    @Test
    @org.junit.Ignore("AOSP emulator su accepts shell/root only; transaction runs through CertificateRootSmoke as shell root instead")
    public void testInstallAndRestorePreserveDeviceRoots() throws Exception {
        assertTrue("Disposable emulator required",Arrays.asList("goldfish","ranchu").contains(android.os.Build.HARDWARE));
        Set<String> before=names();
        android.content.Context c=getInstrumentation().getTargetContext();
        try {
            String recovery=RootCertificates.install(c,new RootCertificates.Log(){public void write(String text){}});
            assertNotNull(recovery);assertTrue(RootCertificates.canRestore(c));
            assertTrue(names().containsAll(before));assertTrue(names().size()>before.size());
            RootCertificates.restore(c,new RootCertificates.Log(){public void write(String text){}});
            assertEquals(before,names());assertFalse(RootCertificates.canRestore(c));
        }finally {if(RootCertificates.canRestore(c))RootCertificates.restore(c,new RootCertificates.Log(){public void write(String text){}});}
    }
}
