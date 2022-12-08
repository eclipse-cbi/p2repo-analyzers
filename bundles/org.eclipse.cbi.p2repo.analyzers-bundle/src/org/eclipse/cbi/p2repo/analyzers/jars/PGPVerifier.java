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
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

public class PGPVerifier {

    public static boolean verify(IArtifactDescriptor artifactDescriptor, File file, IArtifactRepository repository,
            StringBuilder errorOut, StringBuilder warningOut) {
        IProvisioningAgent agent = repository.getProvisioningAgent();
        PGPSignatureVerifier verifier = new PGPSignatureVerifier();
        try {
            if (artifactDescriptor.getProperty(PGPSignatureVerifier.PGP_SIGNATURES_PROPERTY_NAME) == null) {
                errorOut.append("No PGP signature found for " + artifactDescriptor.getArtifactKey());
                verifier.close();
                return false;
            }
            verifier.initialize(agent, null, artifactDescriptor);
            if (!checkVerifierStatus(verifier, warningOut, errorOut)) {
                return false;
            }
            verifier.write(Files.readAllBytes(file.toPath()));
            verifier.close();
        } catch (IOException e) {
            errorOut.append(e.getMessage());
            return false;
        }
        return true;
    }

    private static boolean checkVerifierStatus(PGPSignatureVerifier verifier, StringBuilder warningOut, StringBuilder errorOut)
            throws IOException {
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
}
