package com.clouder.watch.common.sync;

import com.google.gson.Gson;

/**
 * Created by yang_shoulai on 8/17/2015.
 */
public class SyncMessage {

    public enum Method {
        Get, Set
    }

    private String pkgName;

    private String path;

    private Method method = Method.Set;

    public SyncMessage() {
    }

    public SyncMessage(String path) {
        this(null, path);
    }

    public SyncMessage(String packageName, String path) {
        this.pkgName = packageName;
        this.path = path;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public byte[] toBytes() {
        return toString().getBytes();
    }

    public String toString() {
        return new Gson().toJson(this);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPkgName() {
        return pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }
}
