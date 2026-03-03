# Acceptance: MyBatis Phase 1

## Given / When / Then 验收用例

### 用例 1：XML 映射加载成功
- Given
  - 存在合法 mapper XML
  - XML 包含唯一 `namespace` 与 `select id`
- When
  - 构建 `Configuration` 并执行 XML 解析
- Then
  - `Configuration` 中注册对应 `MappedStatement`
  - `statementId` 为 `namespace + "." + id`

### 用例 2：Mapper 接口查询单对象成功
- Given
  - `SqlSessionFactory` 已创建
  - `MapperRegistry` 已注册 Mapper 接口
  - H2 内存数据库中存在测试数据
- When
  - 通过 `SqlSession.getMapper(UserMapper.class)` 调用单对象查询方法
- Then
  - 返回目标对象
  - 返回值字段与查询结果一致

### 用例 3：SqlSession 查询列表成功
- Given
  - `SqlSession` 已打开
  - 对应 `statementId` 已注册
- When
  - 调用 `SqlSession.selectList(statementId)`
- Then
  - 返回非空集合
  - 集合元素类型与 `resultType` 一致

## 正常路径

### 正常路径 1：MapperProxy 路由成功
- Given
  - Mapper 方法名与 XML `id` 一致
- When
  - 调用 Mapper 方法
- Then
  - `MapperProxy` 生成正确 `statementId`
  - `DefaultSqlSession` 成功查找到 `MappedStatement`

### 正常路径 2：简单 JavaBean 映射成功
- Given
  - 查询列名与 JavaBean 属性名一致
- When
  - 执行查询
- Then
  - 返回对象被正确填充

### 正常路径 3：资源释放完整
- Given
  - 一次查询正常完成
- When
  - 查询返回后检查 JDBC 资源
- Then
  - `Connection`、`PreparedStatement`、`ResultSet` 都已关闭

## 失败路径（SQL 不存在 / 参数不匹配 / 映射失败）

### 失败路径 1：SQL 不存在
- Given
  - 调用的 `statementId` 未注册
- When
  - 调用 `SqlSession.selectOne` 或 Mapper 方法
- Then
  - 抛出异常
  - 错误信息包含 `statementId`

### 失败路径 2：重复 statementId
- Given
  - 两个 XML 注册相同 `statementId`
- When
  - 初始化 `Configuration`
- Then
  - 抛出异常
  - 错误信息包含重复 `statementId` 与资源路径

### 失败路径 3：映射失败
- Given
  - `resultType` 与查询列结构不匹配
- When
  - 执行查询
- Then
  - 抛出异常
  - 错误信息包含 `statementId`、`resultType`、SQL

### 失败路径 4：参数不匹配
- Given
  - Phase 1 只支持无参数或单参数透传
  - 调用方式超出当前参数模型
- When
  - 执行对应查询
- Then
  - 抛出异常
  - 错误信息包含 `statementId` 与参数对象摘要

## 性能/资源释放策略验证说明
- 验证目标
  - 无缓存场景下每次查询直连数据库
  - 每次查询结束后资源完整释放
- 验证方式
  - 使用 H2 内存数据库运行示例
  - 使用可观测 JDBC 包装类记录 `close` 调用次数
  - 在正常路径和异常路径分别断言资源关闭
- 边界
  - 不验证连接池复用
  - 不验证事务连接复用
  - 不验证批量执行
