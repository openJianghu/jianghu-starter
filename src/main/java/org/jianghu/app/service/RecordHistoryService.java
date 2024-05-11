package org.jianghu.app.service;

import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.common.JsonUtil;
import org.jianghu.app.config.JianghuKnex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service("recordHistoryServiceSystem")
public class RecordHistoryService {
    @Autowired
    private JianghuKnex jianghuKnex;

    public Map<String, Object> selectOnUseItemListByTable(JSONPathObject actionData) throws Exception {
        // 验证 actionData
        // validateUtil.validate(appDataSchema.selectUnDeleteItemListByTable, actionData);

        String table = actionData.eval("table");

        // 查询 table 中的所有记录，并按 operationAt 降序排序
        String selectSql = "knex('" + table + "').orderBy([{ column: 'operationAt', order: 'desc' }]).select()";
        List<JSONPathObject> rows = jianghuKnex.select(selectSql);

        // 提取所有记录的 ID

        List<Integer> recordIdList = rows.stream()
                .map(row -> row.eval("id", Integer.class))
                .collect(Collectors.toList());

        // 查询 _record_history 表中与记录 ID 相关的统计信息
        String recordHistorySql = "knex('_record_history').count('recordId as count').column('recordId', 'table').select().where({ table: ${table} }).whereIn('recordId', ${recordIdList}).groupBy('recordId')";
        recordHistorySql = recordHistorySql.replace("${table}", "'" + table +"'");
        recordHistorySql = recordHistorySql.replace("${recordIdList}", JsonUtil.toJSONString(recordIdList));
        List<JSONPathObject> recordIdCountList = jianghuKnex.select(recordHistorySql);

        // 将统计信息转换为 Map
        Map<String, Integer> recordIdCountMap = recordIdCountList.stream().collect(Collectors.toMap(recordIdCount -> recordIdCount.eval("recordId"), recordIdCount -> recordIdCount.eval("count")));

        // 更新 rows 中的每个记录，添加统计信息
        for (JSONPathObject row : rows) {
            Integer recordId = row.eval("id");
            row.put("recordHistoryId", null);
            row.put("count", recordIdCountMap.getOrDefault(recordId, 0));
        }

        // 构造返回值
        Map<String, Object> result = new HashMap<>();
        result.put("rows", rows);
        result.put("count", rows.size());
        return result;
    }
}
