# Acceptance: MyBatis Phase 3

## Given / When / Then 验收用例

### 用例 1：默认执行器保持兼容
- Given
  - `Configuration.defaultExecutorType = SIMPLE`
  - Phase 1 和 Phase 2 的 Mapper、XML、参数绑定逻辑不变
- When
  - 执行现有查询用例
- Then
  - 结果与 Phase 2 保持一致
  - 不需要修改 Mapper 接口和 `SqlSession` 调用方式

### 用例 2：同会话同 SQL 触发 Statement 复用
- Given
  - `Configuration.defaultExecutorType = REUSE`
  - 同一 `SqlSession` 内连续执行相同 `statementId`
- When
  - 两次执行查询
- Then
  - `PreparedStatement` 只创建 1 次
  - 两次查询结果都正确

### 用例 3：同会话不同 SQL 不共享 Statement
- Given
  - `Configuration.defaultExecutorType = REUSE`
  - 同一 `SqlSession` 内执行两个不同 SQL
- When
  - 依次执行查询
- Then
  - 生成两个独立的 `PreparedStatement`
  - 两条查询链路互不污染

### 用例 4：不同会话不共享 Statement 缓存
- Given
  - `Configuration.defaultExecutorType = REUSE`
  - 打开两个不同 `SqlSession`
- When
  - 在两个会话中执行相同 SQL
- Then
  - 两个会话各自创建独立 `PreparedStatement`
  - 缓存边界严格限定在单会话内

## 正常路径

### 正常路径 1：ReuseExecutor 命中缓存
- Given
  - 同一会话重复执行相同 SQL
- When
  - 第二次执行查询
- Then
  - 从缓存取得已有 `PreparedStatement`
  - 只重新绑定参数和执行查询

### 正常路径 2：ResultSet 仍按单次查询关闭
- Given
  - 使用 `ReuseExecutor`
- When
  - 查询完成
- Then
  - `ResultSet` 在每次查询结束后立即关闭
  - `PreparedStatement` 保留到会话关闭

### 正常路径 3：会话关闭统一释放语句
- Given
  - `ReuseExecutor` 已缓存多个 `PreparedStatement`
- When
  - 调用 `SqlSession.close()`
- Then
  - 所有缓存 `PreparedStatement` 被关闭
  - 缓存容器被清空

## 失败路径（执行器复用 / 关闭失败 / 资源释放）

### 失败路径 1：缓存语句失效
- Given
  - 缓存中的 `PreparedStatement` 已失效或已关闭
- When
  - 再次执行相同 SQL
- Then
  - 执行器检测到失效语句
  - 移除旧缓存并重新创建语句
  - 错误信息或日志上下文包含 `statementId`、SQL、执行器类型

### 失败路径 2：查询执行异常
- Given
  - `ReuseExecutor` 命中缓存语句
  - 查询执行时 JDBC 抛出异常
- When
  - 执行查询
- Then
  - 当前 `ResultSet` 被关闭
  - 会话关闭时缓存语句仍被完整释放
  - 异常消息包含 `statementId`、SQL、参数摘要

### 失败路径 3：关闭缓存语句失败
- Given
  - `ReuseExecutor.close()` 过程中某个 `PreparedStatement.close()` 抛出异常
- When
  - 关闭 `SqlSession`
- Then
  - 执行器继续尝试关闭剩余语句
  - 最终抛出聚合后的关闭异常
  - 异常消息包含失败语句的 SQL 和会话上下文

## 性能/资源释放策略验证说明
- 验证目标
  - 验证 `ReuseExecutor` 能减少同会话重复 SQL 的 `PreparedStatement` 创建次数
  - 验证引入复用后不破坏 ResultSet 的即时关闭语义
  - 验证会话关闭时 Statement 缓存完整释放
- 验证方式
  - 使用可观测 JDBC 包装类分别记录 `prepareStatement` 次数、`ResultSet.close()` 次数、`PreparedStatement.close()` 次数
  - 在 `SIMPLE` 与 `REUSE` 模式下执行同一组查询，对比语句创建次数
  - 在异常路径下断言 `PreparedStatement` 没有泄漏到会话结束之后
- 边界
  - 不验证跨事务连接复用
  - 不验证缓存命中统计接口
  - 不验证动态 SQL
