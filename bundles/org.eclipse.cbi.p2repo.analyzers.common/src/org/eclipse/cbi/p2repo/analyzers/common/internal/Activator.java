/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.cbi.p2repo.analyzers.common.internal;

import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author dhuebner - Initial contribution and API
 */

public class Activator implements BundleActivator {

	private static BundleContext context;

	@Override
	public void start(final BundleContext context) throws Exception {
		Activator.context = context;
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		Activator.context = null;
	}

	public static IMetadataRepositoryManager getMetadataRepositoryManager() {
		return (IMetadataRepositoryManager) agent().getService(IMetadataRepositoryManager.SERVICE_NAME);
	}

	public static IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) agent().getService(IArtifactRepositoryManager.SERVICE_NAME);
	}

	private static IProvisioningAgent agent() {
		return (IProvisioningAgent) ServiceHelper.getService(context, IProvisioningAgent.SERVICE_NAME);
	}

}
