# Acceptance: Integration Phase 1

## Given / When / Then 验收用例

### 用例 1：Mapper 扫描注册成功
- Given
  - mini-spring 容器已注册 `MapperScannerConfigurer`
  - mini-spring 已支持在 `refresh` 中执行自定义 `BeanFactoryPostProcessor`
  - `basePackages` 指向包含多个 Mapper 接口的包
  - `SqlSessionFactoryBean` 与 `DataSource` BeanDefinition 已存在
- When
  - 执行容器 `refresh`
- Then
  - 每个 Mapper 接口都生成一个工厂方法型 `BeanDefinition`
  - `MapperDefinitionRegistry` 中存在 `beanName -> mapperInterface` 映射
  - 每个 Mapper BeanDefinition 都携带传给 `MapperFactory#getMapper(beanName)` 的字面量参数
  - 未把普通 class 或抽象类注册为 Mapper Bean

### 用例 2：SqlSessionFactory 启动期初始化成功
- Given
  - `DataSource` Bean 可解析
  - `mapperLocations` 指向有效 XML 资源
- When
  - `SqlSessionFactoryBean` 在单例预实例化阶段创建产品对象
- Then
  - `Configuration` 完成初始化
  - 所有 XML 中的 `MappedStatement` 被注册到 `Configuration`
  - 所有 XML 对应的 Mapper 接口被注册到 `MapperRegistry`

### 用例 3：Mapper Bean 可从容器获取并调用
- Given
  - 容器 `refresh` 已完成
  - Mapper 接口与 XML `namespace` 一致
- When
  - 调用 `getBean(userMapper)` 并执行一个基础查询方法
- Then
  - 返回对象为 Mapper 代理 Bean
  - 代理调用通过 `MapperFactory -> SqlSessionTemplate -> SqlSessionFactory -> SqlSession -> Executor` 完成执行
  - 查询结果与 `MappedStatement` 对应返回类型一致

## 正常路径

### 正常路径 1：单 Mapper 单 XML 资源
- Given
  - 一个 Mapper 接口
  - 一个对应 XML 文件
  - 一个 `DataSource`
- When
  - 容器执行 `refresh`
- Then
  - 启动成功
  - `SqlSessionFactory`、`SqlSessionTemplate`、Mapper Bean 可获取
  - Mapper 方法能返回单对象结果

### 正常路径 2：多 Mapper 多 XML 资源
- Given
  - 多个 Mapper 接口分布在同一 `basePackages`
  - 多个 XML 文件均可读取
- When
  - 容器执行 `refresh`
- Then
  - 所有 Mapper BeanDefinition 均注册成功
  - 所有 `statementId` 均进入 `Configuration`
  - 任一 Mapper Bean 获取后均可正常路由到对应 `statementId`

### 正常路径 3：工厂方法单例语义生效
- Given
  - 某 Mapper Bean 使用工厂方法型 `BeanDefinition`
  - 容器对该 BeanDefinition 采用单例缓存
- When
  - 连续两次调用 `getBean` 获取同名 Mapper Bean
- Then
  - 两次返回同一代理实例
  - 不重复创建新的 `MapperProxy`

## 失败路径（缺失映射/冲突/资源加载失败/DataSource 缺失）

### 失败路径 1：DataSource 缺失
- Given
  - `SqlSessionFactoryBean` 已注册
  - 容器中不存在被引用的 `DataSource` Bean
- When
  - 容器执行 `refresh`
- Then
  - 抛出 `MissingDataSourceException`
  - 错误信息包含 `SqlSessionFactoryBean` 的 beanName 和缺失依赖引用名
  - 容器启动终止

### 失败路径 2：XML 资源加载失败
- Given
  - `mapperLocations` 中包含不存在或不可读的资源路径
- When
  - `SqlSessionFactoryBean` 执行 XML 加载
- Then
  - 抛出 `MappingLoadException`
  - 错误信息包含资源路径
  - 不注册任何半完成的 `MappedStatement`

### 失败路径 3：重复 statementId 冲突
- Given
  - 两个 XML 文件注册相同 `statementId`
  - 冲突策略为 `FAIL_FAST`
- When
  - `SqlSessionFactoryBean` 初始化 `Configuration`
- Then
  - 抛出 `StatementConflictException`
  - 错误信息包含重复的 `statementId`、旧资源路径、新资源路径
  - 容器启动终止

### 失败路径 4：重复 Mapper 接口注册
- Given
  - 同一 Mapper 接口因重复扫描或重复配置进入注册流程
- When
  - `MapperScannerConfigurer` 注册 BeanDefinition
- Then
  - 抛出 `MapperRegistrationException`
  - 错误信息包含 `mapperClass` 与扫描来源包
  - 不覆盖旧定义

### 失败路径 5：缺失映射语句
- Given
  - Mapper Bean 已创建
  - XML 中缺少对应方法的 `statementId`
- When
  - 调用该 Mapper 方法
- Then
  - 抛出 `MapperBindingException`
  - 错误信息包含 `mapperClass`、`methodName`、`statementId`

### 失败路径 6：mini-spring 未执行自定义 BeanFactoryPostProcessor
- Given
  - 容器只执行内置 `ConfigurationClassPostProcessor`
  - `MapperScannerConfigurer` 作为自定义 BFPP 已注册
- When
  - 执行容器 `refresh`
- Then
  - Mapper BeanDefinition 不会被注册
  - 该问题必须通过前置容器改造修复，不允许被静默忽略
  - 集成验收视为失败

## 资源释放验证策略说明（连接关闭/异常路径关闭）

### 验证目标
- 校验无事务场景下每次调用均独立获取和关闭 `SqlSession`。
- 校验 JDBC `Connection`、`Statement`、`ResultSet` 在正常路径与异常路径均被关闭。

### 验证策略
- 使用可观测 `DataSource` / `Connection` 测试替身记录 `open` 与 `close` 次数。
- 在正常查询后断言：
  - `Connection.close()` 调用 1 次
  - `Statement.close()` 调用 1 次
  - `ResultSet.close()` 调用 1 次
- 在执行器抛出异常后断言：
  - `SqlSessionTemplate` 仍执行 `SqlSession.close()`
  - JDBC 资源关闭次数与正常路径一致
- 在 `SqlSessionFactoryBean` 启动失败场景断言：
  - 没有泄漏半初始化的 `SqlSessionFactory`
  - 没有残留部分注册的 Mapper Bean 产品对象

### 资源释放边界
- Phase 1 只验证无事务场景。
- 不验证线程绑定连接复用。
- 不验证连接池归还语义。
