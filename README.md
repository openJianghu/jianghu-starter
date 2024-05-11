## 链接

- [插件: AI Coding](https://tongyi.aliyun.com/lingma/download)
- [插件: 日志高亮](https://plugins.jetbrains.com/plugin/7125-grep-console)
- [hutool工具库](https://doc.hutool.cn/pages/index/)
- [参数校验]
  - [json-schema](https://cswr.github.io/JsonSchema)
  - hutool工具库.Validator
- [knex-querylab在线](https://michaelavila.com/knex-querylab/?query=Q)

## 项目命令

- 打包jar: ./mvnw clean install
- 运行jar: java -Dspring.profiles.active=prod -jar jianghuJava-0.0.1.jar

## Jianghu Java TODO

- 全局异常                       @Colin ====》JHResult
- 中间件 鉴权                     @汗蒸
- service/user.js                @汗蒸     
- service/file.js(优化下)         @Colin
- static & upload  缓存配置（优化） @汗蒸
- baisc  demo 所有页面的测试        @Colin
- -------------------------------------------------------------
- com.fsll.jianghu.app ==>       org.jianghu.app
- jianghu-starter 打包            ==最后==

## Jianghu JAVA TODO

- pageHook 还差 塞数据到 ctxForRender （支持服务端渲染 取数据） 
- knex安全:
  - sql注入
  - update & delete ===》校验是否有where
  - jhUpdate&jhDelete ====> 
    - knexStr       recordHistory     operation数据填充  事物支持
    - knexBuilder
- 参数校验
- 代码优化 ===> 风格一致
- 框架发包 ----》框架的继承&覆盖特性

> IDEA 配置 VIEW 代码改动后 自动build

