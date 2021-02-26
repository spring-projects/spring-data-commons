package org.springframework.data.repository.combination;

import org.springframework.data.repository.JpaRepositoryCombination;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @Description:
 * @Auther: create by cmj on 2021/2/26 19:03
 */
@NoRepositoryBean
//Without this annotation （@JpaRepositoryCombination）, the child interface that inherits the BaseJpaBatis will
// not find the implementation class of the BaseBatis interface, and errors can occur at startup
@JpaRepositoryCombination
public interface BaseJpaBatis<T, ID> extends BaseJPA<T,ID>,BaseBatis<T,ID> {
}
