# AGENTS.md — plain-specification

## 作用
Specification 查询模式框架，把查询条件构建与存储后端解耦。

## 模块
- plain-specification-core：核心接口与表达式
- plain-specification-executor：执行器（MyBatis-Plus 3.5.10.1、Redis）
- plain-specification-spring-boot-starter：自动配置（`@EnableSpecification`）
- plain-specification-example：示例应用（ExampleApplication）

## 规则
- Java 8；测试 JUnit 5 + Mockito；`mvn -o test`。
- 下游 plain-platform 与模板工程直接使用，保持 API 向后兼容。
- 改核心 API 时同步更新示例与 README。
