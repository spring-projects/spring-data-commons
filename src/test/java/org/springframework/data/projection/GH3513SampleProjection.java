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
package org.springframework.data.projection;

/**
 * Standalone projection interface used by {@link ProjectionMetadataCachingProbeTests} to reproduce GH-3513. Kept as
 * its own top-level type (rather than nested) so its {@code .class} file can be loaded independently through an
 * isolated {@link ClassLoader} without dragging in an enclosing class.
 *
 * @author Raphael Zanarelli
 */
public interface GH3513SampleProjection {

	String getFirstname();

	String getLastname();
}
