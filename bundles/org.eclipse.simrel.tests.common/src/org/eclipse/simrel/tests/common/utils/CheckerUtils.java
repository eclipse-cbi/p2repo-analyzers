/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.simrel.tests.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.simrel.tests.common.checker.IChecker;

/**
 * @author dhuebner - Initial contribution and API
 */
public class CheckerUtils {

	public static final Properties loadCheckerProperties(Class<? extends IChecker> checkerClazz) {
		Properties properties = new Properties();
		InputStream inStream = checkerClazz.getResourceAsStream(checkerClazz.getSimpleName() + ".properties");
		try {
			if (inStream != null)
				properties.load(inStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return properties;
	}
}
