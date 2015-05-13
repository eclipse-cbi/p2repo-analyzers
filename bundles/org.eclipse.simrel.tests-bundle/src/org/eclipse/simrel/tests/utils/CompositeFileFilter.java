/**
 * 
 */
package org.eclipse.simrel.tests.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author dhuebner
 *
 */
public class CompositeFileFilter implements FilenameFilter {
    private final Iterable<FilenameFilter> filters;

    private CompositeFileFilter(FilenameFilter... filters) {
        this.filters = Collections.unmodifiableList(Arrays.asList(filters));
    }

    /**
     * @param filters {@link FilenameFilter} to compose
     * @return a {@link FilenameFilter} which is an <code>or</code> composition
     *         of given filters.
     */
    public static CompositeFileFilter create(FilenameFilter... filters) {
        return new CompositeFileFilter(filters);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
     */
    @Override
    public boolean accept(File dir, String name) {
        boolean result = false;
        for (FilenameFilter filenameFilter : filters) {
            if (filenameFilter.accept(dir, name)) {
                return true;
            }
        }
        return result;
    }

}
