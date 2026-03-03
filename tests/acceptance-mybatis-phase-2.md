# Acceptance: MyBatis Phase 2

## Given / When / Then 验收用例

### 用例 1：`#{}` 占位符解析成功
- Given
  - XML SQL 中包含多个 `#{}` 占位符
- When
  - 构建 `MappedStatement` 并生成 `BoundSql`
- Then
  - SQL 中的 `#{}` 被替换为 `?`
  - `ParameterMapping` 顺序与 SQL 占位符顺序一致

### 用例 2：单参数查询成功
- Given
  - Mapper 方法只接收 1 个简单参数
  - XML 使用 1 个 `#{}` 占位符
- When
  - 调用 Mapper 查询方法
- Then
  - `PreparedStatement` 正确绑定参数
  - 返回结果对象字段值正确

### 用例 3：JavaBean 参数查询成功
- Given
  - Mapper 方法接收 JavaBean 参数
  - XML 通过属性名引用 `#{id}`、`#{username}`
- When
  - 执行查询
- Then
  - `ParameterHandler` 能从 JavaBean 读取属性
  - 查询结果与条件匹配

### 用例 4：多参数查询成功
- Given
  - Mapper 方法接收多个离散参数
  - XML 通过 `#{param1}`、`#{param2}` 或真实参数名引用
- When
  - 执行查询
- Then
  - `MapperProxy` 将多参数封装为 Map
  - `ParameterHandler` 按顺序完成绑定

### 用例 5：下划线转驼峰映射成功
- Given
  - 查询列名为 `user_name`
  - `Configuration.mapUnderscoreToCamelCase = true`
- When
  - 执行结果映射
- Then
  - 目标对象的 `userName` 属性被正确赋值

## 正常路径

### 正常路径 1：简单类型首列映射
- Given
  - `resultType` 为简单类型
- When
  - 执行查询
- Then
  - 结果集首列被转换到目标简单类型

### 正常路径 2：JavaBean setter 映射
- Given
  - 目标对象存在可写 setter
- When
  - 执行查询
- Then
  - `ResultSetHandler` 优先通过 setter 填充字段

### 正常路径 3：字段直写回退映射
- Given
  - 目标对象没有 setter，但有同名字段
- When
  - 执行查询
- Then
  - `ResultSetHandler` 回退到字段写入

## 失败路径（SQL 不存在 / 参数不匹配 / 映射失败）

### 失败路径 1：参数数量不匹配
- Given
  - SQL 占位符数与传入参数模型不一致
- When
  - 执行查询
- Then
  - 抛出异常
  - 错误信息包含 `statementId`、SQL、参数摘要

### 失败路径 2：参数名不存在
- Given
  - XML 中引用了不存在的参数名
- When
  - 执行查询
- Then
  - 抛出异常
  - 错误信息包含缺失参数名与 `statementId`

### 失败路径 3：结果映射失败
- Given
  - `resultType` 与查询结果结构不兼容
- When
  - 执行查询
- Then
  - 抛出异常
  - 错误信息包含 `statementId`、`resultType`、SQL

### 失败路径 4：关闭下划线转驼峰后映射缺失
- Given
  - 列名与属性名不一致
  - `mapUnderscoreToCamelCase = false`
- When
  - 执行查询
- Then
  - 相关属性不被填充
  - 不抛异常，但结果对象字段为空

## 性能/资源释放策略验证说明
- 验证目标
  - 引入 `ParameterHandler` 和 `ResultSetHandler` 后不破坏资源释放语义
  - 每次查询仍然是单次连接、单次语句、单次结果集生命周期
- 验证方式
  - 继续使用 H2 内存数据库与可观测 JDBC 包装类
  - 在正常路径和参数绑定/映射失败路径分别断言 `close` 次数
  - 在多参数查询场景验证绑定顺序与占位符顺序一致
- 边界
  - 不验证批量执行
  - 不验证缓存
  - 不验证事务连接复用
