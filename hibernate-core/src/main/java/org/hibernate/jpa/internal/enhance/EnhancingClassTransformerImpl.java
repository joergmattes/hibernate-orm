/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.enhance;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.CtClass;
import javassist.CtField;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.ClassTransformer;

/**
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
public class EnhancingClassTransformerImpl implements ClassTransformer {

	private final EnhancementContext enhancementContext;

	public EnhancingClassTransformerImpl(EnhancementContext enhancementContext) {
		this.enhancementContext = enhancementContext;
	}

	@Override
	public byte[] transform(
			ClassLoader loader,
			String className,
			Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {

		// The first design had the enhancer as a class variable. That approach had some goods and bads.
		// We don't had to create an enhancer for each class, but on the other end it would stay in memory forever.
		// It also assumed that all calls come from the same class loader, which is fair, but this makes it more robust.

		try {
			Enhancer enhancer = new Enhancer( new EnhancementContextWrapper( enhancementContext, loader ) );
			return enhancer.enhance( className, classfileBuffer );
		}
		catch (final Exception e) {
			throw new IllegalClassFormatException( "Error performing enhancement of " + className ) {
				@Override
				public synchronized Throwable getCause() {
					return e;
				}
			};
		}
	}

	// Wrapper for a EnhancementContext that allows to set the right classloader.
	private class EnhancementContextWrapper implements EnhancementContext {

		private final ClassLoader loadingClassloader;
		private final EnhancementContext wrappedContext;

		private EnhancementContextWrapper(EnhancementContext wrappedContext, ClassLoader loadingClassloader) {
			this.wrappedContext = wrappedContext;
			this.loadingClassloader = loadingClassloader;
		}

		@Override
		public ClassLoader getLoadingClassLoader() {
			return loadingClassloader;
		}

		@Override
		public boolean isEntityClass(CtClass classDescriptor) {
			return wrappedContext.isEntityClass( classDescriptor );
		}

		@Override
		public boolean isCompositeClass(CtClass classDescriptor) {
			return wrappedContext.isCompositeClass( classDescriptor );
		}

		@Override
		public boolean isMappedSuperclassClass(CtClass classDescriptor) {
			return wrappedContext.isMappedSuperclassClass( classDescriptor );
		}

		@Override
		public boolean doBiDirectionalAssociationManagement(CtField field) {
			return wrappedContext.doBiDirectionalAssociationManagement( field );
		}

		@Override
		public boolean doDirtyCheckingInline(CtClass classDescriptor) {
			return wrappedContext.doDirtyCheckingInline( classDescriptor );
		}

		@Override
		public boolean doExtendedEnhancement(CtClass classDescriptor) {
			return wrappedContext.doExtendedEnhancement( classDescriptor );
		}

		@Override
		public boolean hasLazyLoadableAttributes(CtClass classDescriptor) {
			return wrappedContext.hasLazyLoadableAttributes( classDescriptor );
		}

		@Override
		public boolean isPersistentField(CtField ctField) {
			return wrappedContext.isPersistentField( ctField );
		}

		@Override
		public CtField[] order(CtField[] persistentFields) {
			return wrappedContext.order( persistentFields );
		}

		@Override
		public boolean isLazyLoadable(CtField field) {
			return wrappedContext.isLazyLoadable( field );
		}

		@Override
		public boolean isMappedCollection(CtField field) {
			return wrappedContext.isMappedCollection( field );
		}
	}

}
