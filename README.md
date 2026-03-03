# mini-mybatis

`mini-mybatis` 是一个按阶段演进的轻量 ORM 学习项目，用最小实现方式拆出 MyBatis 的核心执行链，并保证每个阶段都可编译、可运行、可验证。

当前核心链路已经覆盖：

```text
SqlSession -> MapperProxy -> Executor -> StatementHandler -> JDBC -> ResultSetHandler
```

## 当前状态

| Phase | 状态 | 已交付能力 |
| --- | --- | --- |
| Phase 1 | 已完成 | XML 映射加载、`SqlSession`、`MapperProxy`、基础查询闭环 |
| Phase 2 | 已完成 | `#{}` 参数绑定、`BoundSql`、`ParameterHandler`、`ResultSetHandler`、下划线转驼峰 |
| Phase 3 | 已完成 | `ExecutorType`、`ReuseExecutor`、`RoutingStatementHandler`、单会话 `PreparedStatement` 复用 |

## 当前不做

- 完整动态 SQL
- 显式 `ResultMap`
- 查询结果缓存
- 插件体系
- 事务管理
- `mini-spring` 集成代码

`mini-spring` 集成目前只有设计文档，没有实现代码。

## 仓库结构

```text
mini-mybatis
├── docs
├── tests
├── examples
├── src/main/java/com/xujn/minimybatis
├── src/main/resources
├── src/test/java
└── src/test/resources
```

关键目录说明：

- [docs](/Users/xjn/Develop/projects/java/mini-mybatis/docs)：架构蓝图、分阶段设计文档、集成设计文档
- [tests](/Users/xjn/Develop/projects/java/mini-mybatis/tests)：阶段验收策略文档
- [examples](/Users/xjn/Develop/projects/java/mini-mybatis/examples)：每个阶段的可运行样例
- [src/main/java/com/xujn/minimybatis](/Users/xjn/Develop/projects/java/mini-mybatis/src/main/java/com/xujn/minimybatis)：框架源码
- [src/test/java](/Users/xjn/Develop/projects/java/mini-mybatis/src/test/java)：JUnit 验证代码

## 文档索引

- 架构蓝图：[docs/architecture-mybatis.md](/Users/xjn/Develop/projects/java/mini-mybatis/docs/architecture-mybatis.md)
- Phase 1 设计：[docs/mybatis-phase-1.md](/Users/xjn/Develop/projects/java/mini-mybatis/docs/mybatis-phase-1.md)
- Phase 2 设计：[docs/mybatis-phase-2.md](/Users/xjn/Develop/projects/java/mini-mybatis/docs/mybatis-phase-2.md)
- Phase 3 设计：[docs/mybatis-phase-3.md](/Users/xjn/Develop/projects/java/mini-mybatis/docs/mybatis-phase-3.md)
- 集成蓝图：[docs/integration-mybatis-spring.md](/Users/xjn/Develop/projects/java/mini-mybatis/docs/integration-mybatis-spring.md)

## 技术栈

- Java 17
- Maven
- H2 In-Memory Database
- JUnit 5

## 快速开始

### 1. 运行测试

离线环境：

```bash
mvn -o -Dmaven.repo.local=/Users/xjn/.m2/repository test
```

在线环境：

```bash
mvn test
```

### 2. 运行阶段样例

建议串行运行，不要并发执行多个 example。它们都使用 H2 内存数据库，串行执行更稳定。

Phase 1：

```bash
mvn -o -Dmaven.repo.local=/Users/xjn/.m2/repository -q -DskipTests exec:java -Dexec.mainClass=com.xujn.minimybatis.examples.phase1.Phase1QueryExample
```

预期输出：

```text
selectById -> User{id=1, username='alice', email='alice@example.com'}
selectAll size -> 3
```

Phase 2：

```bash
mvn -o -Dmaven.repo.local=/Users/xjn/.m2/repository -q -DskipTests exec:java -Dexec.mainClass=com.xujn.minimybatis.examples.phase2.Phase2BindingExample
```

预期输出：

```text
phase2 selectById -> Phase2User{id=1, userName='alice', email='alice@example.com'}
phase2 multiParam size -> 1
phase2 beanParam size -> 1
```

Phase 3：

```bash
mvn -o -Dmaven.repo.local=/Users/xjn/.m2/repository -q -DskipTests exec:java -Dexec.mainClass=com.xujn.minimybatis.examples.phase3.Phase3ReuseExecutorExample
```

预期输出：

```text
phase3 executorType -> REUSE
phase3 first query -> Phase3User{id=1, username='alice', email='alice@example.com'}
phase3 second query -> Phase3User{id=1, username='alice', email='alice@example.com'}
phase3 selectAll size -> 3
```

## 当前能力边界

### XML 映射

- 支持 `<mapper namespace="...">`
- 支持 `<select ...>` 查询语句
- 支持 `#{}` 占位符
- 不支持 `${}`
- 不支持完整动态 SQL 节点

### 参数绑定

- 支持单参数
- 支持 JavaBean 参数
- 支持多参数 Mapper 方法
- 支持真实参数名和 `param1/param2` 访问

### 结果映射

- 支持简单类型首列映射
- 支持 JavaBean setter 映射
- 支持字段直写回退
- 支持 `mapUnderscoreToCamelCase`
- 不支持显式 `ResultMap`
- 不支持嵌套对象映射

### 执行器

- `ExecutorType.SIMPLE`
  - 每次查询独立创建并关闭 `Connection`、`PreparedStatement`、`ResultSet`
- `ExecutorType.REUSE`
  - 单个 `SqlSession` 生命周期内复用相同 SQL 的 `PreparedStatement`
  - `ResultSet` 仍按单次查询关闭
  - `SqlSession.close()` 时统一释放缓存语句和连接
  - 查询失败后会淘汰对应缓存语句，避免坏语句继续复用

## 代码入口

- 配置中心：[Configuration.java](/Users/xjn/Develop/projects/java/mini-mybatis/src/main/java/com/xujn/minimybatis/session/Configuration.java)
- 会话工厂：[DefaultSqlSessionFactory.java](/Users/xjn/Develop/projects/java/mini-mybatis/src/main/java/com/xujn/minimybatis/session/defaults/DefaultSqlSessionFactory.java)
- 默认会话：[DefaultSqlSession.java](/Users/xjn/Develop/projects/java/mini-mybatis/src/main/java/com/xujn/minimybatis/session/defaults/DefaultSqlSession.java)
- Mapper 代理：[MapperProxy.java](/Users/xjn/Develop/projects/java/mini-mybatis/src/main/java/com/xujn/minimybatis/binding/MapperProxy.java)
- 简单执行器：[SimpleExecutor.java](/Users/xjn/Develop/projects/java/mini-mybatis/src/main/java/com/xujn/minimybatis/executor/SimpleExecutor.java)
- 复用执行器：[ReuseExecutor.java](/Users/xjn/Develop/projects/java/mini-mybatis/src/main/java/com/xujn/minimybatis/executor/ReuseExecutor.java)
- 语句路由：[RoutingStatementHandler.java](/Users/xjn/Develop/projects/java/mini-mybatis/src/main/java/com/xujn/minimybatis/executor/statement/RoutingStatementHandler.java)

## 测试入口

- Phase 1：[Phase1QueryTest.java](/Users/xjn/Develop/projects/java/mini-mybatis/src/test/java/com/xujn/minimybatis/Phase1QueryTest.java)
- Phase 2：[Phase2BindingMappingTest.java](/Users/xjn/Develop/projects/java/mini-mybatis/src/test/java/com/xujn/minimybatis/Phase2BindingMappingTest.java)
- Phase 3：[Phase3ReuseExecutorTest.java](/Users/xjn/Develop/projects/java/mini-mybatis/src/test/java/com/xujn/minimybatis/Phase3ReuseExecutorTest.java)

## 当前验证结果

当前分支已验证通过：

- 全量测试：`28` 个测试通过
- Phase 1 example 可运行
- Phase 2 example 可运行
- Phase 3 example 可运行
