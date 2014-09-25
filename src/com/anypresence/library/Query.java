package com.anypresence.library;

import java.io.Serializable;
import java.util.Map;

/**
 * A class that abstracts away loading data into a list fragment.
 * */
public final class Query implements Serializable {
	private static final long serialVersionUID = 4168511200737474705L;
	private final String mScope;
    private final Map<String, String> mParams;
    private final Integer mLimit;
    private final Integer mOffset;

    public Query(String scope) {
        mScope = scope;
        mParams = null;
        mLimit = null;
        mOffset = null;
    }

    public Query(String scope, Map<String, String> params) {
        mScope = scope;
        mParams = params;
        mLimit = null;
        mOffset = null;
    }

    public Query(String scope, Map<String, String> params, Integer limit, Integer offset) {
        mScope = scope;
        mParams = params;
        mLimit = limit;
        mOffset = offset;
    }

    public String getScope() {
        return mScope;
    }

    public Map<String, String> getParams() {
        return mParams;
    }

    public Integer getLimit() {
        return mLimit;
    }

    public Integer getOffset() {
        return mOffset;
    }
}
