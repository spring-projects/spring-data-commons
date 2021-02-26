package org.springframework.data.repository.combination;

import org.springframework.data.repository.combination.entry.ExamEntry;

import java.io.Serializable;

/**
 * @Description: The interface has both jpa and batis functions, and does not need to write:
 *               <p>
 *                  public interface ExamService extends BaseJPA<ExamEntry, Serializable> , BaseBatis<T,ID>
 *               </p>
 *               If you have a lot of custom warehouses, this way reduces the amount of code duplication
 *
 * @Auther: create by cmj on 2021/2/26 19:08
 */
public interface ExamService extends BaseJpaBatis<ExamEntry, Serializable>{
}
