/*
 * Copyright 2015-2023 the original author or authors.
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
package org.springframework.data.util;

import org.springframework.util.Assert;

import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A tuple of things.
 *
 * @param <S> Type of the first thing.
 * @param <T> Type of the second thing.
 * @author Tobias Trelle
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Iman Hosseinzadeh
 * @since 1.12
 */
public record Pair<S, T>(S first, T second) {
    public Pair {

        Assert.notNull(first, "First must not be null");
        Assert.notNull(second, "Second must not be null");
    }

    /**
     * Creates a new {@link Pair} for the given elements.
     *
     * @param first  must not be {@literal null}.
     * @param second must not be {@literal null}.
     * @return
     */
    public static <S, T> Pair<S, T> of(S first, T second) {
        return new Pair<>(first, second);
    }

    /**
     * A collector to create a {@link Map} from a {@link Stream} of {@link Pair}s.
     *
     * @return
     */
    public static <S, T> Collector<Pair<S, T>, ?, Map<S, T>> toMap() {
        return Collectors.toMap(Pair::first, Pair::second);
    }

    /**
     * @return
     * @deprecated for removal. Use {@link #first()} instead.
     */
    @Deprecated(since = "3.2", forRemoval = true)
    public S getFirst() {
        return this.first;
    }

    /**
     * @return
     * @deprecated for removal. Use {@link #second()} instead.
     */
    @Deprecated(since = "3.2", forRemoval = true)
    public T getSecond() {
        return this.second;
    }

    @Override
    public String toString() {
        return String.format("%s->%s", this.first, this.second);
    }
}
