package org.jianghu.app;

import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.common.JsonUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

@Slf4j
public class JsonUtilTest {

    @Test
    void JSONPathObject_test() throws Exception {
        String jsonString = "{ \"school\": { \"name\": \"Example School\", \"location\": \"New York\", \"students\": [ { \"id\": 1, \"name\": \"Alice\", \"age\": 20, \"classes\": [\"Math\", \"Physics\"] }, { \"id\": 2, \"name\": \"Bob\", \"age\": 22, \"classes\": [\"Chemistry\", \"Biology\"] } ], \"teachers\": [ { \"id\": 100, \"name\": \"Mr. Smith\", \"subject\": \"Math\" }, { \"id\": 101, \"name\": \"Mrs. Johnson\", \"subject\": \"Biology\" } ] } }";
        JSONPathObject jsonPathObject = JsonUtil.parseObject(jsonString);

        // path提取数据
        System.out.println("school: " + jsonPathObject.eval("school", JSONPathObject.class));
        System.out.println("school.name: " + jsonPathObject.eval("school.name"));
        System.out.println("school.students[0].name: " + jsonPathObject.eval("school.students[0].name", String.class));
        System.out.println("school.students[1].name: " + jsonPathObject.eval("school.students[1].name", String.class));
        System.out.println("school.students: " + jsonPathObject.eval("school.students", List.class));
        System.out.println("school.students[0].classes[1]: " + jsonPathObject.eval("school.students[0].classes[1]"));

        // 通配提取数据
        List<String> allSubjects = jsonPathObject.eval("school.teachers[*].subject", List.class);
        System.out.println("All subjects: " + allSubjects);

        // path设置数据
        jsonPathObject.set("school.nameLocal", "Example School Local");
        System.out.println("school.nameLocal: " + jsonPathObject.eval("school.nameLocal"));
    }


    @Test
    void toJSON_toJSONString() throws Exception {
        Address address = new Address("123 Main St", "New York", "NY", "10001");
        Person person = new Person("Alice", 30, "alice@example.com", address);

        // ===============JsonUtil.toJSON
        JSONPathObject jsonPathObject = JsonUtil.toJSON(person);
        System.out.println("name: " + jsonPathObject.eval("name"));
        System.out.println("address.city: " + jsonPathObject.eval("address.city"));
        System.out.println("address: " + jsonPathObject.eval("address"));

        // ===============JsonUtil.toJSONString
        System.out.println("JsonUtil.toJSONString: " + JsonUtil.toJSONString(person));
    }


    @Test
    void parseArray_toJSONList() throws Exception {
        String jsonString1 = "[\"Alice\", \"Bob\", \"Charlie\"]";
        String jsonString2 = "[{\"name\":\"Alice\"}, {\"name\":\"Bob\"}, {\"name\":\"Charlie\"}]";

        List<String> strList = JsonUtil.parseArray(jsonString1, String.class);
        List<Object> objList = JsonUtil.parseArray(jsonString2, Object.class);
        List<JSONPathObject> jsonPathObjectList = JsonUtil.parseArray(jsonString2, JSONPathObject.class);
        System.out.println("strList: " + strList);
        System.out.println("objList: " + objList);
        System.out.println("jsonPathObjectList: " + jsonPathObjectList);


        Address address = new Address("123 Main St", "New York", "NY", "10001");
        List<Person> personList = List.of(
                new Person("Alice", 30, "11@qq.com", address),
                new Person("Bob", 40, "22@qq.com", address),
                new Person("Charlie", 50, "33@qq.com", address));
        List<JSONPathObject> personListOfPathObject = JsonUtil.toJSONList(personList);
        personListOfPathObject.forEach(item -> {
            System.out.println(item.eval("name", String.class));
        });
    }

    @Test
    void replaceString() throws Exception {
        String knexStr = JsonUtil.replaceString("knex('student')" +
                        ".where({ studentId: ${studentId} })" +
                        ".whereIn('studentId', ${studentIdList})" +
                        ".update(${updateData})" +
                        ".select()",
                new JSONPathObject()
                        .set("studentId", "S1001")
                        .set("studentIdList", List.of("S1001", "S1002", "S1003"))
                        .set("updateData", Map.of("level", 2, "name", "张三"))
        );
        System.out.println("knexStr: " + knexStr);
    }


    @Data
    public static class Person {
        private String name;
        private int age;
        private String email;
        private Address address;

        public Person(String name, int age, String email, Address address) {
            this.name = name;
            this.age = age;
            this.email = email;
            this.address = address;
        }
    }

    @Data
    public static class Address {
        private String street;
        private String city;
        private String state;
        private String zipCode;

        public Address(String street, String city, String state, String zipCode) {
            this.street = street;
            this.city = city;
            this.state = state;
            this.zipCode = zipCode;
        }
    }
}
