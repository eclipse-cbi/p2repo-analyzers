/**
 * 
 */
package org.eclipse.simrel.tests.common;

import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author dhuebner
 *
 */
@SuppressWarnings("restriction")
public class Activator implements BundleActivator {

	private static BundleContext context;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		Activator.context = context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
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
