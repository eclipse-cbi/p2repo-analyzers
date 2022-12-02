/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.cbi.p2repo.analyzers.jars;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.processors.pgp.PGPSignatureVerifier;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class PGPVerifier {

    public static boolean verify(File file, IArtifactRepository repository, StringBuilder errorOut,
            StringBuilder warningOut) {
        IArtifactKey artifactKey = toArtifactKey(file);
        IArtifactDescriptor[] artifactDescriptors = repository.getArtifactDescriptors(artifactKey);
        if (artifactDescriptors.length == 0) {
            return false;
        }
        if (artifactDescriptors.length > 1) {
            warningOut.append("Multiple descriptors found for " + artifactKey + System.lineSeparator());
        }
        for (IArtifactDescriptor artifactDescriptor : artifactDescriptors) {
            PGPSignatureVerifier verifier = new PGPSignatureVerifier();
            try {
                if (artifactDescriptor.getProperty(PGPSignatureVerifier.PGP_SIGNATURES_PROPERTY_NAME) == null) {
                    errorOut.append("No PGP signature found for " + artifactKey);
                    verifier.close();
                    return false;
                }
                verifier.initialize(null, null, artifactDescriptor);
                if (!checkVerifierStatus(verifier, warningOut, errorOut)) {
                    return false;
                }
                verifier.write(Files.readAllBytes(file.toPath()));
                verifier.close();
            } catch (IOException e) {
                errorOut.append(e.getMessage());
                return false;
            }
        }
        return true;
    }
    
    private static boolean checkVerifierStatus(PGPSignatureVerifier verifier, StringBuilder warningOut, StringBuilder errorOut) throws IOException {
        if (verifier.getStatus().getSeverity() == IStatus.WARNING) {
            warningOut.append(verifier.getStatus().getMessage());
        }
        if (verifier.getStatus().getSeverity() == IStatus.ERROR) {
            errorOut.append(verifier.getStatus().getMessage());
            verifier.close();
            return false;
        }
        return true;
    }

    private static IArtifactKey toArtifactKey(File fileToCheck) {
        String artifactId = fileToCheck.getName();
        artifactId = artifactId.replace(".jar", "");
        int last_ = artifactId.lastIndexOf('_');
        boolean isFeature = fileToCheck.getParentFile().getName().equals("features");
        return new ArtifactKey(isFeature ? PublisherHelper.ECLIPSE_FEATURE_CLASSIFIER : PublisherHelper.OSGI_BUNDLE_CLASSIFIER, artifactId.substring(0, last_) + (isFeature ? ".feature.jar" : ""), Version.create(artifactId.substring(last_ + 1)));
    }
}
