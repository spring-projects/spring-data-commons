package org.springframework.data.repository.query;

import java.util.Locale;

import org.springframework.util.StringUtils;


/**
 * Strategy interface for which way to lookup {@link RepositoryQuery}s.
 * 
 * @author Oliver Gierke
 */
public interface QueryLookupStrategy<Q extends QueryMethod> {

    public static enum Key {

        CREATE, USE_DECLARED_QUERY, CREATE_IF_NOT_FOUND;

        /**
         * Returns a strategy key from the given XML value.
         * 
         * @param xml
         * @return a strategy key from the given XML value
         */
        public static Key create(String xml) {

            if (!StringUtils.hasText(xml)) {
                return null;
            }

            return valueOf(xml.toUpperCase(Locale.US).replace("-", "_"));
        }
    }


    /**
     * Resolves a {@link RepositoryQuery} from the given {@link QueryMethod}
     * that can be executed afterwards.
     * 
     * @param method
     * @return
     */
    RepositoryQuery resolveQuery(Q method);
}