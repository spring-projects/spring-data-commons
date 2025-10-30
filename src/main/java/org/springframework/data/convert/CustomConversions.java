/*
 * Copyright 2011-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.convert;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.ConverterBuilder.ConverterAware;
import org.springframework.data.core.CustomCollections;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.Predicates;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Value object to capture custom conversion. That is essentially a {@link List} of converters and some additional logic
 * around them. The converters build up two sets of types which store-specific basic types can be converted into and
 * from. These types will be considered simple ones (which means they neither need deeper inspection nor nested
 * conversion. Thus, the {@link CustomConversions} also act as factory for {@link SimpleTypeHolder}.
 * <p>
 * Custom conversions supports the following converters:
 * <ul>
 * <li>Converters built by {@link ConverterBuilder}</li>
 * <li>{@link GenericConverter}</li>
 * <li>{@link ConverterFactory}</li>
 * <li>{@link Converter}</li>
 * </ul>
 * Note that CustomConversions does not support {@link org.springframework.core.convert.converter.ConditionalConverter}
 * as custom conversions maintains a registry of convertible type pairs; We cannot determine convertible types based on
 * {@code ConditionalConverter#matches}, instead, we require a converter to provide its convertible types either through
 * the class declaration or {@code GenericConverter#getConvertibleTypes}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Xeno Amess
 * @author Johannes Englmeier
 * @since 2.0
 */
public class CustomConversions {

	private static Log logger = LogFactory.getLog(CustomConversions.class);
	private static final String READ_CONVERTER_NOT_SIMPLE = "Registering converter from %s to %s as reading converter although it doesn't convert from a store-supported type; You might want to check your annotation setup at the converter implementation";
	private static final String WRITE_CONVERTER_NOT_SIMPLE = "Registering converter from %s to %s as writing converter although it doesn't convert to a store-supported type; You might want to check your annotation setup at the converter implementation";
	private static final String NOT_A_CONVERTER = "Converter %s is neither a Spring Converter, GenericConverter or ConverterFactory";
	private static final String CONVERTER_FILTER = "converter from %s to %s as %s converter";
	private static final String ADD_CONVERTER = "Adding %s" + CONVERTER_FILTER;
	private static final String SKIP_CONVERTER = "Skipping " + CONVERTER_FILTER
			+ " %s is not a store supported simple type";
	private static final List<Object> DEFAULT_CONVERTERS;

	static {

		List<Object> defaults = new ArrayList<>();

		defaults.addAll(Jsr310Converters.getConvertersToRegister());
		defaults.addAll(JMoleculesConverters.getConvertersToRegister());

		DEFAULT_CONVERTERS = Collections.unmodifiableList(defaults);
	}

	private final SimpleTypeHolder simpleTypeHolder;
	private final List<Object> converters;

	private final Set<ConvertiblePair> readingPairs = new LinkedHashSet<>();
	private final Set<ConvertiblePair> writingPairs = new LinkedHashSet<>();
	private final Set<Class<?>> customSimpleTypes = new HashSet<>();
	private final ConversionTargetsCache customReadTargetTypes = new ConversionTargetsCache();
	private final ConversionTargetsCache customWriteTargetTypes = new ConversionTargetsCache();

	private final ConverterConfiguration converterConfiguration;

	private final Function<ConvertiblePair, Class<?>> getReadTarget = convertiblePair -> getCustomTarget(
			convertiblePair.getSourceType(), convertiblePair.getTargetType(), readingPairs);

	private final Function<ConvertiblePair, Class<?>> getWriteTarget = convertiblePair -> getCustomTarget(
			convertiblePair.getSourceType(), convertiblePair.getTargetType(), writingPairs);

	private final Function<ConvertiblePair, Class<?>> getRawWriteTarget = convertiblePair -> getCustomTarget(
			convertiblePair.getSourceType(), null, writingPairs);

	private final @Nullable PropertyValueConversions propertyValueConversions;

	/**
	 * @param converterConfiguration the {@link ConverterConfiguration} to apply.
	 * @since 2.3
	 */
	public CustomConversions(ConverterConfiguration converterConfiguration) {

		this.converterConfiguration = converterConfiguration;

		List<Object> registeredConverters = collectPotentialConverterRegistrations(
				converterConfiguration.getStoreConversions(), converterConfiguration.getUserConverters()).stream()
				.filter(this::isSupportedConverter).filter(this::shouldRegister)
				.map(ConverterRegistrationIntent::getConverterRegistration).map(this::register).distinct()
				.collect(Collectors.toCollection(ArrayList::new));

		Collections.reverse(registeredConverters);

		this.converters = Collections.unmodifiableList(registeredConverters);
		this.simpleTypeHolder = new SimpleTypeHolder(customSimpleTypes,
				converterConfiguration.getStoreConversions().getStoreTypeHolder());
		this.propertyValueConversions = converterConfiguration.getPropertyValueConversions();
	}

	/**
	 * Creates a new {@link CustomConversions} instance registering all given user defined converters and selecting
	 * {@link Converter converters} from {@link StoreConversions} depending on
	 * {@link CustomConversions#getSimpleTypeHolder() store simple types} only considering those that either convert
	 * to/from a store supported type.
	 *
	 * @param storeConversions must not be {@literal null}.
	 * @param converters must not be {@literal null}.
	 */
	public CustomConversions(StoreConversions storeConversions, Collection<?> converters) {
		this(new ConverterConfiguration(storeConversions, new ArrayList<>(converters)));
	}

	private static boolean hasAssignableSourceType(ConvertiblePair pair, Class<?> sourceType) {
		return pair.getSourceType().isAssignableFrom(sourceType);
	}

	private static boolean requestedTargetTypeIsAssignable(@Nullable Class<?> requestedTargetType, Class<?> targetType) {

		return requestedTargetType == null || targetType.isAssignableFrom(requestedTargetType);
	}

	/**
	 * Returns the underlying {@link SimpleTypeHolder}.
	 *
	 * @return the underlying {@link SimpleTypeHolder}.
	 * @see SimpleTypeHolder
	 */
	public SimpleTypeHolder getSimpleTypeHolder() {
		return simpleTypeHolder;
	}

	/**
	 * Determines whether the given, required {@link PersistentProperty property} has a value-specific converter
	 * registered. Returns {@literal false} if no {@link PropertyValueConversions} have been configured for the underlying
	 * store.
	 * <p>
	 * This method protects against {@literal null} when not {@link PropertyValueConversions} have been configured for the
	 * underlying data store, and is a shortcut for: <code>
	 *     customConversions.getPropertyValueConversions().hasValueConverter(property);
	 * </code>
	 *
	 * @param property {@link PersistentProperty} to evaluate; must not be {@literal null}.
	 * @return a boolean value indicating whether {@link PropertyValueConverter} has been configured and registered for
	 *         the {@link PersistentProperty property}.
	 * @see PropertyValueConversions#hasValueConverter(PersistentProperty)
	 * @see #getPropertyValueConversions()
	 * @see PersistentProperty
	 */
	public boolean hasValueConverter(PersistentProperty<?> property) {

		PropertyValueConversions pvc = getPropertyValueConversions();

		return pvc != null && pvc.hasValueConverter(property);
	}

	/**
	 * Returns the required {@link PropertyValueConverter} for the given {@link PersistentProperty} or throws
	 * {@link IllegalStateException} if no converter is available. This is a convenience method for
	 * {@code getPropertyValueConversions().getValueConverter(…)} enforcing non-null constraints.
	 * <p>
	 * Prior to calling this method you should verify a converter is available using
	 * {@link #hasValueConverter(PersistentProperty)}.
	 *
	 * @param property {@link PersistentProperty} to evaluate; must not be {@literal null}.
	 * @return the required {@link PropertyValueConverter}
	 * @throws IllegalStateException if no converter is available.
	 * @since 4.0
	 * @see #hasValueConverter(PersistentProperty)
	 */
	public <DV, SV, P extends PersistentProperty<P>, VCC extends ValueConversionContext<P>> PropertyValueConverter<DV, SV, VCC> getRequiredValueConverter(
			P property) {

		PropertyValueConversions pvc = getPropertyValueConversions();
		PropertyValueConverter<DV, SV, VCC> converter = pvc != null ? pvc.getValueConverter(property) : null;

		if (converter == null) {
			throw new IllegalStateException("No value converter registered for property %s".formatted(property.getName()));
		}

		return converter;
	}

	/**
	 * Returns whether the given type is considered to be simple. That means it's either a general simple type or we have
	 * a writing {@link Converter} registered for a particular type.
	 *
	 * @see SimpleTypeHolder#isSimpleType(Class)
	 * @param type {@link Class} to evaluate as a simple type, such as a primitive type.
	 * @return a boolean value indicating whether the given, required {@link Class type} is simple.
	 */
	public boolean isSimpleType(Class<?> type) {

		Assert.notNull(type, "Type must not be null");

		return simpleTypeHolder.isSimpleType(type);
	}

	/**
	 * Populates the given {@link GenericConversionService} with the converters registered.
	 *
	 * @param conversionService {@link ConverterRegistry} to populate; must not be {@literal null}.
	 * @see ConverterRegistry
	 */
	public void registerConvertersIn(ConverterRegistry conversionService) {

		Assert.notNull(conversionService, "ConversionService must not be null");

		converters.forEach(it -> registerConverterIn(it, conversionService));
		CustomCollections.registerConvertersIn(conversionService);
	}

	/**
	 * Registers the given converter in the given {@link GenericConversionService}.
	 *
	 * @param candidate must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	@SuppressWarnings("rawtypes")
	private void registerConverterIn(Object candidate, ConverterRegistry conversionService) {

		if (candidate instanceof Converter converter) {
			conversionService.addConverter(converter);
		} else if (candidate instanceof ConverterFactory converterFactory) {
			conversionService.addConverterFactory(converterFactory);
		} else if (candidate instanceof GenericConverter genericConverter) {
			conversionService.addConverter(genericConverter);
		} else if (candidate instanceof ConverterAware converterAware) {
			converterAware.getConverters().forEach(it -> registerConverterIn(it, conversionService));
		} else {
			throw new IllegalArgumentException(String.format(NOT_A_CONVERTER, candidate));
		}
	}

	/**
	 * Gets a reference to the configured {@link PropertyValueConversions} if property value conversions are supported by
	 * the underlying data store.
	 *
	 * @return a reference to the configured {@link PropertyValueConversions}; may be {@literal null} if the underlying
	 *         data store does not support property value conversions.
	 * @see PropertyValueConversions
	 */
	public @Nullable PropertyValueConversions getPropertyValueConversions() {
		return propertyValueConversions;
	}

	/**
	 * Get all converters and add origin information
	 *
	 * @param storeConversions collection of store-base conversions; must not be {@literal null}.
	 * @param converters collections of custom, user-based converters; must not be {@literal null}.
	 * @return a {@link List} of intended {@link ConverterRegistration ConverterRegistrations}.
	 * @see ConverterRegistration
	 * @since 2.3
	 */
	private List<ConverterRegistrationIntent> collectPotentialConverterRegistrations(StoreConversions storeConversions,
			Collection<?> converters) {

		List<ConverterRegistrationIntent> converterRegistrations = new ArrayList<>();

		converters.stream().map(storeConversions::getRegistrationsFor).flatMap(Streamable::stream)
				.map(ConverterRegistrationIntent::userConverters).forEach(converterRegistrations::add);

		storeConversions.getStoreConverters().stream().map(storeConversions::getRegistrationsFor)
				.flatMap(Streamable::stream).map(ConverterRegistrationIntent::storeConverters)
				.forEach(converterRegistrations::add);

		DEFAULT_CONVERTERS.stream().map(storeConversions::getRegistrationsFor).flatMap(Streamable::stream)
				.map(ConverterRegistrationIntent::defaultConverters).forEach(converterRegistrations::add);

		return converterRegistrations;
	}

	/**
	 * Registers the given {@link ConvertiblePair} as reading or writing pair depending on the type sides being basic
	 * types.
	 *
	 * @param converterRegistration {@link ConverterRegistration} to register; must not be {@literal null}.
	 * @see ConverterRegistration
	 */
	private Object register(ConverterRegistration converterRegistration) {

		Assert.notNull(converterRegistration, "Converter registration must not be null");

		ConvertiblePair pair = converterRegistration.getConvertiblePair();

		if (converterRegistration.isReading()) {

			readingPairs.add(pair);

			if (logger.isWarnEnabled() && !converterRegistration.isSimpleSourceType()
					&& !Collection.class.isAssignableFrom(pair.getSourceType())) {
				logger.warn(String.format(READ_CONVERTER_NOT_SIMPLE, pair.getSourceType(), pair.getTargetType()));
			}
		}

		if (converterRegistration.isWriting()) {

			writingPairs.add(pair);
			customSimpleTypes.add(pair.getSourceType());

			if (logger.isWarnEnabled() && !converterRegistration.isSimpleTargetType()
					&& !Collection.class.isAssignableFrom(pair.getTargetType())) {
				logger.warn(String.format(WRITE_CONVERTER_NOT_SIMPLE, pair.getSourceType(), pair.getTargetType()));
			}
		}

		return converterRegistration.getConverter();
	}

	/**
	 * Validate a given {@link ConverterRegistration} in a specific setup.<br/>
	 * Non {@link ReadingConverter reading} and user defined {@link Converter converters} are only considered supported if
	 * the {@link ConverterRegistrationIntent#isSimpleTargetType() target type} is considered to be a store simple type.
	 *
	 * @param registrationIntent {@link ConverterRegistrationIntent} to validate; must not be {@literal null}.
	 * @return {@literal true} if supported.
	 * @since 2.3
	 */
	private boolean isSupportedConverter(ConverterRegistrationIntent registrationIntent) {

		boolean register = registrationIntent.isUserConverter() || registrationIntent.isStoreConverter()
				|| (registrationIntent.isReading() && registrationIntent.isSimpleSourceType())
				|| (registrationIntent.isWriting() && registrationIntent.isSimpleTargetType());

		if (logger.isDebugEnabled()) {

			if (register) {
				logger.debug(String.format(ADD_CONVERTER, registrationIntent.isUserConverter() ? "user defined " : "",
						registrationIntent.getSourceType(), registrationIntent.getTargetType(),
						registrationIntent.isReading() ? "reading" : "writing"));
			} else {
				logger.debug(String.format(SKIP_CONVERTER, registrationIntent.getSourceType(),
						registrationIntent.getTargetType(), registrationIntent.isReading() ? "reading" : "writing",
						registrationIntent.isReading() ? registrationIntent.getSourceType() : registrationIntent.getTargetType()));
			}
		}

		return register;
	}

	/**
	 * @param intent must not be {@literal null}.
	 * @return {@literal false} if the given {@link ConverterRegistration} shall be skipped.
	 * @since 2.3
	 */
	private boolean shouldRegister(ConverterRegistrationIntent intent) {
		return !intent.isDefaultConverter()
				|| converterConfiguration.shouldRegister(intent.getConverterRegistration().getConvertiblePair());
	}

	/**
	 * Returns the target type to convert to in case we have a custom conversion registered to convert the given source
	 * type into a store native one.
	 *
	 * @param sourceType must not be {@literal null}
	 * @return the target type to convert to in case we have a custom conversion registered to convert the given source
	 *         type into a store native one.
	 */
	public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType) {

		Assert.notNull(sourceType, "Source type must not be null");

		Class<?> target = customWriteTargetTypes.computeIfAbsent(sourceType, getRawWriteTarget);

		return Void.class.equals(target) || target == null ? Optional.empty() : Optional.of(target);
	}

	/**
	 * Returns the target type we can read an inject of the given source type to. The returned type might be a subclass of
	 * the given expected type though. If {@code requestedTargetType} is {@literal null} we will simply return the first
	 * target type matching or {@literal null} if no conversion can be found.
	 *
	 * @param sourceType must not be {@literal null}
	 * @param requestedTargetType must not be {@literal null}.
	 * @return the target type we can read an inject of the given source type to.
	 */
	public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType, Class<?> requestedTargetType) {

		Assert.notNull(sourceType, "Source type must not be null");
		Assert.notNull(requestedTargetType, "Target type must not be null");

		Class<?> target = customWriteTargetTypes.computeIfAbsent(sourceType, requestedTargetType, getWriteTarget);

		return Void.class.equals(target) || target == null ? Optional.empty() : Optional.of(target);
	}

	/**
	 * Returns whether we have a custom conversion registered to read {@code sourceType} into a native type. The returned
	 * type might be a subclass of the given expected type though.
	 *
	 * @param sourceType must not be {@literal null}
	 * @return whether we have a custom conversion registered to read {@code sourceType} into a native type.
	 */
	public boolean hasCustomWriteTarget(Class<?> sourceType) {

		Assert.notNull(sourceType, "Source type must not be null");

		return getCustomWriteTarget(sourceType).isPresent();
	}

	/**
	 * Returns whether we have a custom conversion registered to read an object of the given source type into an object of
	 * the given native target type.
	 *
	 * @param sourceType must not be {@literal null}.
	 * @param targetType must not be {@literal null}.
	 * @return whether we have a custom conversion registered to read an object of the given source type into an object of
	 *         the given native target type.
	 */
	public boolean hasCustomWriteTarget(Class<?> sourceType, Class<?> targetType) {

		Assert.notNull(sourceType, "Source type must not be null");
		Assert.notNull(targetType, "Target type must not be null");

		return getCustomWriteTarget(sourceType, targetType).isPresent();
	}

	/**
	 * Returns whether we have a custom conversion registered to read the given source into the given target type.
	 *
	 * @param sourceType must not be {@literal null}
	 * @param targetType must not be {@literal null}
	 * @return whether we have a custom conversion registered to read the given source into the given target type.
	 */
	public boolean hasCustomReadTarget(Class<?> sourceType, Class<?> targetType) {

		Assert.notNull(sourceType, "Source type must not be null");
		Assert.notNull(targetType, "Target type must not be null");

		return getCustomReadTarget(sourceType, targetType) != null;
	}

	/**
	 * Returns the actual target type for the given {@code sourceType} and {@code targetType}. Note that the returned
	 * {@link Class} could be an assignable type to the given {@code targetType}.
	 *
	 * @param sourceType must not be {@literal null}.
	 * @param targetType must not be {@literal null}.
	 * @return the actual target type for the given {@code sourceType} and {@code targetType}.
	 */
	private @Nullable Class<?> getCustomReadTarget(Class<?> sourceType, Class<?> targetType) {
		return customReadTargetTypes.computeIfAbsent(sourceType, targetType, getReadTarget);
	}

	/**
	 * Inspects the given {@link ConvertiblePair ConvertiblePairs} for ones that have a source compatible type as source.
	 * Additionally, checks assignability of the target type if one is given.
	 *
	 * @param sourceType must not be {@literal null}.
	 * @param targetType can be {@literal null}.
	 * @param pairs must not be {@literal null}.
	 * @return the base {@link Class type} for the (requested) {@link Class target type} if present.
	 */
	private @Nullable Class<?> getCustomTarget(Class<?> sourceType, @Nullable Class<?> targetType,
			Collection<ConvertiblePair> pairs) {

		if (targetType != null && pairs.contains(new ConvertiblePair(sourceType, targetType))) {
			return targetType;
		}

		for (ConvertiblePair pair : pairs) {

			if (!hasAssignableSourceType(pair, sourceType)) {
				continue;
			}

			Class<?> candidate = pair.getTargetType();

			if (!requestedTargetTypeIsAssignable(targetType, candidate)) {
				continue;
			}

			return candidate;
		}

		return null;
	}

	/**
	 * Value object to cache custom conversion targets.
	 *
	 * @author Mark Paluch
	 */
	static class ConversionTargetsCache {

		private volatile Map<Class<?>, TargetTypes> customReadTargetTypes = new HashMap<>();

		/**
		 * Get or compute a target type given its {@code sourceType}. Returns a cached {@link Optional} if the value
		 * (present/absent target) was computed once. Otherwise, uses a {@link Function mappingFunction} to determine a
		 * possibly existing target type.
		 *
		 * @param sourceType must not be {@literal null}.
		 * @param mappingFunction must not be {@literal null}.
		 * @return the optional target type.
		 */
		public @Nullable Class<?> computeIfAbsent(Class<?> sourceType,
				Function<ConvertiblePair, Class<?>> mappingFunction) {
			return computeIfAbsent(sourceType, AbsentTargetTypeMarker.class, mappingFunction);
		}

		/**
		 * Get or compute a target type given its {@code sourceType} and {@code targetType}. Returns a cached
		 * {@link Optional} if the value (present/absent target) was computed once. Otherwise, uses a {@link Function
		 * mappingFunction} to determine a possibly existing target type.
		 *
		 * @param sourceType must not be {@literal null}.
		 * @param targetType must not be {@literal null}.
		 * @param mappingFunction must not be {@literal null}.
		 * @return the optional target type.
		 */
		public @Nullable Class<?> computeIfAbsent(Class<?> sourceType, Class<?> targetType,
				Function<ConvertiblePair, Class<?>> mappingFunction) {

			TargetTypes targetTypes = customReadTargetTypes.get(sourceType);

			if (targetTypes == null) {

				synchronized (this) {

					TargetTypes customReadTarget = customReadTargetTypes.get(sourceType);
					if (customReadTarget != null) {
						targetTypes = customReadTarget;
					} else {

						Map<Class<?>, TargetTypes> customReadTargetTypes = new HashMap<>(this.customReadTargetTypes);
						targetTypes = new TargetTypes(sourceType);
						customReadTargetTypes.put(sourceType, targetTypes);
						this.customReadTargetTypes = customReadTargetTypes;
					}
				}
			}

			return targetTypes.computeIfAbsent(targetType, mappingFunction);
		}

		/**
		 * Marker type for absent target type caching.
		 */
		interface AbsentTargetTypeMarker {}
	}

	/**
	 * Value object for a specific {@code Class source type} to determine possible target conversion types.
	 *
	 * @author Mark Paluch
	 */
	static class TargetTypes {

		private final Class<?> sourceType;
		private volatile Map<Class<?>, Class<?>> conversionTargets = new HashMap<>();

		TargetTypes(Class<?> sourceType) {
			this.sourceType = sourceType;
		}

		/**
		 * Get or compute a target type given its {@code targetType}. Returns a cached {@link Optional} if the value
		 * (present/absent target) was computed once. Otherwise, uses a {@link Function mappingFunction} to determine a
		 * possibly existing target type.
		 *
		 * @param targetType must not be {@literal null}.
		 * @param mappingFunction must not be {@literal null}.
		 * @return the optional target type.
		 */
		public @Nullable Class<?> computeIfAbsent(Class<?> targetType,
				Function<ConvertiblePair, Class<?>> mappingFunction) {

			Class<?> optionalTarget = conversionTargets.get(targetType);

			if (optionalTarget == null) {

				synchronized (this) {

					Class<?> conversionTarget = conversionTargets.get(targetType);
					if (conversionTarget != null) {
						optionalTarget = conversionTarget;
					} else {

						optionalTarget = mappingFunction.apply(new ConvertiblePair(sourceType, targetType));
						Map<Class<?>, Class<?>> conversionTargets = new HashMap<>(this.conversionTargets);
						conversionTargets.put(targetType, optionalTarget == null ? Void.class : optionalTarget);
						this.conversionTargets = conversionTargets;
					}
				}
			}

			return Void.class.equals(optionalTarget) ? null : optionalTarget;
		}
	}

	/**
	 * Value class tying together a {@link ConverterRegistration} and its {@link ConverterOrigin origin} to allow
	 * fine-grained registration based on store supported types.
	 *
	 * @since 2.3
	 * @author Christoph Strobl
	 */
	protected static class ConverterRegistrationIntent {

		private final ConverterRegistration delegate;
		private final ConverterOrigin origin;

		ConverterRegistrationIntent(ConverterRegistration delegate, ConverterOrigin origin) {
			this.delegate = delegate;
			this.origin = origin;
		}

		static ConverterRegistrationIntent userConverters(ConverterRegistration delegate) {
			return new ConverterRegistrationIntent(delegate, ConverterOrigin.USER_DEFINED);
		}

		static ConverterRegistrationIntent storeConverters(ConverterRegistration delegate) {
			return new ConverterRegistrationIntent(delegate, ConverterOrigin.STORE);
		}

		static ConverterRegistrationIntent defaultConverters(ConverterRegistration delegate) {
			return new ConverterRegistrationIntent(delegate, ConverterOrigin.DEFAULT);
		}

		Class<?> getSourceType() {
			return delegate.getConvertiblePair().getSourceType();
		}

		Class<?> getTargetType() {
			return delegate.getConvertiblePair().getTargetType();
		}

		public boolean isWriting() {
			return delegate.isWriting();
		}

		public boolean isReading() {
			return delegate.isReading();
		}

		public boolean isSimpleSourceType() {
			return delegate.isSimpleSourceType();
		}

		public boolean isSimpleTargetType() {
			return delegate.isSimpleTargetType();
		}

		public boolean isUserConverter() {
			return isConverterOfSource(ConverterOrigin.USER_DEFINED);
		}

		public boolean isStoreConverter() {
			return isConverterOfSource(ConverterOrigin.STORE);
		}

		public boolean isDefaultConverter() {
			return isConverterOfSource(ConverterOrigin.DEFAULT);
		}

		ConverterRegistration getConverterRegistration() {
			return delegate;
		}

		private boolean isConverterOfSource(ConverterOrigin source) {
			return origin.equals(source);
		}

		protected enum ConverterOrigin {
			DEFAULT, USER_DEFINED, STORE
		}
	}

	/**
	 * Conversion registration information.
	 *
	 * @author Oliver Gierke
	 * @author Mark Paluch
	 */
	private static class ConverterRegistration {

		private final Object converter;
		private final ConvertiblePair convertiblePair;
		private final StoreConversions storeConversions;
		private final boolean reading;
		private final boolean writing;

		private ConverterRegistration(Object converter, ConvertiblePair convertiblePair, StoreConversions storeConversions,
				boolean reading, boolean writing) {
			this.converter = converter;
			this.convertiblePair = convertiblePair;
			this.storeConversions = storeConversions;
			this.reading = reading;
			this.writing = writing;
		}

		/**
		 * Returns whether the converter shall be used for writing.
		 *
		 * @return
		 */
		public boolean isWriting() {
			return writing || (!reading && isSimpleTargetType());
		}

		/**
		 * Returns whether the converter shall be used for reading.
		 *
		 * @return
		 */
		public boolean isReading() {
			return reading || (!writing && isSimpleSourceType());
		}

		/**
		 * Returns the actual conversion pair.
		 *
		 * @return
		 */
		public ConvertiblePair getConvertiblePair() {
			return convertiblePair;
		}

		/**
		 * Returns whether the source type is a simple one.
		 *
		 * @return
		 */
		public boolean isSimpleSourceType() {
			return storeConversions.isStoreSimpleType(convertiblePair.getSourceType());
		}

		/**
		 * Returns whether the target type is a simple one.
		 *
		 * @return
		 */
		public boolean isSimpleTargetType() {
			return storeConversions.isStoreSimpleType(convertiblePair.getTargetType());
		}

		/**
		 * @return
		 */
		Object getConverter() {
			return converter;
		}
	}

	/**
	 * Value type to capture store-specific extensions to the {@link CustomConversions}. Allows to forward store specific
	 * default conversions and a set of types that are supposed to be considered simple.
	 *
	 * @author Oliver Gierke
	 */
	public static class StoreConversions {

		public static final StoreConversions NONE = StoreConversions.of(SimpleTypeHolder.DEFAULT, Collections.emptyList());

		private final SimpleTypeHolder storeTypeHolder;
		private final Collection<?> storeConverters;

		private StoreConversions(SimpleTypeHolder storeTypeHolder, Collection<?> storeConverters) {

			this.storeTypeHolder = storeTypeHolder;
			this.storeConverters = storeConverters;
		}

		/**
		 * Creates a new {@link StoreConversions} for the given store-specific {@link SimpleTypeHolder} and the given
		 * converters.
		 *
		 * @param storeTypeHolder must not be {@literal null}.
		 * @param converters must not be {@literal null}.
		 * @return
		 */
		public static StoreConversions of(SimpleTypeHolder storeTypeHolder, Object... converters) {

			Assert.notNull(storeTypeHolder, "SimpleTypeHolder must not be null");
			Assert.notNull(converters, "Converters must not be null");

			return new StoreConversions(storeTypeHolder, Arrays.asList(converters));
		}

		/**
		 * Creates a new {@link StoreConversions} for the given store-specific {@link SimpleTypeHolder} and the given
		 * converters.
		 *
		 * @param storeTypeHolder must not be {@literal null}.
		 * @param converters must not be {@literal null}.
		 * @return
		 */
		public static StoreConversions of(SimpleTypeHolder storeTypeHolder, Collection<?> converters) {

			Assert.notNull(storeTypeHolder, "SimpleTypeHolder must not be null");
			Assert.notNull(converters, "Converters must not be null");

			return new StoreConversions(storeTypeHolder, converters);
		}

		/**
		 * Returns {@link ConverterRegistration}s for the given converter.
		 *
		 * @param converter must not be {@literal null}.
		 * @return
		 */
		Streamable<ConverterRegistration> getRegistrationsFor(Object converter) {

			Assert.notNull(converter, "Converter must not be null");

			Class<?> type = converter.getClass();
			boolean isWriting = isAnnotatedWith(type, WritingConverter.class);
			boolean isReading = isAnnotatedWith(type, ReadingConverter.class);

			if (converter instanceof ConverterAware converterAware) {

				return Streamable
						.of(() -> converterAware.getConverters().stream().flatMap(it -> getRegistrationsFor(it).stream()));

			} else if (converter instanceof GenericConverter genericConverter) {

				Set<ConvertiblePair> convertibleTypes = genericConverter.getConvertibleTypes();

				return convertibleTypes == null ? Streamable.empty()
						: Streamable.of(convertibleTypes).map(it -> register(converter, it, isReading, isWriting));

			} else if (converter instanceof ConverterFactory) {

				return getRegistrationFor(converter, ConverterFactory.class, isReading, isWriting);

			} else if (converter instanceof Converter) {

				return getRegistrationFor(converter, Converter.class, isReading, isWriting);

			} else {
				throw new IllegalArgumentException(String.format("Unsupported converter type %s", converter));
			}
		}

		private static boolean isAnnotatedWith(Class<?> type, Class<? extends Annotation> annotationType) {
			return AnnotationUtils.findAnnotation(type, annotationType) != null;
		}

		private Streamable<ConverterRegistration> getRegistrationFor(Object converter, Class<?> type, boolean isReading,
				boolean isWriting) {

			Class<?> converterType = converter.getClass();
			Class<?>[] arguments = GenericTypeResolver.resolveTypeArguments(converterType, type);

			if (arguments == null) {
				throw new IllegalStateException(String.format("Couldn't resolve type arguments for %s", converterType));
			}

			return Streamable.of(register(converter, arguments[0], arguments[1], isReading, isWriting));
		}

		private ConverterRegistration register(Object converter, Class<?> source, Class<?> target, boolean isReading,
				boolean isWriting) {
			return register(converter, new ConvertiblePair(source, target), isReading, isWriting);
		}

		private ConverterRegistration register(Object converter, ConvertiblePair pair, boolean isReading,
				boolean isWriting) {
			return new ConverterRegistration(converter, pair, this, isReading, isWriting);
		}

		private boolean isStoreSimpleType(Class<?> type) {
			return storeTypeHolder.isSimpleType(type);
		}

		SimpleTypeHolder getStoreTypeHolder() {
			return this.storeTypeHolder;
		}

		Collection<?> getStoreConverters() {
			return this.storeConverters;
		}

		@Override
		public boolean equals(@Nullable Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof StoreConversions that)) {
				return false;
			}

			if (!ObjectUtils.nullSafeEquals(storeTypeHolder, that.storeTypeHolder)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(storeConverters, that.storeConverters);
		}

		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(storeTypeHolder);
			result = 31 * result + ObjectUtils.nullSafeHashCode(storeConverters);
			return result;
		}

		@Override
		public String toString() {
			return "StoreConversions{" + "storeTypeHolder=" + storeTypeHolder + ", storeConverters=" + storeConverters + '}';
		}
	}

	/**
	 * Value object holding the actual {@link StoreConversions} and custom {@link Converter converters} configured for
	 * registration.
	 *
	 * @author Christoph Strobl
	 * @since 2.3
	 */
	protected static class ConverterConfiguration {

		private final StoreConversions storeConversions;
		private final List<?> userConverters;
		private final Predicate<ConvertiblePair> converterRegistrationFilter;
		private final @Nullable PropertyValueConversions propertyValueConversions;

		/**
		 * Create a new ConverterConfiguration holding the given {@link StoreConversions} and user defined converters.
		 *
		 * @param storeConversions must not be {@literal null}.
		 * @param userConverters must not be {@literal null} use {@link Collections#emptyList()} instead.
		 */
		public ConverterConfiguration(StoreConversions storeConversions, List<?> userConverters) {
			this(storeConversions, userConverters, Predicates.isTrue());
		}

		/**
		 * Create a new ConverterConfiguration holding the given {@link StoreConversions} and user defined converters as
		 * well as a {@link Collection} of {@link ConvertiblePair} for which to skip the registration of default converters.
		 * <br />
		 * This allows store implementations to modify default converter registration based on specific needs and
		 * configurations. User defined converters will are never subject of filtering.
		 *
		 * @param storeConversions must not be {@literal null}.
		 * @param userConverters must not be {@literal null} use {@link Collections#emptyList()} instead.
		 * @param converterRegistrationFilter must not be {@literal null}..
		 */
		public ConverterConfiguration(StoreConversions storeConversions, List<?> userConverters,
				Predicate<ConvertiblePair> converterRegistrationFilter) {

			this(storeConversions, userConverters, converterRegistrationFilter, PropertyValueConversions.simple(it -> {}));
		}

		/**
		 * Create a new ConverterConfiguration holding the given {@link StoreConversions} and user defined converters as
		 * well as a {@link Collection} of {@link ConvertiblePair} for which to skip the registration of default converters.
		 * <br />
		 * This allows store implementations to modify default converter registration based on specific needs and
		 * configurations. User defined converters will are never subject of filtering.
		 *
		 * @param storeConversions must not be {@literal null}.
		 * @param userConverters must not be {@literal null} use {@link Collections#emptyList()} instead.
		 * @param converterRegistrationFilter must not be {@literal null}.
		 * @param propertyValueConversions can be {@literal null}.
		 * @since 2.7
		 */
		public ConverterConfiguration(StoreConversions storeConversions, List<?> userConverters,
				Predicate<ConvertiblePair> converterRegistrationFilter,
				@Nullable PropertyValueConversions propertyValueConversions) {

			this.storeConversions = storeConversions;
			this.userConverters = new ArrayList<>(userConverters);
			this.converterRegistrationFilter = converterRegistrationFilter;
			this.propertyValueConversions = propertyValueConversions;
		}

		/**
		 * @return never {@literal null}
		 */
		StoreConversions getStoreConversions() {
			return storeConversions;
		}

		/**
		 * @return never {@literal null}.
		 */
		List<?> getUserConverters() {
			return userConverters;
		}

		/**
		 * @return never {@literal null}.
		 */
		boolean shouldRegister(ConvertiblePair candidate) {
			return this.converterRegistrationFilter.test(candidate);
		}

		/**
		 * @return the configured {@link PropertyValueConversions} if set, {@literal null} otherwise.
		 * @since 2.7
		 */
		public @Nullable PropertyValueConversions getPropertyValueConversions() {
			return this.propertyValueConversions;
		}
	}
}
