package org.springframework.data.querydsl;


import com.querydsl.core.types.Predicate;

/**
 * Interface to allow execution of QueryDsl modifying {@link Predicate} instances.
 *
 * @author Nikita Mishchenko
 */
public interface QueryDslPredicateModifyingExecutor {

    /**
     * Delete all entities matching the given {@link Predicate}.
     *
     * @return the number of all entities affected by the given {@link Predicate}.
     */
    long delete(Predicate... predicate);

}
