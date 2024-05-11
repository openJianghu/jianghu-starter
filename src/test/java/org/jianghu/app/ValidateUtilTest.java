package org.jianghu.app;

import com.alibaba.fastjson2.JSONWriter;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.common.ValidateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest(classes = JiangHuApplication.class)
public class ValidateUtilTest {

    public static void main(String[] args) throws Exception {
        JSONPathObject schema = JSONPathObject
                .of("type", "object")
                .set("required", new String[]{"name", "age", "appData", "studentIdList"})
                .set("properties.name.type", "string")
                .set("properties.name.enum", new String[]{"张三", "李四"})
                .set("properties.age.type", "integer")

                .set("appData.type", "object")
                .set("properties.appData.required", new String[]{"name", "age"})
                .set("properties.appData.properties.name.type", "string")
                .set("properties.appData.properties.age.type", "integer")

                .set("properties.studentIdList.type", "array")
                .set("properties.studentIdList.maxItems", 1)
                .set("properties.studentIdList.items.type", "object");


        JSONPathObject jsonData = JSONPathObject.of("name", "张三")
                .set("age", 11)
                .set("address", "北京市海淀区")
                .set("appData.name", "张三")
                .set("appData.age", 11)
                .set("studentIdList", List.of(new Object()))
                ;


        System.out.println("schema: " + schema.toJSONString(JSONWriter.Feature.PrettyFormat));
        System.out.println("jsonData: " + jsonData.toJSONString(JSONWriter.Feature.PrettyFormat));
        ValidateUtil.validate(schema, jsonData, null);
    }


}
