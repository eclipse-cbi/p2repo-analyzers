/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.cbi.p2repo.analyzers;

import java.io.File;

import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

public class TestActivator implements BundleActivator {
    public static BundleContext     context;
    private static PackageAdmin     packageAdmin    = null;
    private static ServiceReference packageAdminRef = null;

    public static BundleContext getContext() {
        return context;
    }

    /*
     * Return a file handle to the framework log file, or null if it is not
     * available.
     */
    public static File getLogFile() {
        FrameworkLog log = (FrameworkLog) ServiceHelper.getService(context, FrameworkLog.class.getName());
        return log == null ? null : log.getFile();
    }

    public void start(BundleContext context) throws Exception {
        TestActivator.context = context;
        packageAdminRef = context.getServiceReference(PackageAdmin.class.getName());
        packageAdmin = (PackageAdmin) context.getService(packageAdminRef);
    }

    public void stop(BundleContext context) throws Exception {
        TestActivator.context = null;
    }

    public static Bundle getBundle(String symbolicName) {
        if (packageAdmin == null) {
            return null;
        }
        Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
        if (bundles == null) {
            return null;
        }
        // Return the first bundle that is not installed or uninstalled
        for (int i = 0; i < bundles.length; i++) {
            if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
                return bundles[i];
            }
        }
        return null;
    }

}
