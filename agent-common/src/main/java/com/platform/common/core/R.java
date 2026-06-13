package com.platform.common.core;

import lombok.Data;
import java.io.Serializable;

@Data
public class R<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp = System.currentTimeMillis();

    public static <T> R<T> ok() { return ok(null); }
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }
    public static <T> R<T> fail(String msg) { return fail(500, msg); }
    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(msg);
        return r;
    }
}
