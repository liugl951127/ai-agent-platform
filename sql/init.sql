CREATE DATABASE IF NOT EXISTS agent_platform DEFAULT CHARSET utf8mb4;
USE agent_platform;

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64) UNIQUE NOT NULL,
  password VARCHAR(128) NOT NULL,
  nickname VARCHAR(64),
  email VARCHAR(128),
  status TINYINT DEFAULT 1,
  create_time DATETIME,
  update_time DATETIME,
  deleted INT DEFAULT 0
);

DROP TABLE IF EXISTS agent_info;
CREATE TABLE agent_info (
  id BIGINT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  avatar VARCHAR(255),
  description TEXT,
  system_prompt TEXT,
  model_id BIGINT,
  tool_ids VARCHAR(512),
  workflow_key VARCHAR(128),
  knowledge_id BIGINT,
  user_id BIGINT,
  create_time DATETIME,
  update_time DATETIME,
  deleted INT DEFAULT 0
);

DROP TABLE IF EXISTS llm_model;
CREATE TABLE llm_model (
  id BIGINT PRIMARY KEY,
  name VARCHAR(128),
  provider VARCHAR(64),
  model_name VARCHAR(128),
  api_base VARCHAR(255),
  api_key VARCHAR(255),
  temperature DECIMAL(3,2) DEFAULT 0.70,
  max_tokens INT DEFAULT 2048,
  status TINYINT DEFAULT 1,
  create_time DATETIME,
  update_time DATETIME,
  deleted INT DEFAULT 0
);

DROP TABLE IF EXISTS agent_tool;
CREATE TABLE agent_tool (
  id BIGINT PRIMARY KEY,
  name VARCHAR(128),
  code VARCHAR(128) UNIQUE,
  description TEXT,
  param_schema TEXT,
  handler VARCHAR(255),
  create_time DATETIME,
  update_time DATETIME,
  deleted INT DEFAULT 0
);

DROP TABLE IF EXISTS chat_session;
CREATE TABLE chat_session (
  id BIGINT PRIMARY KEY,
  user_id BIGINT,
  agent_id BIGINT,
  title VARCHAR(255),
  create_time DATETIME,
  update_time DATETIME,
  deleted INT DEFAULT 0
);

DROP TABLE IF EXISTS chat_message;
CREATE TABLE chat_message (
  id BIGINT PRIMARY KEY,
  session_id BIGINT,
  role VARCHAR(32),
  content TEXT,
  tokens INT,
  create_time DATETIME,
  update_time DATETIME,
  deleted INT DEFAULT 0
);

DROP TABLE IF EXISTS knowledge_base;
CREATE TABLE knowledge_base (
  id BIGINT PRIMARY KEY,
  name VARCHAR(128),
  description TEXT,
  es_index VARCHAR(128),
  user_id BIGINT,
  create_time DATETIME,
  update_time DATETIME,
  deleted INT DEFAULT 0
);

INSERT INTO sys_user(id,username,password,nickname,status)
VALUES (1,'admin','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','管理员',1);

-- 默认示例智能体
INSERT INTO agent_info(id,name,description,system_prompt,model_id,tool_ids,knowledge_id,user_id)
VALUES (1,'小助手','通用对话助手','你是一个简洁友好的助手',1,'',NULL,1);

-- 默认模型
INSERT INTO llm_model(id,name,provider,model_name,api_base,api_key,temperature,max_tokens)
VALUES (1,'Ollama Llama3','OLLAMA','llama3','http://host.docker.internal:11434','',0.70,2048);

-- 示例工具
INSERT INTO agent_tool(id,name,code,description,param_schema,handler)
VALUES
(1,'天气查询','weather','查询某城市天气','{"type":"object","properties":{"city":{"type":"string"}},"required":["city"]}','weatherTool');
