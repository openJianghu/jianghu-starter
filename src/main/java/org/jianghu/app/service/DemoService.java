package org.jianghu.app.service;

import cn.hutool.core.date.DateUtil;
import org.jianghu.app.common.Constant;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.common.JsonUtil;
import org.jianghu.app.common.annotation.JHValidate;
import org.jianghu.app.config.JianghuKnex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("demoServiceSystem")
public class DemoService {

    @Autowired
    private JianghuKnex jianghuKnex;

    /**
     *
     * @param actionData @JHValid
     * {
     *     type: 'object',
     *     additionalProperties: true,
     *     required: [ 'errorLogList' ],
     *     properties: {
     *       errorLogList: {
     *         type: 'array',
     *         items: {
     *           type: 'object',
     *           additionalProperties: true,
     *           required: [ 'errorTime', 'errorMessage' ],
     *           properties: {
     *             userId: { anyOf: [{ type: 'null' }, { type: 'string' }, { type: 'number' }] },
     *             deviceId: { anyOf: [{ type: 'null' }, { type: 'string' }, { type: 'number' }] },
     *             errorTime: { anyOf: [{ type: 'string' }] },
     *             errorMessage: { anyOf: [{ type: 'string' }, { type: 'number' }] },
     *           },
     *         },
     *       },
     *     },
     *  }
     */
    @Transactional
    public void addStudent(@JHValidate("{\n" +
            "    type: 'object',\n" +
            "    additionalProperties: true,\n" +
            "    required: [ 'studentId' ],\n" +
            "    properties: {\n" +
            "       studentId: { type: 'string' },\n" +
            "    },\n" +
            "  }") JSONPathObject actionData) throws Exception {

        /*
        jianghuKnex.insert("knex('student').insert({studentId: ${studentId}})", JSONPathObject.of("studentId", actionData.eval("studentId"))););

        String deleteSql = JsonUtil.replaceString("knex('student').where({ studentId: ${studentId} }).delete()", actionData);
        jianghuKnex.delete(deleteSql);

        List<JSONPathObject> studentList = jianghuKnex.select(JsonUtil.replaceString("knex('student').where({ gender: '男' }).select()", actionData));
        String name = studentList.get(0).eval("name");*/


        String knexStr = "knex('student').where({ gender: ${gender}}).jhUpdate({remarks: ${remarks}})";
        actionData
                .set("gender", "男")
                .set("remarks", DateUtil.date().toString(Constant.ISO8601));
        List<JSONPathObject> result = jianghuKnex.select(knexStr, actionData);
        System.out.println("actionData: " + actionData.toJSONString());
        System.out.println("result: " + JsonUtil.toJSONString(result));

        List<JSONPathObject> studentList = Arrays
                .stream(new String[]{"1001", "1002", "1003", "1004"})
                .map(studentId -> JSONPathObject.of("studentId", studentId).set("remarks", DateUtil.date().toString(Constant.ISO8601)))
                .collect(Collectors.toList());
        jianghuKnex.insert("knex('student').insert()", studentList);
    }


    public JSONPathObject hookTest(JSONPathObject actionData) throws Exception {
        return JSONPathObject.of("content", "hookTest====================");
    }

    /*public JSONPathObject validationTest(
            // @Validated([{key: "name", required: true, type: "string"}])
                                         JSONPathObject actionData) throws Exception {
        return JSONPathObject.of("content", "hookTest====================");
    }*/
}
