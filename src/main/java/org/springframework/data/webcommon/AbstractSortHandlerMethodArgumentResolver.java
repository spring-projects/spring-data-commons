package org.springframework.data.webcommon;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.data.web.SpringDataAnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.springframework.data.domain.Sort.*;
import static org.springframework.data.web.SortDefault.SortDefaults;

/**
 * Abstract class for Web&WebFLux SortHandlerMethodArgumentResolver
 *
 * @author Eugene Utkin
 * @see org.springframework.data.web.SortHandlerMethodArgumentResolver
 * @see org.springframework.data.webflux.SortHandlerMethodArgumentResolver
 */
public class AbstractSortHandlerMethodArgumentResolver {

    private static final String DEFAULT_PARAMETER = "sort";
    private static final String DEFAULT_QUALIFIER_DELIMITER = "_";
    private static final String DEFAULT_PROPERTY_DELIMITER = ",";
    private static final Sort DEFAULT_SORT = unsorted();

    private static final String SORT_DEFAULTS_NAME = SortDefaults.class.getSimpleName();
    private static final String SORT_DEFAULT_NAME = SortDefault.class.getSimpleName();



    private String propertyDelimiter = DEFAULT_PROPERTY_DELIMITER;
    private Sort fallbackSort = DEFAULT_SORT;
    private String sortParameter = DEFAULT_PARAMETER;
    private String qualifierDelimiter = DEFAULT_QUALIFIER_DELIMITER;

    protected Sort resolveSortArgument(String[] directionParameter, MethodParameter parameter) {
        return resolveSortArgument(directionParameter == null ? null : asList(directionParameter), parameter);
    }

    protected Sort resolveSortArgument(List<String> directionParameter, MethodParameter parameter) {
        // No parameters
        boolean notHasParameters = directionParameter == null || directionParameter.isEmpty();
        // Single empty parameter, e.g "sort="
        boolean hasEmptyParameter = notHasParameters || (directionParameter.size() == 1 && !StringUtils.hasText(directionParameter.get(0)));
        if (hasEmptyParameter) {
            return getDefaultFromAnnotationOrFallback(parameter);
        }

        return parseParameterIntoSort(directionParameter, propertyDelimiter);
    }

    private Sort getDefaultFromAnnotationOrFallback(MethodParameter parameter) {
        SortDefaults annotatedDefaults = parameter.getParameterAnnotation(SortDefaults.class);
        SortDefault annotatedDefault = parameter.getParameterAnnotation(SortDefault.class);

        if (annotatedDefault != null && annotatedDefaults != null) {
            throw new IllegalArgumentException(
                    String.format("Cannot use both @%s and @%s on parameter %s! Move %s into %s to define sorting order!",
                            SORT_DEFAULTS_NAME, SORT_DEFAULT_NAME, parameter.toString(), SORT_DEFAULT_NAME, SORT_DEFAULTS_NAME));
        }

        if (annotatedDefault != null) {
            return appendOrCreateSortTo(annotatedDefault, unsorted());
        }

        if (annotatedDefaults != null) {

            Sort sort = unsorted();

            for (SortDefault currentAnnotatedDefault : annotatedDefaults.value()) {
                sort = appendOrCreateSortTo(currentAnnotatedDefault, sort);
            }

            return sort;
        }

        return fallbackSort;
    }

    /**
     * Creates a new {@link Sort} instance from the given {@link SortDefault} or appends it to the given {@link Sort}
     * instance if it's not {@literal null}.
     *
     * @param sortDefault
     * @param sortOrNull
     * @return
     */
    private Sort appendOrCreateSortTo(SortDefault sortDefault, Sort sortOrNull) {

        String[] fields = SpringDataAnnotationUtils.getSpecificPropertyOrDefaultFromValue(sortDefault, "sort");

        if (fields.length == 0) {
            return unsorted();
        }

        return sortOrNull.and(by(sortDefault.direction(), fields));
    }

    /**
     * Returns the sort parameter to be looked up from the request. Potentially applies qualifiers to it.
     *
     * @param parameter can be {@literal null}.
     * @return
     */
    protected String getSortParameter(@Nullable MethodParameter parameter) {

        StringBuilder builder = new StringBuilder();

        Qualifier qualifier = parameter != null ? parameter.getParameterAnnotation(Qualifier.class) : null;

        if (qualifier != null) {
            builder.append(qualifier.value()).append(qualifierDelimiter);
        }

        return builder.append(sortParameter).toString();
    }

    /**
     * Parses the given sort expressions into a {@link Sort} instance. The implementation expects the sources to be a
     * concatenation of Strings using the given delimiter. If the last element can be parsed into a {@link Direction} it's
     * considered a {@link Direction} and a simple property otherwise.
     *
     * @param source will never be {@literal null}.
     * @param delimiter the delimiter to be used to split up the source elements, will never be {@literal null}.
     * @return
     */
    public Sort parseParameterIntoSort(String[] source, String delimiter) {
        return parseParameterIntoSort(asList(source), delimiter);
    }

    /**
     * Parses the given sort expressions into a {@link Sort} instance. The implementation expects the sources to be a
     * concatenation of Strings using the given delimiter. If the last element can be parsed into a {@link Direction} it's
     * considered a {@link Direction} and a simple property otherwise.
     *
     * @param source will never be {@literal null}.
     * @param delimiter the delimiter to be used to split up the source elements, will never be {@literal null}.
     * @return
     */
    private Sort parseParameterIntoSort(List<String> source, String delimiter) {

        List<Order> allOrders = new ArrayList<>();

        for (String part : source) {

            if (part == null) {
                continue;
            }

            String[] elements = part.split(delimiter);

            Optional<Direction> direction = elements.length == 0 ? Optional.empty()
                    : Direction.fromOptionalString(elements[elements.length - 1]);

            int lastIndex = direction.map(it -> elements.length - 1).orElseGet(() -> elements.length);

            Arrays.stream(elements, 0, lastIndex)
                    .filter(s -> !s.isEmpty())
                    .map(property -> direction.map(it -> new Order(it, property)).orElse(Order.by(property)))
                    .forEach(allOrders::add);
        }

        return allOrders.isEmpty() ? unsorted() : by(allOrders);
    }


    /**
     * Configures the {@link Sort} to be used as fallback in case no {@link SortDefault} or {@link SortDefaults} (the
     * latter only supported in legacy mode) can be found at the method parameter to be resolved.
     * <p>
     * If you set this to {@literal null}, be aware that you controller methods will get {@literal null} handed into them
     * in case no {@link Sort} data can be found in the request.
     *
     * @param fallbackSort the {@link Sort} to be used as general fallback.
     */
    public void setFallbackSort(Sort fallbackSort) {
        this.fallbackSort = fallbackSort;
    }

    /**
     * Configure the request parameter to lookup sort information from. Defaults to {@code sort}.
     *
     * @param sortParameter must not be {@literal null} or empty.
     */
    public void setSortParameter(String sortParameter) {

        Assert.hasText(sortParameter, "SortParameter must not be null nor empty!");
        this.sortParameter = sortParameter;
    }

    /**
     * Configures the delimiter used to separate the qualifier from the sort parameter. Defaults to {@code _}, so a
     * qualified sort property would look like {@code qualifier_sort}.
     *
     * @param qualifierDelimiter the qualifier delimiter to be used or {@literal null} to reset to the default.
     */
    public void setQualifierDelimiter(String qualifierDelimiter) {
        this.qualifierDelimiter = qualifierDelimiter == null ? DEFAULT_QUALIFIER_DELIMITER : qualifierDelimiter;
    }

    /**
     * Configures the delimiter used to separate property references and the direction to be sorted by. Defaults to
     * {@code}, which means sort values look like this: {@code firstname,lastname,asc}.
     *
     * @param propertyDelimiter must not be {@literal null} or empty.
     */
    public void setPropertyDelimiter(String propertyDelimiter) {

        Assert.hasText(propertyDelimiter, "Property delimiter must not be null or empty!");
        this.propertyDelimiter = propertyDelimiter;
    }

    /**
     * Folds the given {@link Sort} instance into a {@link List} of sort expressions, accumulating {@link Order} instances
     * of the same direction into a single expression if they are in order.
     *
     * @param sort must not be {@literal null}.
     * @return
     */
    protected List<String> foldIntoExpressions(Sort sort) {

        List<String> expressions = new ArrayList<>();
        ExpressionBuilder builder = null;

        for (Order order : sort) {

            Direction direction = order.getDirection();

            if (builder == null) {
                builder = new ExpressionBuilder(direction);
            } else if (!builder.hasSameDirectionAs(order)) {
                builder.dumpExpressionIfPresentInto(expressions);
                builder = new ExpressionBuilder(direction);
            }

            builder.add(order.getProperty());
        }

        return builder == null ? Collections.emptyList() : builder.dumpExpressionIfPresentInto(expressions);
    }

    /**
     * Folds the given {@link Sort} instance into two expressions. The first being the property list, the second being the
     * direction.
     *
     * @throws IllegalArgumentException if a {@link Sort} with multiple {@link Direction}s has been handed in.
     * @param sort must not be {@literal null}.
     * @return
     */
    protected List<String> legacyFoldExpressions(Sort sort) {

        List<String> expressions = new ArrayList<>();
        ExpressionBuilder builder = null;

        for (Order order : sort) {

            Direction direction = order.getDirection();

            if (builder == null) {
                builder = new ExpressionBuilder(direction);
            } else if (!builder.hasSameDirectionAs(order)) {
                throw new IllegalArgumentException(String.format(
                        "%s in legacy configuration only supports a single direction to sort by!", getClass().getSimpleName()));
            }

            builder.add(order.getProperty());
        }

        return builder == null ? Collections.emptyList() : builder.dumpExpressionIfPresentInto(expressions);
    }

    /**
     * Helper to easily build request parameter expressions for {@link Sort} instances.
     *
     * @author Oliver Gierke
     */
    class ExpressionBuilder {

        private final List<String> elements = new ArrayList<>();
        private final Direction direction;

        /**
         * Sets up a new {@link ExpressionBuilder} for properties to be sorted in the given {@link Direction}.
         *
         * @param direction must not be {@literal null}.
         */
        public ExpressionBuilder(Direction direction) {

            Assert.notNull(direction, "Direction must not be null!");
            this.direction = direction;
        }

        /**
         * Returns whether the given {@link Order} has the same direction as the current {@link ExpressionBuilder}.
         *
         * @param order must not be {@literal null}.
         * @return
         */
        public boolean hasSameDirectionAs(Order order) {
            return this.direction == order.getDirection();
        }

        /**
         * Adds the given property to the expression to be built.
         *
         * @param property
         */
        public void add(String property) {
            this.elements.add(property);
        }

        /**
         * Dumps the expression currently in build into the given {@link List} of {@link String}s. Will only dump it in case
         * there are properties piled up currently.
         *
         * @param expressions
         * @return
         */
        public List<String> dumpExpressionIfPresentInto(List<String> expressions) {

            if (elements.isEmpty()) {
                return expressions;
            }

            elements.add(direction.name().toLowerCase());
            expressions.add(StringUtils.collectionToDelimitedString(elements, propertyDelimiter));

            return expressions;
        }
    }

}
