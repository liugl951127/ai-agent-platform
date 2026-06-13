-- =====================================================
-- 多租户 / 审计 / 灰度规则  -  schema 变更
-- 在已有 init.sql 后追加,或单独执行
-- =====================================================
USE agent_platform;

-- -----------------------------------------------------
-- 1. 给所有业务表加 tenant_id 列 + 索引
-- -----------------------------------------------------

-- sys_user
ALTER TABLE sys_user ADD COLUMN tenant_id BIGINT DEFAULT 0 AFTER id;
ALTER TABLE sys_user ADD INDEX idx_user_tenant (tenant_id, username);

-- agent_info
ALTER TABLE agent_info ADD COLUMN tenant_id BIGINT DEFAULT 0 AFTER id;
ALTER TABLE agent_info ADD INDEX idx_agent_tenant (tenant_id);

-- llm_model
ALTER TABLE llm_model ADD COLUMN tenant_id BIGINT DEFAULT 0 AFTER id;
ALTER TABLE llm_model ADD INDEX idx_llm_tenant (tenant_id);

-- agent_tool
ALTER TABLE agent_tool ADD COLUMN tenant_id BIGINT DEFAULT 0 AFTER id;
ALTER TABLE agent_tool ADD INDEX idx_tool_tenant (tenant_id);

-- chat_session
ALTER TABLE chat_session ADD COLUMN tenant_id BIGINT DEFAULT 0 AFTER id;
ALTER TABLE chat_session ADD INDEX idx_chat_session_tenant (tenant_id, user_id);

-- chat_message
ALTER TABLE chat_message ADD COLUMN tenant_id BIGINT DEFAULT 0 AFTER id;
ALTER TABLE chat_message ADD INDEX idx_chat_msg_tenant (tenant_id, session_id);

-- knowledge_base
ALTER TABLE knowledge_base ADD COLUMN tenant_id BIGINT DEFAULT 0 AFTER id;
ALTER TABLE knowledge_base ADD INDEX idx_kb_tenant (tenant_id);

-- -----------------------------------------------------
-- 2. sys_tenant (租户表)
-- -----------------------------------------------------
DROP TABLE IF EXISTS sys_tenant;
CREATE TABLE sys_tenant (
  id BIGINT PRIMARY KEY,
  code VARCHAR(64) UNIQUE NOT NULL COMMENT '租户编码,业务使用',
  name VARCHAR(128) NOT NULL COMMENT '租户名称',
  status TINYINT DEFAULT 1 COMMENT '1=启用 0=禁用',
  expire_at DATETIME COMMENT '过期时间 NULL=永久',
  create_time DATETIME,
  update_time DATETIME,
  deleted INT DEFAULT 0
) COMMENT='多租户';

-- 初始: 默认租户 + 演示租户
INSERT INTO sys_tenant(id, code, name, status) VALUES
  (1, 'default', '默认租户', 1),
  (2, 'demo',    '演示租户', 1);

-- 把已有的 admin / sample 数据归到 default 租户
UPDATE sys_user    SET tenant_id = 1;
UPDATE agent_info  SET tenant_id = 1;
UPDATE llm_model   SET tenant_id = 1;
UPDATE agent_tool  SET tenant_id = 1;
UPDATE knowledge_base SET tenant_id = 1;

-- -----------------------------------------------------
-- 3. sys_audit_log (审计日志)
-- -----------------------------------------------------
DROP TABLE IF EXISTS sys_audit_log;
CREATE TABLE sys_audit_log (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT,
  user_id BIGINT,
  username VARCHAR(64),
  module VARCHAR(64) COMMENT '业务模块',
  action VARCHAR(64) COMMENT '操作类型 (CREATE/UPDATE/DELETE/QUERY/...)',
  resource_type VARCHAR(64) COMMENT '资源类型',
  resource_id VARCHAR(128) COMMENT '资源 ID',
  method VARCHAR(16) COMMENT 'HTTP 方法',
  url VARCHAR(255) COMMENT '请求 URL',
  ip VARCHAR(64),
  ua VARCHAR(512),
  request_args TEXT COMMENT '请求参数 (脱敏后)',
  response_data TEXT,
  cost_ms INT COMMENT '耗时 ms',
  status TINYINT COMMENT '1=成功 0=失败',
  error_msg TEXT,
  create_time DATETIME,
  deleted INT DEFAULT 0,
  INDEX idx_audit_tenant_time (tenant_id, create_time),
  INDEX idx_audit_user_time   (user_id, create_time)
) COMMENT='操作审计';

-- -----------------------------------------------------
-- 4. sys_gray_rule (灰度发布规则)
-- -----------------------------------------------------
DROP TABLE IF EXISTS sys_gray_rule;
CREATE TABLE sys_gray_rule (
  id BIGINT PRIMARY KEY,
  name VARCHAR(128) NOT NULL COMMENT '规则名',
  resource VARCHAR(255) NOT NULL COMMENT '资源标识 (e.g. /agent/chat 或 AgentRunV2)',
  strategy VARCHAR(32) NOT NULL COMMENT '灰度策略: USER_ID / TENANT_ID / IP / RATIO',
  match_value VARCHAR(1024) COMMENT '匹配值 (逗号分隔 或 比例 0-100)',
  status TINYINT DEFAULT 1 COMMENT '1=启用 0=禁用',
  description VARCHAR(255),
  create_time DATETIME,
  update_time DATETIME,
  deleted INT DEFAULT 0,
  INDEX idx_gray_resource (resource, status)
) COMMENT='灰度发布规则';

-- 初始示例: demo 租户启用 agent/chat 的灰度
INSERT INTO sys_gray_rule(id, name, resource, strategy, match_value, status, description)
VALUES
  (1, 'demo 租户走新链路', '/agent/chat', 'TENANT_ID', '2', 1, 'demo 租户走新代码路径');
