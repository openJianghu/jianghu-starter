package org.jianghu.app.config;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONWriter;
import com.caoccao.javet.enums.JSRuntimeType;
import com.caoccao.javet.interop.NodeRuntime;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.engine.IJavetEngine;
import com.caoccao.javet.interop.engine.JavetEnginePool;
import com.caoccao.javet.node.modules.NodeModuleModule;
import org.jianghu.app.common.*;
import org.jianghu.app.context.ContextHolder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class JianghuKnex {

    private V8Runtime knexV8Runtime;
    private NodeRuntime knexNodeRuntime;
    public JdbcTemplate jdbc;
    private Boolean jhIdConfig_enable;
    private String jhIdConfig_jhId;
    private Collection<String> jhIdConfig_careTableViewList;

    public JianghuKnex(DataSource dataSource, JSONPathObject jhIdConfig) throws Exception {
        knexV8RuntimeInit();
        // knexNodeRuntimeInit();
        this.jdbc = new JdbcTemplate(dataSource);
        jhIdConfig_enable = jhIdConfig.eval("enable", Boolean.class);
        if (jhIdConfig_enable) {
            jhIdConfig_jhId = jhIdConfig.eval("jhId", String.class);
            Map<String, String> careTableViewListMap = jhIdConfig.eval("careTableViewList", Map.class);
            jhIdConfig_careTableViewList = careTableViewListMap.values();
        }
    }

    @Data
    public class KnexInfo {
        private String knexStr;
        private String knexCommand;
        private String tableName;
        private String operation;
        private Object data;
        private String knexSql;
        public KnexInfo() {}
    }

    public List<JSONPathObject> execute(String knexStr) {
        return execute(knexStr, null);
    }

    public List<JSONPathObject> execute(String knexStr, JSONPathObject data) {
        String operation = ReUtil.get("\\w+(?=\\([^)]*\\)$)", knexStr, 0);
        List<JSONPathObject> result = null;
        switch (operation) {
            case "select":
                result = this.select(knexStr, data);
                break;
            case "insert":
            case "jhInsert":
                List<Integer> insertResult = this.insert(knexStr, data);
                result = insertResult.stream().map(id -> JSONPathObject.of("id", id)).collect(Collectors.toList());
                break;
            case "update":
            case "jhUpdate":
                int updateCount = this.update(knexStr, data);
                result = Arrays.asList(JSONPathObject.of("count", updateCount));
                break;
            case "delete":
            case "jhDelete":
                int deleteCount = this.delete(knexStr, data);
                result = Arrays.asList(JSONPathObject.of("count", deleteCount));
                break;
           default:
               result = this.select(knexStr, data);
               break;
        }
        return result;
    }


    public int count(String knexStr) {
        return count(knexStr, null);
    }

    public int count(String knexStr, JSONPathObject data) {
        KnexInfo knexInfo = parseKnexInfo(knexStr, data);
        Integer count = this.jdbc.queryForObject(knexInfo.knexSql, Integer.class);
        return count;
    }

    public JSONPathObject first(String knexStr) {
        return first(knexStr, null);
    }

    public JSONPathObject first(String knexStr, JSONPathObject data) {
        KnexInfo knexInfo = parseKnexInfo(knexStr, data);
        List<Map<String, Object>> list = this.jdbc.queryForList(knexInfo.knexSql);
        JSONPathObject object = CollectionUtils.isEmpty(list) ? null : JsonUtil.toJSON(list.get(0));
        return object;
    }

    public List<JSONPathObject> select(String knexStr) {
        return select(knexStr, null);
    }

    public List<JSONPathObject> select(String knexStr, JSONPathObject data) {
        KnexInfo knexInfo = parseKnexInfo(knexStr, data);
        List<Map<String, Object>> list = this.jdbc.queryForList(knexInfo.knexSql);
        return JsonUtil.toJSONList(list);
    }

    public List<Integer> insert(String knexStr, JSONPathObject insertDataOne) {
        if (insertDataOne == null) {
            return insert(knexStr, List.of());
        }
        return insert(knexStr, List.of(insertDataOne));
    }

    public List<Integer> insert(String knexStr, List<JSONPathObject> insertDataList) {
        String operation = ReUtil.get("\\w+(?=\\([^)]*\\)$)", knexStr, 0);
        String tableName = ReUtil.get("knex\\((['\"])([^'\"]+)\\1", knexStr, 2);
        boolean jhIdEnable = jhIdConfig_enable && jhIdConfig_careTableViewList.contains(tableName);
        JSONPathObject data = new JSONPathObject();
        if (!insertDataList.isEmpty()) {
            knexStr = knexStr
                    .replaceAll("\\.insert\\(.*\\)", "\\.insert(\\$\\{\\__insertData__\\})")
                    .replaceAll("\\.jhInsert\\(.*\\)", "\\.jhInsert(\\$\\{\\__insertData__\\})");
            insertDataList.stream().forEach(item -> {
                item.set("operation", operation);
                item.set("operationByUserId", ContextHolder.eval("userInfo.userId"));
                item.set("operationByUser", ContextHolder.eval("userInfo.username"));
                item.set("operationAt", DateUtil.date().toString(Constant.ISO8601));
                if (jhIdEnable) {
                    item.set("jhId", jhIdConfig_jhId);
                }
            });
            data.set("__insertData__", insertDataList);
        }

        KnexInfo knexInfo = parseKnexInfo(knexStr, data);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        this.jdbc.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(knexInfo.knexSql, Statement.RETURN_GENERATED_KEYS);
            return preparedStatement;
        }, keyHolder);
        List<Map<String, Object>> keyList = keyHolder.getKeyList();
        List<Integer> ids = keyList.stream().map(key -> Integer.valueOf(key.get("GENERATED_KEY").toString())).collect(Collectors.toList());
        if (knexInfo.operation.equals("jhInsert")) {
            this.backupNewDataListToRecordHistory(knexInfo.tableName, ids, knexInfo.operation);
        }
        return ids;
    }


    public int update(String knexStr) {
        return update(knexStr, null);
    }

    public int update(String knexStr, JSONPathObject data) {
        KnexInfo knexInfo = parseKnexInfo(knexStr, data);

        List<Integer> ids = null;
        if (knexInfo.operation.equals("jhUpdate")) {
            String knexCommandOfIdSelect = knexInfo.knexCommand.replaceAll("\\.update\\(\\{.*?\\}\\)", ".select('id')");
            String knexSqlOfIdSelect = parseKnexInfo(knexCommandOfIdSelect).knexSql;
            ids = this.jdbc.queryForList(knexSqlOfIdSelect).stream().map(item -> Integer.valueOf(item.get("id").toString())).collect(Collectors.toList());
            this.backupNewDataListToRecordHistory(knexInfo.tableName, ids, "jhUpdate:before");
        }

        int count = this.jdbc.update(knexInfo.knexSql);

        if (knexInfo.operation.equals("jhUpdate")) {
            this.backupNewDataListToRecordHistory(knexInfo.tableName, ids, "jhUpdate:after");
        }
        return count;
    }

    public int delete(String knexStr) {
        return delete(knexStr, null);
    }

    public int delete(String knexStr, JSONPathObject data) {
        KnexInfo knexInfo = parseKnexInfo(knexStr, data);

        List<Integer> ids = null;
        if (knexInfo.operation.equals("jhDelete")) {
            String knexCommandOfIdSelect = knexInfo.knexCommand.replaceAll("\\.delete\\(.*?\\)", ".select('id')");
            String knexSqlOfIdSelect = parseKnexInfo(knexCommandOfIdSelect).knexSql;
            ids = this.jdbc.queryForList(knexSqlOfIdSelect).stream().map(item -> Integer.valueOf(item.get("id").toString())).collect(Collectors.toList());
            this.backupNewDataListToRecordHistory(knexInfo.tableName, ids, knexInfo.operation);
        }

        int count = this.jdbc.update(knexInfo.knexSql);
        return count;
    }

    public KnexInfo parseKnexInfo(String knexStr) {
        return parseKnexInfo(knexStr, null);
    }

    public KnexInfo parseKnexInfo(String knexStr, JSONPathObject data) {
        long startTime = System.currentTimeMillis();
        KnexInfo knexInfo = new KnexInfo();

        knexStr = knexStr.trim();
        if (knexStr.endsWith(";")) {
            knexStr = knexStr.substring(0, knexStr.length() - 1);
        }
        if (!knexStr.startsWith("knex")) {
            knexInfo.knexStr = knexStr;
            knexInfo.knexSql = data != null ? JsonUtil.replaceString(knexStr, data) : knexStr;;
            // TODO: 非knex语句 jhId支持 / 做一个校验提醒 该sql 没有jhId条件
            return knexInfo;
        }

        String knexCommand = data != null ? JsonUtil.replaceString(knexStr, data) : knexStr;
        String tableName = ReUtil.get("knex\\((['\"])([^'\"]+)\\1", knexStr, 2);
        String operation = ReUtil.get("\\w+(?=\\([^)]*\\)$)", knexStr, 0);
        boolean jhIdEnable = jhIdConfig_enable && jhIdConfig_careTableViewList.contains(tableName);
        if (Arrays.asList("jhUpdate", "jhInsert", "jhDelete").contains(operation)) {
            knexCommand = knexCommand
                    .replace(".jhUpdate(", ".update(")
                    .replace(".jhInsert(", ".insert(")
                    .replace(".jhDelete(", ".delete(");
        }
        JSONPathObject operationData = JSONPathObject.of("operation", operation)
            .set("operationByUserId", ContextHolder.eval("userInfo.userId"))
            .set("operationByUser", ContextHolder.eval("userInfo.username"))
            .set("operationAt", DateUtil.date().toString(Constant.ISO8601));
        if (jhIdEnable) {
            knexCommand += String.format(".where('%s.jhId', '%s')", tableName, jhIdConfig_jhId);
        }

        knexInfo.knexStr = knexStr;
        knexInfo.knexCommand = knexCommand;
        knexInfo.tableName = tableName;
        knexInfo.operation = operation;
        knexInfo.data = data;

        try {
            if ("update".equals(operation) || "jhUpdate".equals(operation)){
                knexInfo.knexCommand += JsonUtil.replaceString(".update({ operation: ${operation}, operationByUserId: ${operationByUserId}, operationByUser: ${operationByUser}, operationAt: ${operationAt} })", operationData);
            }
            String knexSql = knexV8Runtime.getExecutor(knexInfo.knexCommand + ".toQuery();").executeString();
            knexInfo.knexSql = knexSql;
            if (Arrays.asList("update", "delete", "jhUpdate", "jhDelete").contains(operation)) {
                if (!knexSql.contains("where")) {
                    throw new BizException(BizEnum.resource_sql_where_options_missing);
                }
            }
            // Tip: 有些操作不需要打印日志
            if (!knexInfo.knexCommand.endsWith("select('id')")){
                log.debug("[JianghuKnex] [useTime: '{}/ms']\n - cmd: {} \n - sql: {}", System.currentTimeMillis() - startTime, knexInfo.knexCommand, knexInfo.knexSql);
            }

            return knexInfo;
        } catch (Exception e) {
            BizException bizException = new BizException(BizEnum.knex_execution_exception, e.getMessage());
            bizException.setStackTrace(e.getStackTrace());
            log.error("[JianghuKnex] knexInfo: {}", JsonUtil.toJSONString(knexInfo, JSONWriter.Feature.PrettyFormat));
            throw bizException;
        }
    }


    public void backupNewDataListToRecordHistory(String table, List<Integer> ids, String operation) {
        if (ids.isEmpty()) {
            return;
        }
        String packageContent = ContextHolder.eval(ContextHolder.REQUEST, "{}", String.class);
        String idsStr = ids.stream().map(Object::toString).collect(Collectors.joining(","));
        List<Map<String, Object>> newDataList = this.jdbc.queryForList("select * from `" + table + "` where id in (" + idsStr + ");");
        if (newDataList.isEmpty()) {
            return;
        }
        List<JSONPathObject> recordHistoryList = newDataList.stream().map(newData -> {
            newData.put("operation", operation);
            if (operation.equals("jhDelete")) {
                newData.put("operationByUserId", ContextHolder.eval("userInfo.userId"));
                newData.put("operationByUser", ContextHolder.eval("userInfo.username"));
                newData.put("operationAt", DateUtil.date().toString(Constant.ISO8601));
            }
            return JSONPathObject.of("table", table)
                    .set("recordId", newData.get("id"))
                    .set("recordContent", JsonUtil.toJSONString(newData))
                    .set("packageContent", packageContent)
                    .set("operation", newData.get("operation"))
                    .set("operationByUserId", newData.get("operationByUserId"))
                    .set("operationByUser", newData.get("operationByUser"))
                    .set("operationAt", newData.get("operationAt"));
        }).collect(Collectors.toList());
        if (jhIdConfig_enable) {
            recordHistoryList.forEach(item -> item.set("jhId", jhIdConfig_jhId));
        }
        Set<String> keySet = recordHistoryList.get(0).keySet();
        String keyListStr = recordHistoryList.get(0).keySet().stream().map(key -> "`" + key + "`").collect(Collectors.joining(","));
        String placeholderForValue = StrUtil.repeatAndJoin("?", keySet.size(), ", ");
        String insertRecordHistorySql = String.format("insert into _record_history (%s) values (%s);", keyListStr, placeholderForValue);
        List<Object[]> insertData = recordHistoryList.stream().map(item -> item.values().toArray()).collect(Collectors.toList());
        this.jdbc.batchUpdate(insertRecordHistorySql, insertData);
    }


    /**
     * 通过命令行执行 knexStr toSql
     */
    /*public String knexStrToSQLByCMD(String knexStr) {
        try {
            String nodeScriptTemp = "const Knex = require('knex'); const knex = Knex({client: 'mysql'}); console.log(__knexStr__)";
            String nodeScript = nodeScriptTemp.replace("__knexStr__", knexStr + ".toQuery()");
            // long startTime = System.currentTimeMillis();
            ProcessBuilder processBuilder = new ProcessBuilder("node", "-e", nodeScript);
            Process process = processBuilder.start();
            BufferedReader.evaler = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String sql = null;
            String line = null;
            while ((line =.evaler.evalLine()) != null) {
                sql = line;
            }
            // long endTime = System.currentTimeMillis();
            process.waitFor();
            // System.out.println("====>cmd knex toSQl: " + sql + (endTime - startTime) + "/毫秒");
            return sql;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }*/
    private void knexV8RuntimeInit() throws Exception{
        String jsFileName = "knex-browser.esm.js";
        ClassPathResource classPathResource = new ClassPathResource("node_modules/" + jsFileName);
        String fileContent = StreamUtils.copyToString(classPathResource.getInputStream(), StandardCharsets.UTF_8);
        V8Runtime v8Runtime = V8Host.getV8Instance().createV8Runtime();
        v8Runtime.setV8ModuleResolver((runtime, resourceName, v8ModuleReferrer) -> {
            if (jsFileName.equals(resourceName)) {
                return runtime.getExecutor(fileContent)
                        .setResourceName(resourceName).compileV8Module();
            } else {
                return null;
            }
        });
        v8Runtime.getExecutor(String.format("import Knex from '%s'; ", jsFileName) +
                        "const knex = new Knex({client: 'mysql', useNullAsDefault: true });" +
                        "globalThis.knex = knex;")
                .setModule(true)
                .setResourceName("./test.js")
                .executeVoid();
        this.knexV8Runtime = v8Runtime;
    }

    private void knexNodeRuntimeInit() throws Exception{
        ClassPathResource classPathResource = new ClassPathResource("node_modules");
        JavetEnginePool<NodeRuntime> javetEnginePool = new JavetEnginePool<NodeRuntime>();
        javetEnginePool.getConfig().setJSRuntimeType(JSRuntimeType.Node);
        IJavetEngine<NodeRuntime> iJavetEngine = javetEnginePool.getEngine();
        NodeRuntime nodeRuntime = iJavetEngine.getV8Runtime();
        File workingDirectory = new File(classPathResource.getPath());
        nodeRuntime.getNodeModule(NodeModuleModule.class).setRequireRootDirectory(workingDirectory);
        nodeRuntime.getExecutor(
                "const Knex = require('knex');\n" +
                        "const knex = new Knex({client: 'mysql', useNullAsDefault: true });\n" +
                        "globalThis.knex = knex;").executeVoid();
        this.knexNodeRuntime = nodeRuntime;
    }


}
