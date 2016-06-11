package com.epienriz.hengruicao.wifidatacollector.core;

/**
 * Created by hengruicao on 6/8/16.
 */
public interface DataListener<T> {
    void notifyResult(T result);
}
