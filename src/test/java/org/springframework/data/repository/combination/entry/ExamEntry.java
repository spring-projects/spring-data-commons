package org.springframework.data.repository.combination.entry;

import javax.persistence.*;

/**
 * @Description: This is an examination table in the database
 * @Auther: create by cmj on 2021/2/26 19:08
 */
@Entity(name = "exam")
public class ExamEntry {
    //...
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "exam_id",columnDefinition = "int(11) not null auto_increment comment 'unique identification'")
    private Integer examId;
}
