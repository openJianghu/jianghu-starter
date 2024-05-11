package org.jianghu.app.controller.controllerUtil;

import cn.hutool.core.util.ReflectUtil;
import org.jianghu.app.common.*;
import org.jianghu.app.config.JianghuKnex;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ResourceUtil {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private JianghuKnex jianghuKnex;

    public JSONPathObject sqlResource(JSONPathObject requestBody, JSONPathObject resource) throws Exception {
        // requestBody.appData
        String pageId = requestBody.eval("appData.pageId");
        String actionId = requestBody.eval("appData.actionId");
        Integer limit = requestBody.eval("appData.limit");

        // resource
        if (resource == null) {
            String resourceSelectSql = String.format("knex('_resource').where({ pageId: '%s', actionId: '%s', }).first()", pageId, actionId);
            resource = jianghuKnex.first(resourceSelectSql);
        }
        JSONPathObject resourceData = resource.eval("resourceData", JSONPathObject.class);
        JSONPathObject appData = requestBody.eval("appData", JSONPathObject.class);
        JSONPathObject actionData = requestBody.eval("appData.actionData", JSONPathObject.class);

        // 校验数据
        validate(requestBody, resource);

        // resourceData
        String table = resourceData.eval("table");
        String operation = resourceData.eval("operation", String.class);
        String rawSql = resourceData.eval("rawSql");
        List<String> excludedFieldList = resourceData.eval("excludedFieldList");
        List<String> fieldList = resourceData.eval("fieldList");

        // rawSql
        if (StringUtils.isNotEmpty(rawSql)) {
            List<JSONPathObject> rows = jianghuKnex.select(rawSql);
            return JSONPathObject.of("rows", rows);
        }

        // fieldList
        if (CollectionUtils.isEmpty(fieldList) && !CollectionUtils.isEmpty(excludedFieldList)) {
            List<String> fieldListOfTable = jianghuKnex.jdbc.query(String.format("desc %s", table), (rs, rowNum) -> rs.getString("Field"));
            fieldList = fieldListOfTable.stream().filter(field -> !excludedFieldList.contains(field)).collect(Collectors.toList());
        }

        // 1. where 构建：前后端合并
        // 前端部分，来自前端传过来的 actionData，不支持部分参数
        appData.remove("whereKnex");
        appData.remove("fieldList");
        appData.remove("excludedFieldList");
        appData.remove("rawSql");
        // ==========frontendWhereData by resourceData
        String frontendWhere = buildWhereCondition(appData);
        // ==========backendWhereData by appData
        String backendWhere = buildWhereCondition(resourceData);

        String whereCondition = backendWhere + frontendWhere;
        String knexArgs = "";
        if (operation.equals("insert") || operation.equals("update") || operation.equals("jhInsert") || operation.equals("jhUpdate")) {
            knexArgs = JsonUtil.toJSONString(actionData);
        }
        if (operation.equals("select") && !CollectionUtils.isEmpty(fieldList)) {
            knexArgs = fieldList.stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
        }
        if ((operation.equals("delete") || operation.equals("update") || operation.equals("jhDelete") || operation.equals("jhUpdate")) && StringUtils.isEmpty(whereCondition)) {
            throw new RuntimeException("where是必需的!");
        }

        // 2. 翻页场景需要 count 计算
        JSONPathObject resultData = JSONPathObject.of();
        if (limit != null) {
            String knexCommandCountString = String.format("knex('%s')%s.count('*')", table, whereCondition);
            // 去掉 limit, offset, orderBy
            knexCommandCountString = knexCommandCountString
                    .replaceAll("\\.limit\\([^\\)]+\\)", "")
                    .replaceAll("\\.offset\\([^\\)]+\\)", "")
                    .replaceAll("\\.orderBy\\([^\\)]+\\)", "");
            Integer count = jianghuKnex.count(knexCommandCountString);
            resultData.set("count", count);
        }

        // 3. jianghuKnex 执行
        String knexCommandTemplate = "knex(${table})${whereCondition}.${operation}(${knexArgs})";
        String knexCommandString = knexCommandTemplate
                .replace("${table}", "'" + table + "'")
                .replace("${whereCondition}", whereCondition)
                .replace("${operation}", operation)
                .replace("${knexArgs}", knexArgs);
        // logger.debug("[resourceSql] {}.{}: {}", pageId, actionId, knexCommandString);
        List<JSONPathObject> rows = jianghuKnex.execute(knexCommandString);
        resultData.set("rows", rows);
        return resultData;
    }


    public Object serviceResource(JSONPathObject requestBody, JSONPathObject resource) throws Exception {
        JSONPathObject actionData = requestBody.eval("appData.actionData", new JSONPathObject(), JSONPathObject.class);
        JSONPathObject resourceData = resource.eval("resourceData", JSONPathObject.class);

        String serviceFunction = resourceData.eval("serviceFunction");
        Object service = getServiceBean(resourceData.eval("service", String.class));
        Method method = ReflectUtil.getMethod(service.getClass(), serviceFunction, JSONPathObject.class);
        if (method == null) { throw new BizException(BizEnum.resource_service_method_not_found); }
        try {
            Object resultData = method.invoke(service, actionData);
            return resultData;
        } catch(InvocationTargetException e){
            Throwable targetException = e.getTargetException();
            if (targetException instanceof BizException) {
                throw (BizException) targetException;
            } else {
                throw new RuntimeException(targetException);
            }
        }
    }

    public Object getServiceBean(String serviceName) {
        String serviceNameApp = serviceName+ "Service";
        String serviceNameSystem = serviceName + "ServiceSystem";

        Object service = null;
        if (applicationContext.containsBean(serviceNameApp)) {
            service = applicationContext.getBean(serviceNameApp);
        } else if (applicationContext.containsBean(serviceNameSystem)) {
            service = applicationContext.getBean(serviceNameSystem);
        }

        if (service == null) {
            throw new BizException(BizEnum.resource_service_not_found);
        }
        return service;
    }

    // =================================================内部方法===============================================================
    private void validate(JSONPathObject body, JSONPathObject resource) {
        JSONPathObject resourceData = resource.eval("resourceData", JSONPathObject.class);
        JSONPathObject appDataSchema = resource.eval("appDataSchema", JSONPathObject.class);

        String operation = resourceData.eval("operation");
        JSONPathObject appData = body.eval("appData", new JSONPathObject(), JSONPathObject.class);
        JSONPathObject actionData = body.eval("actionData", new JSONPathObject(), JSONPathObject.class);

        List<String> validOperations = Arrays.asList("select", "insert", "update", "delete", "jhInsert", "jhUpdate", "jhDelete");
        if (!validOperations.contains(operation)) {
            throw new BizException(BizEnum.resource_sql_operation_invalid);
        }

        // 如果是更新或删除，需要指定条件
        List<String> conditionRequiredOperations = Arrays.asList("update", "delete", "jhUpdate", "jhDelete");
        if (conditionRequiredOperations.contains(operation)) {
            boolean hasCondition = false;
            List<String> conditionKeys = Arrays.asList("where", "whereLike", "whereOrOption", "whereOption", "whereIn", "whereKnex", "rawSql");
            for (String conditionKey : conditionKeys) {
                if (resourceData.get(conditionKey) != null || (appData != null && appData.get(conditionKey) != null)) {
                    hasCondition = true;
                    break;
                }
            }
            if (!hasCondition) {
                throw new BizException(BizEnum.resource_sql_need_condition);
            }
        }

        if (appDataSchema != null && !appDataSchema.isEmpty()) {
            ValidateUtil.validate(appDataSchema, appData, "appData");
        }

        // 创建 or 更新时不能指定 主键id ===> 避免无操作
        if (actionData != null) {
            actionData.remove("id");
        }
    }

    private String buildWhereCondition(JSONPathObject appData) {
        // where
        String wherePart = "";
        Map where = appData.eval("where", Map.class);
        if (where != null) {
            wherePart = String.format(".where(%s)", JsonUtil.toJSONString(where));
        }

        // whereLike
        String whereLikePart = "";
        Map<String, String> whereLike = appData.eval("whereLike", Map.class);
        if (whereLike != null) {
            for (Map.Entry<String, String> entry : whereLike.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                whereLikePart += String.format(".where('%s', 'like', '%%%s%%')", key, value);
            }
        }

        // whereIn
        String whereInPart = "";
        Map<String, List<Object>> whereIn = appData.eval("whereIn", Map.class);
        if (whereIn != null) {
            for (Map.Entry<String, List<Object>> entry : whereIn.entrySet()) {
                String key = entry.getKey();
                List<Object> value = entry.getValue();
                whereInPart += String.format(".whereIn('%s', %s)", key, JsonUtil.toJSONString(value));
            }
        }

        // whereOptions
        String whereOptionsPart = "";
        List<List<Object>> whereOptions = appData.eval("whereOptions", List.class);
        if (whereOptions != null) {
            for (List<Object> whereOption : whereOptions) {
                int optionLength = whereOption.size();
                if (optionLength == 3) {
                    whereOptionsPart += String.format(".where('%s', '%s', '%s')", whereOption.get(0), whereOption.get(1), whereOption.get(2));
                } else if (optionLength == 2) {
                    whereOptionsPart += String.format(".where('%s', '%s')", whereOption.get(0), whereOption.get(1));
                } else {
                    throw new IllegalArgumentException("Invalid where options");
                }
            }
        }

        // whereOrOptions
        String whereOrOptionsPart = "";
        List<List<Object>> whereOrOptions = appData.eval("whereOrOptions", List.class);
        if (whereOrOptions != null) {
            whereOrOptionsPart += ".where(function() { this";
            for (List<Object> whereOrOption : whereOrOptions) {
                int optionLength = whereOrOption.size();
                if (optionLength == 3) {
                    whereOrOptionsPart += String.format(".orWhere('%s', '%s', '%s')", whereOrOption.get(0), whereOrOption.get(1), whereOrOption.get(2));
                } else if (optionLength == 2) {
                    whereOrOptionsPart += String.format(".orWhere('%s', '%s')", whereOrOption.get(0), whereOrOption.get(1));
                } else {
                    throw new IllegalArgumentException("Invalid where options");
                }
            }
            whereOrOptionsPart += "})";
        }

        // limit offset
        String limitAndOffset = "";
        Integer limit = appData.eval("limit", Integer.class);
        if (limit != null) {
            limitAndOffset += String.format(".limit(%d)", limit);
        }
        Integer offset = appData.eval("offset", Integer.class);
        if (offset != null) {
            limitAndOffset += String.format(".offset(%d)", offset);
        }

        // orderBy
        String orderByPart = "";
        List<Map<String, String>> orderBy = appData.eval("orderBy", List.class);
        if (orderBy != null) {
            orderByPart = String.format(".orderBy(%s)", JsonUtil.toJSONString(orderBy));
        }

        // whereKnex
        String whereKnex = appData.eval("whereKnex", String.class);
        if (whereKnex == null) {
            whereKnex = "";
        }

        String whereCondition = wherePart +
                whereLikePart +
                whereInPart +
                whereOptionsPart +
                whereOrOptionsPart +
                whereKnex +
                orderByPart +
                limitAndOffset;

        return whereCondition;
    }

}

