/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.core;

/**
 * Just a value object to hold the validation rules.
 *
 * @author Christoph Strobl
 * @since 4.2
 */
class SimplePathValidationRules implements PathValidationRules {

	private final SegmentClassification segmentClassifier;
	private final AccessValidator accessValidator;
	private final MapKeyValidator mapKeyValidator;
	private final IndexValidator indexValidator;
	private final int maxAllowedSegments;
	private final int maxSegmentLength;
	private final int maxPathLength;

	public SimplePathValidationRules(SegmentClassification segmentClassifier, AccessValidator accessValidator,
			MapKeyValidator mapKeyValidator, IndexValidator indexValidator, int maxAllowedSegments, int maxSegmentLength,
			int maxPathLength) {

		this.segmentClassifier = segmentClassifier;
		this.accessValidator = accessValidator;
		this.mapKeyValidator = mapKeyValidator;
		this.indexValidator = indexValidator;
		this.maxAllowedSegments = maxAllowedSegments;
		this.maxSegmentLength = maxSegmentLength;
		this.maxPathLength = maxPathLength;
	}

	@Override
	public SegmentClassification segmentClassifier() {
		return segmentClassifier;
	}

	@Override
	public AccessValidator accessValidator() {
		return accessValidator;
	}

	@Override
	public MapKeyValidator mapKeyValidator() {
		return mapKeyValidator;
	}

	@Override
	public IndexValidator indexValidator() {
		return indexValidator;
	}

	@Override
	public int maxAllowedSegments() {
		return maxAllowedSegments;
	}

	@Override
	public int maxSegmentLength() {
		return maxSegmentLength;
	}

	@Override
	public int maxPathLength() {
		return maxPathLength;
	}
}
