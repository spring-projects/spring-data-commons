package org.springframework.data.repository.config;

public class MyOtherRepositoryImpl implements MyOtherRepositoryExtensions {
    @Override
    public String getImplementationId() {
        return getClass().getName();
    }
}
