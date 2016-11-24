/*******************************************************************************
 * Copyright (c) 2016 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cbi.p2repo.analyzers.ui.popup.actions;

import org.eclipse.osgi.util.NLS;

/**
 * @author dhuebner - Initial contribution and API
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.cbi.p2repo.analyzers.ui.popup.actions.messages"; //$NON-NLS-1$
	public static String P2AnalyzerAction_dialog_info_msg;
	public static String P2AnalyzerAction_dialog_info_title;
	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
