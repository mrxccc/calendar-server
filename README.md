# 日历服务系统
项目目标：在一个系统中，集成多个日历源，如Google Calendar、Outlook Calendar、iCloud Calendar等。
## 项目简介
这是一个基于CalDAV协议的日历服务系统，支持多个日历源的集成和同步。目前支持：
- Google Calendar 集成
- CalDAV 服务器功能

## 技术栈
- 后端：Spring Boot 3.x + JPA/Hibernate
- 数据库：MySQL
- 认证：JWT + OAuth2.0 (用于Google Calendar集成)
- 缓存：Redis
- 测试：JUnit 5 + Mockito

## 主要功能模块
1. CalDAV服务器
   - 支持标准的CalDAV协议
   - 事件的CRUD操作
   - 日历同步功能

2. 外部日历集成
   - Google Calendar集成
   - Bedework集成
   - 事件冲突检测

3. 用户管理
   - 用户注册/登录
   - OAuth2.0授权管理

## 启动方式

1. **环境准备**：
   - 确保已安装Java 17或更高版本。
   - 确保MySQL数据库已启动，并配置好`src/main/resources/application.properties`中的数据库连接信息。

2. **构建项目**：
   - 在项目根目录下运行以下命令以构建项目：
     ```bash
     mvn clean install
     ```

3. **运行项目**：
   - 使用以下命令启动Spring Boot应用：
     ```bash
     mvn spring-boot:run
     ```

## 测试方式

1. **单元测试**：
   - 使用以下命令运行所有单元测试：
     ```bash
     mvn test
     ```

2. **API测试**：
   - 使用Postman导入`Google_Calendar_API_Tests.postman_collection.json`文件。
   - 运行Postman中的测试用例以验证API功能。

3. **MVP测试**：
   - 参考`MVP-TEST-PLAN.md`中的测试计划，手动执行各项功能测试。

## API文档
API文档通过Springdoc OpenAPI (Swagger)提供，访问地址：http://localhost:8080/swagger-ui.html 

4.启动Bedework测试CalDav流程 
**Docker启动Bedework**：
   - 在项目根目录下运行以下命令启动Bedework服务：
     ```bash
     docker-compose up -d bedework
     ```
   - 等待服务启动完成后，访问`http://localhost:8080/caladmin`查看Bedework日历服务