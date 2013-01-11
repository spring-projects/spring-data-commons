package org.springframework.data.repository.config;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.util.ClassUtils;
import org.springframework.util.Assert;

/**
 * Custom {@link ClassPathScanningCandidateComponentProvider} scanning for interfaces extending the given base
 * interface. Skips interfaces annotated with {@link NoRepositoryBean}.
 * 
 * @author Oliver Gierke
 */
class RepositoryComponentProvider extends ClassPathScanningCandidateComponentProvider {

	/**
	 * Creates a new {@link RepositoryComponentProvider} using the given {@link TypeFilter} to include components to be
	 * picked up.
	 * 
	 * @param includeFilters the {@link TypeFilter}s to select repository interfaces to consider, must not be
	 *          {@literal null}.
	 */
	public RepositoryComponentProvider(Iterable<? extends TypeFilter> includeFilters) {

		super(false);

		Assert.notNull(includeFilters);

		if (includeFilters.iterator().hasNext()) {
			for (TypeFilter filter : includeFilters) {
				addIncludeFilter(filter);
			}
		} else {
			super.addIncludeFilter(new InterfaceTypeFilter(Repository.class));
			super.addIncludeFilter(new AnnotationTypeFilter(RepositoryDefinition.class, true, true));
		}

		addExcludeFilter(new AnnotationTypeFilter(NoRepositoryBean.class));
	}

	/**
	 * Custom extension of {@link #addIncludeFilter(TypeFilter)} to extend the added {@link TypeFilter}. For the
	 * {@link TypeFilter} handed we'll have two filters registered: one additionally enforcing the
	 * {@link RepositoryDefinition} annotation, the other one forcing the extension of {@link Repository}.
	 * 
	 * @see ClassPathScanningCandidateComponentProvider#addIncludeFilter(TypeFilter)
	 */
	@Override
	public void addIncludeFilter(TypeFilter includeFilter) {

		List<TypeFilter> filterPlusInterface = new ArrayList<TypeFilter>(2);
		filterPlusInterface.add(includeFilter);
		filterPlusInterface.add(new InterfaceTypeFilter(Repository.class));

		super.addIncludeFilter(new AllTypeFilter(filterPlusInterface));

		List<TypeFilter> filterPlusAnnotation = new ArrayList<TypeFilter>(2);
		filterPlusAnnotation.add(includeFilter);
		filterPlusAnnotation.add(new AnnotationTypeFilter(RepositoryDefinition.class, true, true));

		super.addIncludeFilter(new AllTypeFilter(filterPlusAnnotation));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider#isCandidateComponent(org.springframework.beans.factory.annotation.AnnotatedBeanDefinition)
	 */
	@Override
	protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {

		boolean isNonRepositoryInterface = !ClassUtils.isGenericRepositoryInterface(beanDefinition.getBeanClassName());
		boolean isTopLevelType = !beanDefinition.getMetadata().hasEnclosingClass();

		return isNonRepositoryInterface && isTopLevelType;
	}

	/**
	 * {@link org.springframework.core.type.filter.TypeFilter} that only matches interfaces. Thus setting this up makes
	 * only sense providing an interface type as {@code targetType}.
	 * 
	 * @author Oliver Gierke
	 */
	private static class InterfaceTypeFilter extends AssignableTypeFilter {

		/**
		 * Creates a new {@link InterfaceTypeFilter}.
		 * 
		 * @param targetType
		 */
		public InterfaceTypeFilter(Class<?> targetType) {
			super(targetType);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter#match(org.springframework.core.type.classreading.MetadataReader, org.springframework.core.type.classreading.MetadataReaderFactory)
		 */
		@Override
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {

			return metadataReader.getClassMetadata().isInterface() && super.match(metadataReader, metadataReaderFactory);
		}
	}

	// Copy of Spring's AnnotationTypeFilter until SPR-8336 gets resolved.

	/**
	 * A simple filter which matches classes with a given annotation, checking inherited annotations as well.
	 * <p>
	 * The matching logic mirrors that of <code>Class.isAnnotationPresent()</code>.
	 * 
	 * @author Mark Fisher
	 * @author Ramnivas Laddad
	 * @author Juergen Hoeller
	 * @since 2.5
	 */
	private static class AnnotationTypeFilter extends AbstractTypeHierarchyTraversingFilter {

		private final Class<? extends Annotation> annotationType;

		private final boolean considerMetaAnnotations;

		/**
		 * Create a new AnnotationTypeFilter for the given annotation type. This filter will also match meta-annotations. To
		 * disable the meta-annotation matching, use the constructor that accepts a ' <code>considerMetaAnnotations</code>'
		 * argument. The filter will not match interfaces.
		 * 
		 * @param annotationType the annotation type to match
		 */
		public AnnotationTypeFilter(Class<? extends Annotation> annotationType) {
			this(annotationType, true);
		}

		/**
		 * Create a new AnnotationTypeFilter for the given annotation type. The filter will not match interfaces.
		 * 
		 * @param annotationType the annotation type to match
		 * @param considerMetaAnnotations whether to also match on meta-annotations
		 */
		public AnnotationTypeFilter(Class<? extends Annotation> annotationType, boolean considerMetaAnnotations) {
			this(annotationType, considerMetaAnnotations, false);
		}

		/**
		 * Create a new {@link AnnotationTypeFilter} for the given annotation type.
		 * 
		 * @param annotationType the annotation type to match
		 * @param considerMetaAnnotations whether to also match on meta-annotations
		 * @param considerInterfaces whether to also match interfaces
		 */
		public AnnotationTypeFilter(Class<? extends Annotation> annotationType, boolean considerMetaAnnotations,
				boolean considerInterfaces) {
			super(annotationType.isAnnotationPresent(Inherited.class), considerInterfaces);
			this.annotationType = annotationType;
			this.considerMetaAnnotations = considerMetaAnnotations;
		}

		@Override
		protected boolean matchSelf(MetadataReader metadataReader) {
			AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
			return metadata.hasAnnotation(this.annotationType.getName())
					|| (this.considerMetaAnnotations && metadata.hasMetaAnnotation(this.annotationType.getName()));
		}

		@Override
		protected Boolean matchSuperClass(String superClassName) {
			if (Object.class.getName().equals(superClassName)) {
				return Boolean.FALSE;
			} else if (superClassName.startsWith("java.")) {
				try {
					Class<?> clazz = getClass().getClassLoader().loadClass(superClassName);
					return (clazz.getAnnotation(this.annotationType) != null);
				} catch (ClassNotFoundException ex) {
					// Class not found - can't determine a match that way.
				}
			}
			return null;
		}
	}

	/**
	 * Helper class to create a {@link TypeFilter} that matches if all the delegates match.
	 * 
	 * @author Oliver Gierke
	 */
	private static class AllTypeFilter implements TypeFilter {

		private final List<TypeFilter> delegates;

		/**
		 * Creates a new {@link AllTypeFilter} to match if all the given delegates match.
		 * 
		 * @param delegates must not be {@literal null}.
		 */
		public AllTypeFilter(List<TypeFilter> delegates) {

			Assert.notNull(delegates);
			this.delegates = delegates;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.core.type.filter.TypeFilter#match(org.springframework.core.type.classreading.MetadataReader, org.springframework.core.type.classreading.MetadataReaderFactory)
		 */
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {

			for (TypeFilter filter : delegates) {
				if (!filter.match(metadataReader, metadataReaderFactory)) {
					return false;
				}
			}

			return true;
		}
	}
}
