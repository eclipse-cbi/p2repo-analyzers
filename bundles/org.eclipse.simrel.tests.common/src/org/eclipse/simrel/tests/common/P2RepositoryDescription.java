/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.simrel.tests.common;

import java.net.URI;

import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * @author dhuebner - Initial contribution and API
 */
public class P2RepositoryDescription {

	private URI url;
	private IMetadataRepository repo;
	private IFileArtifactRepository artRepo;

	public URI getP2RepoFolder() {
		return this.url;
	}

	public void setRepoURL(final URI p2RepoURL) {
		this.url = p2RepoURL;
	}

	public void setMetadataRepository(final IMetadataRepository repo) {
		this.repo = repo;
	}

	public IMetadataRepository getMetadataRepository() {
		return this.repo;
	}

	public void setArtifactRepository(final IFileArtifactRepository artRepo) {
		this.artRepo = artRepo;
	}

	public IFileArtifactRepository getArtifactRepository() {
		return this.artRepo;
	}
}
