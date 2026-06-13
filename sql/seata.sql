-- =====================================================
-- Seata Server / Client 数据库表 (MySQL 8)
-- 用于 AT 模式的 undo_log
-- =====================================================

CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARSET utf8mb4;
USE seata;

-- undo_log (AT 模式必须)
DROP TABLE IF EXISTS undo_log;
CREATE TABLE undo_log (
  id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
  branch_id     BIGINT       NOT NULL,
  xid           VARCHAR(100) NOT NULL,
  context       VARCHAR(128) NOT NULL,
  rollback_info LONGBLOB     NOT NULL,
  log_status    INT          NOT NULL,
  log_created   DATETIME     NOT NULL,
  log_modified  DATETIME     NOT NULL,
  ext           VARCHAR(100) DEFAULT NULL,
  INDEX idx_log_created (log_created),
  INDEX idx_xid (xid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- distributed_lock (集群模式下用)
DROP TABLE IF EXISTS distributed_lock;
CREATE TABLE distributed_lock (
  lock_key     VARCHAR(20) PRIMARY KEY,
  lock_value   VARCHAR(20) NOT NULL,
  expire       BIGINT      DEFAULT 0
);

INSERT INTO distributed_lock(lock_key, lock_value, expire)
VALUES ('AsyncCommittingRetry', ' ', 0),
       ('RetryRollbacking',     ' ', 0),
       ('RetryRollbackingDead', ' ', 0);
