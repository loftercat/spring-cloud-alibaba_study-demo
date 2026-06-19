# Cloud-Demo

基于 Spring Cloud Alibaba 的微服务学习项目，涵盖服务注册发现、配置中心、负载均衡、声明式调用、流量控制、网关路由和分布式事务等核心能力。

---

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.4 | 基础框架 |
| Spring Cloud | 2023.0.3 | 微服务生态 |
| Spring Cloud Alibaba | 2023.0.3.2 | 阿里微服务套件 |
| Nacos | 2.4.3 | 服务注册发现 & 配置中心 |
| Sentinel | 1.8.x | 流量控制 & 熔断降级 |
| Seata | 2.x | 分布式事务 |
| Gateway | 2023.0.3 | API 网关 |
| OpenFeign | 2023.0.3 | 声明式服务调用 |
| MyBatis | 3.0.4 | ORM 框架 |
| MySQL | 8.x | 关系型数据库 |

---

## 项目结构

```
cloud-demo                          # 总父项目（依赖管理）
├── model                           # 公共模型层
├── gateway                         # Spring Cloud Gateway 网关 :8080
└── services                        # 服务聚合层
    ├── service-product             # 商品服务 :9000
    ├── service-order               # 订单服务 :8000
    ├── seata-business              # Seata 业务服务（TM）:21000
    ├── seata-order                 # Seata 订单服务（RM）:22000
    ├── seata-account               # Seata 账户服务（RM）:20000
    └── seata-storage               # Seata 库存服务（RM）:23000
```

---

## 快速启动

### 前置依赖

1. **Nacos Server**（注册中心 & 配置中心）
   ```bash
   # 下载并启动 Nacos
   startup.cmd -m standalone
   # 默认地址：http://127.0.0.1:8848
   ```

2. **Sentinel Dashboard**（可选，用于流量控制可视化）
   ```bash
   java -jar sentinel-dashboard-1.8.x.jar
   # 默认地址：http://127.0.0.1:8080
   ```

3. **Seata Server**（分布式事务协调器）
   ```bash
   # 下载并启动 Seata TC
   # 默认地址：http://127.0.0.1:8091
   ```

4. **MySQL**（Seata RM 服务需要）
   - 创建 `storage_db`、`order_db`、`account_db` 三个数据库
   - 各库中创建对应的业务表和 `undo_log` 表

### 启动顺序

| 顺序 | 服务 | 端口 | 说明 |
|------|------|------|------|
| 1 | gateway | 8080 | API 统一入口 |
| 2 | service-product | 9000 | 商品服务 |
| 3 | service-order | 8000 | 订单服务 |
| 4 | seata-storage | 23000 | 库存服务（RM）|
| 5 | seata-account | 20000 | 账户服务（RM）|
| 6 | seata-order | 22000 | 订单服务（RM）|
| 7 | seata-business | 21000 | 业务服务（TM）|

---

## 功能模块

### 1. 服务注册与发现（Nacos）
- 所有服务启动后自动注册到 Nacos
- 支持服务健康检查与动态上下线

### 2. 服务间调用
- **RestTemplate**：三种调用方式（DiscoveryClient / LoadBalancerClient / @LoadBalanced）
- **OpenFeign**：声明式远程调用，支持 Fallback 兜底

### 3. 配置中心（Nacos Config）
- `@Value` + `@RefreshScope`：动态刷新单个配置
- `@ConfigurationProperties`：类型安全的配置绑定，无需额外注解即可自动刷新

### 4. 流量控制（Sentinel）
- `@SentinelResource`：注解方式限流与降级
- `BlockExceptionHandler`：全局异常统一处理
- 与 OpenFeign 整合：服务异常自动触发 Fallback

### 5. API 网关（Gateway）
- 路由转发与负载均衡
- 全局 CORS 跨域配置
- 自定义 GatewayFilterFactory 与 GlobalFilter
- 请求耗时统计、响应头处理

### 6. 分布式事务（Seata）
- **AT 模式**：自动代理数据源，通过 `undo_log` 实现无侵入回滚
- **TCC 模式**：手动实现 Try/Confirm/Cancel 三阶段，性能更好、控制更精细

---

## 接口速查

### 基础微服务接口

| 接口 | 地址 |
|------|------|
| 查询商品 | `GET http://localhost:8080/product/{id}` |
| 创建订单 | `GET http://localhost:8080/order/create?productId=1&userId=1` |
| 查看动态配置 | `GET http://localhost:8000/order/config` |

### Seata 分布式事务接口

| 接口 | 地址 |
|------|------|
| 下单购买 | `GET http://localhost:21000/purchase?userId=1&commodityCode=SKU001&count=2` |

> 通过修改 `count=5` 可触发 storage 服务异常，验证全局回滚。

---

## 学习文档

详细的学习流程和原理说明请查看：

📄 [docs/spring-cloud-alibaba-learning.md](docs/spring-cloud-alibaba-learning.md)

---

## 注意事项

1. **版本兼容性**：Spring Boot、Spring Cloud、Spring Cloud Alibaba、Nacos 版本必须严格匹配
2. **Gateway 依赖**：不能引入 `spring-boot-starter-web`（Servlet 阻塞模型），Gateway 基于 WebFlux
3. **Seata 数据源**：RM 服务需要配置独立数据源，Seata 会自动代理数据源实现分支事务注册
4. **Feign 调用**：被调用的服务方法需要标注 `@Transactional`，Seata 才能将其纳入全局事务管理