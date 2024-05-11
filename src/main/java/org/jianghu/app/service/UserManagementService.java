package org.jianghu.app.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import org.jianghu.app.common.BizEnum;
import org.jianghu.app.common.BizException;
import org.jianghu.app.common.JSONPathObject;
import org.jianghu.app.config.JianghuKnex;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service("userManagementServiceSystem")
public class UserManagementService {

    @Autowired
    private JianghuKnex jianghuKnex;

    public void addUser(JSONPathObject actionData) throws Exception {
        // 验证 actionData
        // validateUtil.validate(appDataSchema.addUser, actionData);

        String userId = actionData.eval("userId");
        String clearTextPassword = actionData.eval("clearTextPassword");

        String md5Salt = RandomUtil.randomString(12);
        String password = SecureUtil.md5(clearTextPassword + "_" + md5Salt);

        Long idSequence = getNextIdByTableAndField("_user", "idSequence");
        Integer userExistCount = jianghuKnex.count("knex('_user').where({ userId: ${userId}}).count('*', {as: 'count'})", actionData);

        if (userExistCount > 0) {
            throw new BizException(BizEnum.user_id_exist);
        }


        JSONPathObject insertParams = JSONPathObject.of()
                .set("username", actionData.eval("username"))
                .set("contactNumber", actionData.eval("contactNumber"))
                .set("gender", actionData.eval("gender"))
                .set("birthday", actionData.eval("birthday"))
                .set("signature", actionData.eval("signature"))
                .set("email", actionData.eval("email"))
                .set("userType", actionData.eval("userType"))
                .set("userStatus", actionData.eval("userStatus"))
                .set("userAvatar", actionData.eval("userAvatar"))
                .set("idSequence", idSequence)
                .set("userId", userId)
                .set("password", password)
                .set("clearTextPassword", clearTextPassword)
                .set("md5Salt", md5Salt);
        jianghuKnex.insert("knex('_user').insert()", insertParams);
    }

    public void resetUserPassword(JSONPathObject actionData) throws Exception {
        // 验证 actionData
        // validateUtil.validate(appDataSchema.initUserPassword, actionData);
        String userId = actionData.eval("userId");
        String clearTextPassword = actionData.eval("clearTextPassword");
        JSONPathObject countResult = jianghuKnex.first("knex('_user').where({ userId: ${userId}}).count('*', {as: 'count'})", actionData);
        Long userExistCount = countResult.eval("count", Long.class);

        if (userExistCount == 0) {
            throw new BizException(BizEnum.user_not_exist);
        }

        // hutool 随机 12 位字符串
        String md5Salt = RandomUtil.randomString(12);
        String password = SecureUtil.md5(clearTextPassword + "_" + md5Salt);
        jianghuKnex.update("knex('_user').where({ userId: ${userId} }).update({ password: ${password}, md5Salt: ${md5Salt}})",
                JSONPathObject.of().set("userId", userId).set("password", password).set("md5Salt", md5Salt));
    }


    private Long getNextIdByTableAndField(String table, String field) {

        JSONPathObject jsonPathObject = jianghuKnex.first("knex(${table}).max(${max})",
                JSONPathObject.of().set("table", table).set("max", field + " as maxValue"));

        Long maxValue = jsonPathObject.eval("maxValue", Long.class);
        if (maxValue == null || maxValue == 0) {
            maxValue = Long.valueOf(26260000);
        } else {
            maxValue += 1;
        }

        return maxValue;
    }

}
