/**
 * 
 */
package org.eclipse.simrel.tests.common;

import java.net.URI;

import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;

/**
 * @author dhuebner
 *
 */
public class P2RepositoryDescription {

	private URI url;
	private IMetadataRepository repo;
	private IFileArtifactRepository artRepo;

	public URI getP2RepoFolder() {
		return url;
	}

	public void setRepoURL(URI p2RepoURL) {
		this.url = p2RepoURL;
	}

	public void setMetadataRepository(IMetadataRepository repo) {
		this.repo = repo;
	}

	public IMetadataRepository getMetadataRepository() {
		return repo;
	}

	public void setArtifactRepository(IFileArtifactRepository artRepo) {
		this.artRepo = artRepo;
	}

	public IFileArtifactRepository getArtifactRepository() {
		return artRepo;
	}
}
