# Spring Cloud & Alibaba 分布式技术栈

## 概述

Spring Cloud 是一套微服务解决方案，Spring Cloud Alibaba 是阿里巴巴提供的 Spring Cloud 子项目，包含微服务开发所需的各类组件。

### 技术栈一览

**Spring Cloud:**
- OpenFeign
- Gateway

**Spring Cloud Alibaba:**
- Nacos
- Sentinel
- Seata

---

## 一、Nacos — 注册/配置中心

Nacos (Dynamic Naming and Configuration Service) 是阿里巴巴开源的一个更易于构建云原生应用的动态服务发现、配置管理和服务管理平台。

### 1.1 Nacos 安装

根据 Spring Cloud Alibaba 版本对应下载服务包。推荐使用 Docker 快速启动：

```bash
# Docker 启动 Nacos（单机模式）
docker run -d --name nacos \
  -e MODE=standalone \
  -p 8848:8848 \
  -p 9848:9848 \
  nacos/nacos-server:v2.2.3
```

访问地址：`http://localhost:8848/nacos`，默认账号密码：`nacos/nacos`

### 1.2 服务注册

#### 引入依赖

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

#### 配置 Nacos 地址

```yaml
# application.yml
spring:
  application:
    name: service-product
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
```

#### 启动微服务

```java
@SpringBootApplication
public class ProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}
```

启动后，该服务会自动注册到 Nacos，可在 Nacos 控制台"服务管理 → 服务列表"中查看。

### 1.3 服务发现

`@EnableDiscoveryClient` 不必显式声明，引入 Discovery 依赖后自动开启。

#### DiscoveryClient API

```java
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping("/services")
    public List<String> getServices() {
        // 获取所有注册的服务名称
        return discoveryClient.getServices();
    }

    @GetMapping("/instances")
    public List<ServiceInstance> getInstances() {
        // 获取 service-order 服务的所有实例
        return discoveryClient.getInstances("service-order");
    }
}
```

#### RestTemplate + @LoadBalanced（客户端负载均衡）

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced  // 开启负载均衡，服务名 -> 实际地址
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/create")
    public String createOrder() {
        // 通过服务名调用，由 Ribbon/LoadBalancer 完成负载均衡
        String result = restTemplate.getForObject(
            "http://service-product/api/product/1", String.class);
        return "Order created, product info: " + result;
    }
}
```

#### LoadBalancerClient API

```java
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @GetMapping("/choose")
    public String chooseInstance() {
        // 手动选择一个服务实例
        ServiceInstance instance = loadBalancerClient.choose("service-product");
        String url = instance.getUri() + "/api/product/1";
        // 使用选中的实例地址进行调用
        return "Chosen instance: " + url;
    }
}
```

### 1.4 配置中心

#### 基本配置

```yaml
# application.yml
spring:
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        namespace: ${spring.profiles.active:}
        group: order
        file-extension: yaml
  config:
    import:
      - nacos:common.yaml?group=order
```

> **说明：** `namespace`、`dataId`、`group` 三者共同实现环境数据隔离。

在 Nacos 控制台创建配置：
- Data ID：`service-order.yaml`
- Group：`order`
- 配置内容示例：

```yaml
# Nacos 配置中心中的内容
order:
  timeout: 30
  max-items: 100
  discount: 0.9
```

#### @RefreshScope 动态刷新

```java
@RestController
@RequestMapping("/api/config")
@RefreshScope  // 配置变更后自动刷新
public class ConfigController {

    @Value("${order.timeout:10}")
    private int timeout;

    @Value("${order.max-items:50}")
    private int maxItems;

    @GetMapping("/info")
    public Map<String, Object> getConfig() {
        return Map.of("timeout", timeout, "maxItems", maxItems);
    }
}
```

#### @ConfigurationProperties 动态属性配置类

```java
@Component
@ConfigurationProperties(prefix = "order")
@RefreshScope
public class OrderProperties {

    private int timeout = 10;
    private int maxItems = 50;
    private double discount = 1.0;

    // getters & setters
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    public int getMaxItems() { return maxItems; }
    public void setMaxItems(int maxItems) { this.maxItems = maxItems; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
}
```

使用方式：

```java
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private OrderProperties orderProperties;

    @GetMapping("/properties")
    public OrderProperties getProperties() {
        return orderProperties;  // 自动绑定，支持动态刷新
    }
}
```

---

## 二、OpenFeign — 远程调用

OpenFeign 是一个声明式的 HTTP 客户端，简化了微服务间的远程调用。

### 2.1 基本用法

#### 引入依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

#### 启用 Feign

```java
@SpringBootApplication
@EnableFeignClients  // 开启 Feign 客户端
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

#### 定义 Feign 接口

```java
@FeignClient(value = "service-product")  // value = 目标服务名
public interface ProductFeignClient {

    @GetMapping("/api/product/{productId}")
    Product getProductById(@PathVariable("productId") Long productId);

    @GetMapping("/api/product/list")
    List<Product> getProductList();

    @PostMapping("/api/product")
    Result createProduct(@RequestBody Product product);
}
```

#### 使用 Feign 调用

```java
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @GetMapping("/detail/{productId}")
    public String getOrderDetail(@PathVariable Long productId) {
        // 像调用本地方法一样调用远程服务
        Product product = productFeignClient.getProductById(productId);
        return "Order with product: " + product.getProductName();
    }
}
```

### 2.2 重试机制

```java
@Configuration
public class FeignConfig {

    @Bean
    public Retryer retryer() {
        // period=100ms, maxPeriod=1000ms, maxAttempts=5
        return new Retryer.Default(100, 1000, 5);
    }
}
```

```yaml
# application.yml — 配合超时配置
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 3000
            read-timeout: 5000
          service-product:  # 针对特定服务配置
            connect-timeout: 2000
            read-timeout: 3000
```

### 2.3 Fallback — 兜底返回（集成 Sentinel）

#### 配置

```yaml
# application.yml
feign:
  sentinel:
    enabled: true  # 开启 Feign 的 Sentinel 支持
```

#### 自定义兜底实现类

```java
@Component
public class ProductFeignClientFallback implements ProductFeignClient {

    @Override
    public Product getProductById(Long productId) {
        log.info("getProductById 接口的兜底策略触发");
        Product product = new Product();
        product.setId(productId);
        product.setPrice(BigDecimal.valueOf(500));
        product.setProductName("兜底商品-" + productId);
        product.setNum(1);
        return product;
    }

    @Override
    public List<Product> getProductList() {
        log.info("getProductList 接口的兜底策略触发");
        return Collections.emptyList();
    }

    @Override
    public Result createProduct(Product product) {
        log.info("createProduct 接口的兜底策略触发");
        return Result.fail("服务不可用，请稍后重试");
    }
}
```

#### 在 @FeignClient 中指定兜底类

```java
@FeignClient(
    value = "service-product",
    fallback = ProductFeignClientFallback.class  // 指定兜底实现
)
public interface ProductFeignClient {

    @GetMapping("/api/product/{productId}")
    Product getProductById(@PathVariable("productId") Long productId);

    @GetMapping("/api/product/list")
    List<Product> getProductList();

    @PostMapping("/api/product")
    Result createProduct(@RequestBody Product product);
}
```

---

## 三、Sentinel — 熔断限流

Sentinel 是阿里巴巴开源的面向分布式服务架构的流量控制、熔断降级组件。

### 3.1 引入依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: 127.0.0.1:8080  # Sentinel 控制台地址
```

### 3.2 启动 Sentinel 控制台

```bash
java -jar sentinel-dashboard-1.8.6.jar --server.port=8080
```

访问 `http://localhost:8080`，默认账号密码：`sentinel/sentinel`

### 3.3 定义资源层级

#### 自动发现资源层级

Web 接口和 Feign 接口都会被 Sentinel 自动发现，无需额外配置。

#### 自定义资源层级

```java
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @GetMapping("/create")
    @SentinelResource(value = "createOrder")  // 自定义资源名
    public Result createOrder(@RequestParam Long productId) {
        // 业务逻辑
        return Result.success("订单创建成功");
    }

    @PostMapping("/pay")
    @SentinelResource(value = "payOrder")
    public Result payOrder(@RequestParam Long orderId) {
        // 业务逻辑
        return Result.success("支付成功");
    }
}
```

### 3.4 定义规则

> **注意：** 在 Sentinel 网页端对不同资源层级进行规则配置。

#### 流量控制规则

**QPS（每秒查询数）限流示例：**

- 资源名：`createOrder`
- 阈值类型：QPS
- 单机阈值：10（每秒最多10个请求）

**流控模式：**

| 模式 | 说明 | 使用场景 |
|------|------|----------|
| 直接 | 对本资源直接限流 | 保护单个接口 |
| 关联 | 关联资源达到阈值时，限流本资源 | 写操作影响读操作时 |
| 链路 | 只记录指定链路上的流量 | 对特定调用来源限流 |

**流控效果：**

| 效果 | 说明 | 示例 |
|------|------|------|
| 快速失败 | 直接抛出 FlowException | 默认方式，返回错误 |
| Warm Up | 预热，阈值逐步提升 | 防止冷启动被突发流量打垮 |
| 匀速排队 | 请求匀速通过，排队等待 | 用于处理间隔性突发流量 |

#### 熔断降级规则

**断路器三种状态：**

```
  ┌──────────┐    达到阈值     ┌──────────┐
  │  关闭     │ ─────────────→ │  打开     │
  │ (CLOSED) │                │ (OPEN)   │
  └──────────┘                └────┬─────┘
       ↑                           │
       │       超过等待时间          │
       │    ┌──────────┐            │
       └────│  半开     │←───────────┘
            │ (HALF_OPEN)│
            └──────────┘
```

**熔断策略配置示例：**

- 资源名：`getProductById`
- 熔断策略：慢调用比例
- 最大 RT：200ms
- 比例阈值：0.5（50%）
- 熔断时长：10s
- 最小请求数：5

### 3.5 兜底回调

#### 方式一：Web API 自定义 BlockExceptionHandler

```java
@Component
public class CustomBlockExceptionHandler implements BlockExceptionHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       BlockException e) throws Exception {
        response.setContentType("application/json;charset=utf-8");
        Result result = null;

        if (e instanceof FlowException) {
            result = Result.fail(429, "请求被限流了，请稍后重试");
        } else if (e instanceof DegradeException) {
            result = Result.fail(500, "服务被降级了，请稍后重试");
        } else if (e instanceof ParamFlowException) {
            result = Result.fail(429, "热点参数限流");
        } else if (e instanceof AuthorityException) {
            result = Result.fail(403, "授权规则不通过");
        }

        response.getWriter().write(JSON.toJSONString(result));
    }
}
```

#### 方式二：@SentinelResource 定义兜底回调

```java
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @GetMapping("/create")
    @SentinelResource(
        value = "createOrder",
        blockHandler = "createOrderBlockHandler",  // 限流/降级兜底
        fallback = "createOrderFallback"            // 业务异常兜底
    )
    public Result createOrder(@RequestParam Long productId) {
        // 可能抛出异常的业务逻辑
        if (productId <= 0) {
            throw new IllegalArgumentException("商品ID不合法");
        }
        return Result.success("订单创建成功");
    }

    // 限流/降级时的兜底方法（参数需与原方法一致，额外加 BlockException）
    public Result createOrderBlockHandler(Long productId, BlockException e) {
        return Result.fail("createOrder 接口被限流或降级");
    }

    // 业务异常时的兜底方法
    public Result createOrderFallback(Long productId, Throwable e) {
        return Result.fail("createOrder 接口发生异常: " + e.getMessage());
    }
}
```

#### 方式三：OpenFeign 定义兜底回调

参考上文 [2.3 Fallback 章节](#23-fallback--兜底返回集成-sentinel)。

#### 方式四：Spring Boot 全局异常处理统一处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FlowException.class)
    public Result handleFlowException(FlowException e) {
        return Result.fail(429, "流量限制，请稍后重试");
    }

    @ExceptionHandler(DegradeException.class)
    public Result handleDegradeException(DegradeException e) {
        return Result.fail(500, "服务降级中，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统内部错误");
    }
}
```

---

## 四、Gateway — 网关

Spring Cloud Gateway 是 Spring Cloud 的网关组件，基于 WebFlux，提供路由、过滤等功能。

### 4.1 引入依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

### 4.2 Route — 路由

路由是网关的基本构建块，由 ID、目标 URI、断言集合和过滤器集合组成。

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: order-route
          uri: lb://service-order       # lb:// 表示负载均衡到服务
          predicates:
            - Path=/api/order/**        # 匹配 /api/order/ 开头的路径
          filters:
            - RewritePath=/api/(?<segment>.*), /$\{segment}  # 路径重写

        - id: product-route
          uri: lb://service-product
          predicates:
            - Path=/api/product/**
          filters:
            - StripPrefix=1             # 去掉第一段路径前缀

        - id: user-route
          uri: http://localhost:9001    # 直接路由到具体地址
          predicates:
            - Path=/api/user/**
```

### 4.3 Predicate — 断言

#### 内置断言

| 断言工厂 | 说明 | 示例 |
|----------|------|------|
| Path | 路径匹配 | `- Path=/api/**` |
| Method | 请求方法匹配 | `- Method=GET,POST` |
| Header | Header 匹配 | `- Header=X-Request-Id, \d+` |
| Query | 参数匹配 | `- Query=name, zhangsan` |
| Cookie | Cookie 匹配 | `- Cookie=sessionId, .+` |
| Host | Host 匹配 | `- Host=**.example.com` |
| After/Before/Between | 时间匹配 | `- After=2024-01-01T00:00:00+08:00` |
| Weight | 权重路由 | `- Weight=group1, 80` |

#### 自定义断言

假设我们需要一个 **时间段断言**，只允许在指定时间范围内访问（比如活动期间）。

**实现断言工厂：**

```java
@Component
public class TimeRangeRoutePredicateFactory
        extends AbstractRoutePredicateFactory<TimeRangeRoutePredicateFactory.Config> {

    public TimeRangeRoutePredicateFactory() {
        super(Config.class);
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return exchange -> {
            LocalTime now = LocalTime.now();
            // 判断当前时间是否在配置的时间段内
            return !now.isBefore(config.getStart()) && !now.isAfter(config.getEnd());
        };
    }

    @Data
    public static class Config {
        private LocalTime start;   // 开始时间，如 09:00
        private LocalTime end;     // 结束时间，如 18:00
    }
}
```

**配置使用：**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: activity-route
          uri: lb://service-activity
          predicates:
            - Path=/api/activity/**
            - TimeRange=09:00,18:00   # 仅在 9:00 ~ 18:00 时间段生效
```

> **原理：** 类名 `TimeRangeRoutePredicateFactory` 去掉 `RoutePredicateFactory` 后缀，得到 `TimeRange`，YAML 中用它来引用，等号后面传入参数即可。

### 4.4 Filter — 过滤器

#### 内置过滤器

| 过滤器工厂 | 说明 |
|-----------|------|
| AddRequestHeader | 添加请求头 |
| AddRequestParameter | 添加请求参数 |
| AddResponseHeader | 添加响应头 |
| RewritePath | 路径重写 |
| StripPrefix | 去除路径前缀 |
| PrefixPath | 添加路径前缀 |
| Retry | 重试 |
| RequestRateLimiter | 请求限流 |
| CircuitBreaker | 断路器 |

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-route
          uri: lb://service-order
          predicates:
            - Path=/api/order/**
          filters:
            - AddRequestHeader=X-Color, blue          # 添加请求头
            - AddRequestParameter=source, gateway      # 添加请求参数
            - RewritePath=/api/(?<segment>.*), /$\{segment}
```

#### 自定义过滤器（全局过滤器）

```java
@Component
@Slf4j
public class RtGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        long startTime = System.currentTimeMillis();
        log.info("请求[{}]开始，时间：{}", request.getURI(), startTime);

        Mono<Void> filter = chain.filter(exchange);
        filter.doFinally((result) -> {
            long endTime = System.currentTimeMillis();
            log.info("请求[{}]结束，时间：{}，耗时：{}ms",
                request.getURI(), endTime, endTime - startTime);
        });
        return filter;
    }

    @Override
    public int getOrder() {
        return 0;  // 过滤器执行顺序，数值越小越先执行
    }
}
```

---

## 五、Seata — 分布式事务

Seata (Simple Extensible Autonomous Transaction Architecture) 是阿里巴巴开源的分布式事务解决方案。

### 5.1 架构

Seata 的分布式事务由三个核心组件组成：

```
┌──────────────────────────────────────────────────────────┐
│                        TC (事务协调者)                      │
│                      Seata Server                          │
│                   ┌─────────────────┐                      │
│                   │  全局事务状态管理  │                      │
│                   │  分支事务状态管理  │                      │
│                   └────────┬────────┘                      │
└────────────────────────────┼───────────────────────────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  TM (事务管理器)  │ │  RM (资源管理器)  │ │  RM (资源管理器)  │
│   订单服务        │ │   商品服务        │ │   库存服务        │
│  发起全局事务     │ │  参与全局事务     │ │  参与全局事务     │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

| 角色 | 说明 |
|------|------|
| **TC** (Transaction Coordinator) | 事务协调者，维护全局和分支事务状态，驱动全局事务提交或回滚 |
| **TM** (Transaction Manager) | 事务管理器，定义全局事务范围，发起全局事务的服务 |
| **RM** (Resource Manager) | 资源管理器，每个和本次事务相关的服务，管理分支事务 |

### 5.2 二阶提交协议

```
第一阶段 — 本地提交 (Prepare Phase)         第二阶段 — 全局提交/回滚 (Commit/Rollback Phase)
─────────────────────────────────────     ───────────────────────────────────────────
TM ──开启全局事务──▶ TC                    TC ──全局提交──▶ 各 RM
                      │                                      │
TM ──注册分支事务──▶  TC                    TM 通知 TC 提交   各 RM 提交本地事务
                      │                    ─────────────────▶
各 RM 执行业务逻辑    │                    或
各 RM 注册分支事务   │                    TM 通知 TC 回滚    各 RM 回滚本地事务
各 RM 本地事务提交   │                    ─────────────────▶
（undo log 已记录）  │
```

### 5.3 四种事务模式

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| **AT** | 自动补偿型，基于 undo log | 默认模式，适用于大部分场景 |
| **TCC** | Try-Confirm-Cancel，手动补偿 | 需要自定义资源锁定的场景 |
| **Saga** | 长事务，正向补偿 | 长流程、老系统集成 |
| **XA** | 强一致性，两阶段提交协议 | 对一致性要求极高的场景 |

> **注：** 着重掌握 AT 和 TCC 两种常用模式即可，默认使用 AT 模式。

### 5.4 AT 模式示例

#### 引入依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>
```

#### 配置

```yaml
# application.yml
seata:
  enabled: true
  tx-service-group: my_test_tx_group  # 事务分组
  service:
    vgroup-mapping:
      my_test_tx_group: default
    grouplist:
      default: 127.0.0.1:8091
```

#### 使用 @GlobalTransactional

```java
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private StockFeignClient stockFeignClient;

    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public void createOrder(Long productId, Integer quantity) {
        // 1. 创建订单（本地事务）
        Order order = new Order();
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setStatus("CREATED");
        orderMapper.insert(order);

        // 2. 扣减库存（远程调用，分支事务）
        stockFeignClient.deduct(productId, quantity);

        // 3. 查询商品信息（远程调用，分支事务）
        Product product = productFeignClient.getProductById(productId);

        // 4. 更新订单金额
        order.setTotalAmount(product.getPrice().multiply(
            BigDecimal.valueOf(quantity)));
        orderMapper.updateById(order);

        // 任何一步失败，全局事务自动回滚
    }
}
```

**AT 模式原理：**

```
1. 一阶段（本地提交）：
   - 执行本地 SQL
   - 记录 undo log（用于回滚）
   - 提交本地事务
   - 向 TC 报告分支事务状态

2. 二阶段（全局决议）：
   - 全局提交：异步删除 undo log
   - 全局回滚：通过 undo log 进行数据补偿回滚
```

### 5.5 TCC 模式示例

TCC 模式需要手动实现 Try、Confirm、Cancel 三个方法：

```java
@LocalTCC  // 标记为 TCC 模式
public interface AccountTccAction {

    /**
     * Try：资源预留
     */
    @TwoPhaseBusinessAction(name = "deductAccount", commitMethod = "commit", rollbackMethod = "rollback")
    boolean prepareDeduct(
        @BusinessActionContextParameter(paramName = "userId") Long userId,
        @BusinessActionContextParameter(paramName = "amount") BigDecimal amount);

    /**
     * Confirm：确认提交
     */
    boolean commit(BusinessActionContext context);

    /**
     * Cancel：回滚
     */
    boolean rollback(BusinessActionContext context);
}
```

```java
@Service
public class AccountTccActionImpl implements AccountTccAction {

    @Autowired
    private AccountMapper accountMapper;

    @Override
    public boolean prepareDeduct(Long userId, BigDecimal amount) {
        // Try 阶段：冻结金额
        Account account = accountMapper.selectById(userId);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }
        account.setBalance(account.getBalance().subtract(amount));
        account.setFrozenAmount(account.getFrozenAmount().add(amount));
        accountMapper.updateById(account);
        return true;
    }

    @Override
    public boolean commit(BusinessActionContext context) {
        // Confirm 阶段：确认扣减（解冻）
        Long userId = Long.valueOf(context.getActionContext("userId").toString());
        Account account = accountMapper.selectById(userId);
        account.setFrozenAmount(BigDecimal.ZERO);
        accountMapper.updateById(account);
        return true;
    }

    @Override
    public boolean rollback(BusinessActionContext context) {
        // Cancel 阶段：回滚（解冻并恢复余额）
        Long userId = Long.valueOf(context.getActionContext("userId").toString());
        BigDecimal amount = new BigDecimal(
            context.getActionContext("amount").toString());
        Account account = accountMapper.selectById(userId);
        account.setBalance(account.getBalance().add(amount));
        account.setFrozenAmount(account.getFrozenAmount().subtract(amount));
        accountMapper.updateById(account);
        return true;
    }
}
```

```java
// 在业务服务中使用 TCC
@Service
public class OrderService {

    @Autowired
    private AccountTccAction accountTccAction;

    @GlobalTransactional
    public void createOrderWithPayment(Long userId, BigDecimal amount) {
        // TCC 的 Try 阶段被 @GlobalTransactional 自动管理
        accountTccAction.prepareDeduct(userId, amount);

        // 其他分支事务...
        // 全局提交时自动调用 commit()
        // 全局回滚时自动调用 rollback()
    }
}
```

---

> **本文档基于 Spring Cloud & Alibaba 分布式技术栈学习笔记整理，为每个组件添加了可运行的示例代码。**
