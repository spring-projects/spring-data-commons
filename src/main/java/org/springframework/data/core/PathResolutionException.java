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

import org.jspecify.annotations.Nullable;

/**
 * Exception being thrown when a property path is not valid.
 * <p>
 * {@link #getMessage()} names nothing but the rejected path. {@link #getDetailedMessage()} describes why the path was
 * rejected and must not be propagated to a client.
 *
 * @author Christoph Strobl
 * @since 4.2
 */
public class PathResolutionException extends PropertyResolutionException {

	private static final int MAX_MESSAGE_PATH_LENGTH = 64;
	private static final String MESSAGE_TEMPLATE = "Invalid property path '%s'";

	private final String path;

	/**
	 * {@literal null} if the detailed message is to be derived from the {@link #getCause() cause} on demand. Deriving it
	 * eagerly would force the cause to render hints for a path that is typically rejected and never looked at.
	 */
	private final @Nullable String detailedMessage;

	/**
	 * Creates a new {@link PathResolutionException} using the given detailed message as message of a generated cause.
	 *
	 * @param path the rejected property path.
	 * @param detailedMessage the description of why the path was rejected.
	 */
	public PathResolutionException(String path, String detailedMessage) {
		this(path, detailedMessage, new PropertyResolutionException(detailedMessage));
	}

	/**
	 * Creates a new {@link PathResolutionException} for the given cause, using the cause's message as detailed message.
	 *
	 * @param path the rejected property path.
	 * @param cause the exception describing why the path was rejected.
	 */
	public PathResolutionException(String path, PropertyResolutionException cause) {

		super(String.format(MESSAGE_TEMPLATE, abbreviate(path)), cause);

		this.path = path;
		this.detailedMessage = null;
	}

	private PathResolutionException(String path, String detailedMessage, PropertyResolutionException cause) {

		super(String.format(MESSAGE_TEMPLATE, abbreviate(path)), cause);

		this.path = path;
		this.detailedMessage = detailedMessage;
	}

	static String abbreviate(String value) {

		return value.length() <= MAX_MESSAGE_PATH_LENGTH //
				? value //
				: value.substring(0, MAX_MESSAGE_PATH_LENGTH) + String.format("... (%d chars)", value.length());
	}

	/**
	 * @return the rejected property path.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return the detailed message - not for public consumption.
	 * @see #getMessage()
	 */
	public String getDetailedMessage() {

		if (this.detailedMessage != null) {
			return this.detailedMessage;
		}

		Throwable cause = getCause();
		String causeMessage = cause != null ? cause.getMessage() : null;

		return causeMessage != null ? causeMessage : getMessage();
	}
}
