package org.jianghu.app.config;

import org.jianghu.app.common.JSONPathObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class BeanConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JHConfig jhConfig;

    @Bean
    public JianghuKnex jianghuKnex() throws Exception {
        JSONPathObject jianghuConfig = jhConfig.eval("jianghuConfig.jhIdConfig", new JSONPathObject(), JSONPathObject.class);
        return new JianghuKnex(dataSource, jianghuConfig);
    }

}
