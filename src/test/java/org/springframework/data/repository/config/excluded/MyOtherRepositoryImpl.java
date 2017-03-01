package org.springframework.data.repository.config.excluded;

import org.springframework.data.repository.config.MyOtherRepositoryExtensions;

public class MyOtherRepositoryImpl implements MyOtherRepositoryExtensions {
    @Override
    public String getImplementationId() {
        return getClass().getName();
    }
}
