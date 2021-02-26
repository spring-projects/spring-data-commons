package org.springframework.data.repository.combination;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @Description: A jpa - based interface feature
 * @Auther: create by cmj on 2021/2/26 19:02
 */
@NoRepositoryBean
public interface BaseJPA<T, ID> extends CrudRepository<T, ID> {
}
