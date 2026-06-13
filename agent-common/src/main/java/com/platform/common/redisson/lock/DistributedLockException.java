package com.platform.common.redisson.lock;

/** 分布式锁获取失败异常 */
public class DistributedLockException extends RuntimeException {
    public DistributedLockException(String msg) { super(msg); }
}
