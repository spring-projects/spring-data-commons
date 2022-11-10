package com.custom.querydslpredicatebuilder;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.binding.PathInformation;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.util.TypeInformation;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.Optional;

@Component("querydslPredicateBuilder")
public class QuerydslPredicateBuilderCustom extends QuerydslPredicateBuilder {
    /**
     * Creates a custom {@link QuerydslPredicateBuilder}
     *
     */
    public QuerydslPredicateBuilderCustom() {
        super(DefaultConversionService.getSharedInstance(), new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE).getEntityPathResolver());
    }

    //TODO Copy same logic as original class and just changed 'builder::or'
    @Override
    public Predicate getPredicate(TypeInformation<?> type, MultiValueMap<String, ?> values, QuerydslBindings bindings) {
        Assert.notNull(bindings, "Context must not be null");

        BooleanBuilder builder = new BooleanBuilder();

        if (values.isEmpty()) {
            return getPredicate(builder);
        }

        for (var entry : values.entrySet()) {

            if (isSingleElementCollectionWithEmptyItem(entry.getValue())) {
                continue;
            }

            String path = entry.getKey();

            if (!bindings.isPathAvailable(path, type)) {
                continue;
            }

            PathInformation propertyPath = bindings.getPropertyPath(path, type);

            if (propertyPath == null) {
                continue;
            }

            Collection<Object> value = convertToPropertyPathSpecificType(entry.getValue(), propertyPath, conversionService);
            Optional<Predicate> predicate = invokeBinding(propertyPath, bindings, value, resolver, defaultBinding);

            predicate.ifPresent(builder::or);
        }

        return getPredicate(builder);
    }
}
