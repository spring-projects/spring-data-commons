package org.springframework.data.repository;

import java.lang.annotation.*;

/**
 * @Description: Identify that the interface is a composite interface, so the interface can not be used directly,
 *               and the parent interfaces should be used, Continue this logical lookup if one of the parent interfaces uses the annotation,
 *               Use the following scenario, for example：
 *               <p>
 *                    public interface BaseBatis<T,ID>{}
 *                    public interface BaseBatisImpl<T,ID>{
 *                        //...Method and logic defined by oneself
 *                    }
 *                    ...
 *               </p>
 *               <p>
 *                  @NoRepositoryBean
 *                  @JpaRepositoryCombination
 *                  public interface BaseJpaBatis<T,ID> extends  JpaRepository<T,ID>,BaseBatis<T,ID> ... { }
 *
 *                  public interface OfferQuestionService extends BaseJpaBatis<OfferQuestionEntry, Serializable> {}
 *
 *                  public interface ExamPracticeService extends BaseJpaBatis<BrowserUserEntry, Serializable> {}
 *
 *                  @Controller
 *                  @RequestMapping("/")
 *                  public class OfferQuestionController {
 *
 *                  @Resource
 *                  OfferQuestionService offerQuestionService;
 *                  @Resource
 *                  ExamPracticeService examPracticeService;
 *
 *               </p>
 *               Solve the problem of duplicate inheritance interface, avoid duplicate inheritance interface
 *
 * @Auther: create by CaoMingjie
 */
@Target({ ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JpaRepositoryCombination {
}
