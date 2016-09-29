/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @bug 4856966
 * @summary Test the new RSA provider can verify all the RSA certs in the cacerts file
 * @author Andreas Sterbenz
 * @library ..
 * @library ../../../../java/security/testlibrary
 */

// this test serves as our known answer test

import java.io.*;
import java.util.*;

import java.security.*;
import java.security.cert.*;

public class TestCACerts extends PKCS11Test {

    private final static char SEP = File.separatorChar;

    public static void main(String[] args) throws Exception {
        main(new TestCACerts());
    }

    public void main(Provider p) throws Exception {

        /*
         * Use Solaris SPARC 11.2 or later to avoid an intermittent failure
         * when running SunPKCS11-Solaris (8044554)
         */
        if (p.getName().equals("SunPKCS11-Solaris") &&
            System.getProperty("os.name").equals("SunOS") &&
            System.getProperty("os.arch").equals("sparcv9") &&
            System.getProperty("os.version").compareTo("5.11") <= 0 &&
            getDistro().compareTo("11.2") < 0) {

            System.out.println("SunPKCS11-Solaris provider requires " +
                "Solaris SPARC 11.2 or later, skipping");
            return;
        }

        long start = System.currentTimeMillis();
        Providers.setAt(p, 1);
        try {
            String PROVIDER = p.getName();
            String javaHome = System.getProperty("java.home");
            String caCerts = javaHome + SEP + "lib" + SEP + "security" + SEP + "cacerts";
            InputStream in = new FileInputStream(caCerts);
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(in, null);
            in.close();
            for (Enumeration e = ks.aliases(); e.hasMoreElements(); ) {
                String alias = (String)e.nextElement();
                if (ks.isCertificateEntry(alias)) {
                    System.out.println("* Testing " + alias + "...");
                    X509Certificate cert = (X509Certificate)ks.getCertificate(alias);
                    PublicKey key = cert.getPublicKey();
                    String alg = key.getAlgorithm();
                    if (alg.equals("RSA")) {
                        System.out.println("Signature algorithm: " + cert.getSigAlgName());
                        cert.verify(key, PROVIDER);
                    } else {
                        System.out.println("Skipping cert with key: " + alg);
                    }
                } else {
                    System.out.println("Skipping alias " + alias);
                }
            }
            long stop = System.currentTimeMillis();
            System.out.println("All tests passed (" + (stop - start) + " ms).");
         } finally {
            Security.removeProvider(p.getName());
         }
    }
}
