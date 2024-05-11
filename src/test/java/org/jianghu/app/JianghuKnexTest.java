package org.jianghu.app;


import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.config.JianghuKnex;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest(classes = JiangHuApplication.class)
public class JianghuKnexTest {

    @Autowired
    private JianghuKnex jianghuKnex;


    @Test
    void crud() {

        /*String knexStrOfInsert = "knex('student').jhInsert()";
        List<JSONPathObject> studentList = Arrays
                .stream(new String[]{"1001", "1002", "1003", "1004"})
                .map(studentId -> JSONPathObject.of("studentId", studentId).set("remarks", DateUtil.date().toString(Constant.ISO8601)))
                .collect(Collectors.toList());
        List<Integer> ids = jianghuKnex.insert(knexStrOfInsert, studentList);
        System.out.println("jianghuKnex.insert ids:" + ids);

        String knexStrOfUpdate = "knex('student').where({ gender: ${gender}}).jhUpdate({remarks: ${remarks}})";
        JSONPathObject actionData = JSONPathObject
                .of("gender", "男")
                .set("remarks", DateUtil.date().toString(Constant.ISO8601));
        int updateCount = jianghuKnex.update(knexStrOfUpdate, actionData);
        System.out.println("jianghuKnex.update updateCount:" + updateCount);


        String knexStrOfDelete = "knex('student').whereIn('id', ${ids}).jhDelete()";
        int deleteCount = jianghuKnex.delete(knexStrOfDelete, JSONPathObject.of("ids", ids));
        System.out.println("jianghuKnex.delete deleteCount:" + deleteCount);*/


        String knexStrOfSelect = "knex('student').where({ gender: ${gender} }).select()";
        List<JSONPathObject> list = jianghuKnex.select(knexStrOfSelect, JSONPathObject.of("gender", "男"));
        System.out.println("jianghuKnex.select list:" + list);

        String knexStrOfFirst = "knex('student').where({ gender: ${gender} }).first()";
        JSONPathObject firstObj = jianghuKnex.first(knexStrOfFirst, JSONPathObject.of("gender", "男"));
        System.out.println("jianghuKnex.first obj:" + firstObj);
    }

}


