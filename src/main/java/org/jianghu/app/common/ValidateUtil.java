package org.jianghu.app.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ValidateUtil {

    public static final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    public static final ObjectMapper objectMapper = new ObjectMapper();

    public static void validate(String schema, JSONPathObject jsonData, String errorTip) {
        if (StringUtils.isEmpty(schema) || !JSON.isValid(schema, JSONReader.Feature.AllowUnQuotedFieldNames)) {
            throw new BizException(BizEnum.valid_schema_invalid);
        }
        JSONPathObject schemaJSON = JsonUtil.parseObject(schema, JSONReader.Feature.AllowUnQuotedFieldNames);
        validate(schemaJSON, jsonData, errorTip);
    }

    public static void validate(JSONPathObject schema, JSONPathObject jsonData, String errorTip) {
        if (jsonData == null) { jsonData = JSONPathObject.of(); }
        Set<ValidationMessage> validateMessageList = schemaFactory.getSchema(schema.toJSONString()).validate(jsonData.toJSONString(), InputFormat.JSON);
        if (!validateMessageList.isEmpty()) {
            String errorReasonSupplement = validateMessageList.stream().map(ValidationMessage::getMessage).collect(Collectors.joining("; "));
            String errorCode = BizEnum.request_body_invalid.getErrorCode();
            String errorReason = errorTip;
            if (StringUtils.isEmpty(errorReason)) {
                errorReason = BizEnum.request_body_invalid.getErrorReason();
            }
            throw new BizException(errorCode, errorReason, errorReasonSupplement);
        }
    }

    private static String translateErrorMessage(ValidationMessage message) {
        String keyword = message.getMessageKey();
        String string = message.getInstanceLocation().toString();
        switch (keyword) {
            case "required":
                return "缺少必需的属性";
            case "type":
                return "属性类型不匹配";
            case "minimum":
                return "数值低于最小限制";
            case "maximum":
                return "数值高于最大限制";
            // 添加更多关键字的映射
            default:
                return "未知错误";
        }
    }

}
