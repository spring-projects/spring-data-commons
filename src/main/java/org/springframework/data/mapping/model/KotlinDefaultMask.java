/*
 * Copyright 2018-2025 the original author or authors.
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
package org.springframework.data.mapping.model;

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.KParameter.Kind;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * Value object representing defaulting masks used for Kotlin methods applying parameter defaulting.
 *
 * @author Mark Paluch
 * @since 2.1
 */
public class KotlinDefaultMask {

	private final int[] defaulting;

	private KotlinDefaultMask(int[] defaulting) {
		this.defaulting = defaulting;
	}

	/**
	 * Callback method to notify {@link IntConsumer} for each defaulting mask.
	 *
	 * @param maskCallback must not be {@literal null}.
	 */
	public void forEach(IntConsumer maskCallback) {

		for (int i : defaulting) {
			maskCallback.accept(i);
		}
	}

	/**
	 * Return the number of defaulting masks required to represent the number of {@code arguments}.
	 *
	 * @param arguments number of method arguments.
	 * @return the number of defaulting masks required. Returns at least one to be used with {@code copy} methods.
	 */
	public static int getMaskCount(int arguments) {
		return ((arguments - 1) / Integer.SIZE) + 1;
	}

	/**
	 * Return the number of defaulting masks required to represent the number of {@code optionalParameterCount}.
	 * In contrast to {@link #getMaskCount(int)}, this method can return zero if there are no optional parameters available.
	 *
	 * @param optionalParameterCount number of method arguments.
	 * @return the number of defaulting masks required. Returns zero if no optional parameters are available.
	 */
	static int getExactMaskCount(int optionalParameterCount) {
		return optionalParameterCount == 0 ? 0 : getMaskCount(optionalParameterCount);
	}

	/**
	 * Creates defaulting mask(s) used to invoke Kotlin {@literal default} methods that conditionally apply parameter
	 * values.
	 *
	 * @param function the {@link KFunction} that should be invoked.
	 * @param isPresent {@link Predicate} for the presence/absence of parameters.
	 * @return {@link KotlinDefaultMask}.
	 */
	public static KotlinDefaultMask from(KFunction<?> function, Predicate<KParameter> isPresent) {
		return forCopy(function, isPresent);
	}

	/**
	 * Creates defaulting mask(s) used to invoke Kotlin {@literal copy} methods that conditionally apply parameter values.
	 *
	 * @param function the {@link KFunction} that should be invoked.
	 * @param isPresent {@link Predicate} for the presence/absence of parameters.
	 * @return {@link KotlinDefaultMask}.
	 */
	static KotlinDefaultMask forCopy(KFunction<?> function, Predicate<KParameter> isPresent) {
		return from(function, isPresent, true);
	}

	/**
	 * Creates defaulting mask(s) used to invoke Kotlin constructors where a defaulting mask isn't required unless there's
	 * one nullable argument.
	 *
	 * @param function the {@link KFunction} that should be invoked.
	 * @param isPresent {@link Predicate} for the presence/absence of parameters.
	 * @return {@link KotlinDefaultMask}.
	 */
	static KotlinDefaultMask forConstructor(KFunction<?> function, Predicate<KParameter> isPresent) {
		return from(function, isPresent, false);
	}

	/**
	 * Creates defaulting mask(s) used to invoke Kotlin {@literal default} methods that conditionally apply parameter
	 * values.
	 *
	 * @param function the {@link KFunction} that should be invoked.
	 * @param isPresent {@link Predicate} for the presence/absence of parameters.
	 * @return {@link KotlinDefaultMask}.
	 */
	private static KotlinDefaultMask from(KFunction<?> function, Predicate<KParameter> isPresent,
			boolean requiresAtLeastOneMask) {

		List<Integer> masks = new ArrayList<>();
		int index = 0;
		int mask = 0;
		boolean hasSeenParameter = false;

		List<KParameter> parameters = function.getParameters();

		for (KParameter parameter : parameters) {

			if (index != 0 && index % Integer.SIZE == 0) {
				masks.add(mask);
				mask = 0;
			}

			if (parameter.isOptional()) {
				hasSeenParameter = true;

				if (!isPresent.test(parameter)) {
					mask = mask | (1 << (index % Integer.SIZE));
				}
			}

			if (parameter.getKind() == Kind.VALUE) {
				index++;
			}
		}

		if (hasSeenParameter || requiresAtLeastOneMask) {
			masks.add(mask);
		}

		int[] defaulting = new int[masks.size()];
		for (int i = 0; i < masks.size(); i++) {
			defaulting[i] = masks.get(i);
		}

		return new KotlinDefaultMask(defaulting);
	}

	public int[] getDefaulting() {
		return this.defaulting;
	}
}
