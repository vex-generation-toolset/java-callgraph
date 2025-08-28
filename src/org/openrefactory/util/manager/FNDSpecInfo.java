/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util.manager;

import java.util.List;

/**
 * <p>Value object for storing methods and types information
 *
 * @author Kanak Das
 */
public class FNDSpecInfo {
    private String declaringType;

    private String methodName;

    private String returnType;

    private List<String> modifiers;

    private String packageName;

    private int paramCount;

    private boolean canReturnNull;

    private boolean returnsPrimitive;

    private String sinceVersion;

    private List<String> params;

    // Two new pieces of information were added to the JSON spec file
    private List<String> exceptions;

    private String declaringTypeParam;

    // Adds a new field 'bounded_type_parameters' to JSON spec file
    private String boundedTypeParam;

    public FNDSpecInfo() {}

    public FNDSpecInfo(
            String declaringType,
            String methodName,
            String returnType,
            List<String> modifiers,
            String packageName,
            int paramCount,
            boolean canReturnNull,
            boolean returnsPrimitive,
            String sinceVersion,
            List<String> params,
            List<String> exceptions,
            String declaringTypeParam,
            String boundedTypeParam) {
        this.declaringType = declaringType;
        this.methodName = methodName;
        this.returnType = returnType;
        this.modifiers = modifiers;
        this.packageName = packageName;
        this.paramCount = paramCount;
        this.canReturnNull = canReturnNull;
        this.returnsPrimitive = returnsPrimitive;
        this.sinceVersion = sinceVersion;
        this.params = params;
        this.exceptions = exceptions;
        this.declaringTypeParam = declaringTypeParam;
        this.boundedTypeParam = boundedTypeParam;
    }

    public String getDeclaringType() {
        return declaringType;
    }

    public void setDeclaringType(String declaringType) {
        this.declaringType = declaringType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public List<String> getModifiers() {
        return modifiers;
    }

    public void setModifiers(List<String> modifiers) {
        this.modifiers = modifiers;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getParamCount() {
        return paramCount;
    }

    public void setParamCount(int paramCount) {
        this.paramCount = paramCount;
    }

    public boolean canReturnNull() {
        return canReturnNull;
    }

    public void setCanReturnNull(boolean canReturnNull) {
        this.canReturnNull = canReturnNull;
    }

    public boolean returnsPrimitive() {
        return returnsPrimitive;
    }

    public void setReturnsPrimitive(boolean returnsPrimitive) {
        this.returnsPrimitive = returnsPrimitive;
    }

    public String getSinceVersion() {
        return sinceVersion;
    }

    public void setSinceVersion(String sinceVersion) {
        this.sinceVersion = sinceVersion;
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }

    public List<String> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<String> exceptions) {
        this.exceptions = exceptions;
    }

    public String getDeclaringTypeParam() {
        return declaringTypeParam;
    }

    public void setDeclaringTypeParam(String declaringTypeParam) {
        this.declaringTypeParam = declaringTypeParam;
    }

    public String getBoundedTypeParam() {
        return boundedTypeParam;
    }

    public void setBoundedTypeParam(String boundedTypeParam) {
        this.boundedTypeParam = boundedTypeParam;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (canReturnNull ? 1231 : 1237);
        result = prime * result + ((declaringType == null) ? 0 : declaringType.hashCode());
        result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
        result = prime * result + ((modifiers == null) ? 0 : modifiers.hashCode());
        result = prime * result + ((packageName == null) ? 0 : packageName.hashCode());
        result = prime * result + paramCount;
        result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
        result = prime * result + (returnsPrimitive ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        FNDSpecInfo other = (FNDSpecInfo) obj;
        if (canReturnNull != other.canReturnNull) return false;
        if (declaringType == null) {
            if (other.declaringType != null) return false;
        } else if (!declaringType.equals(other.declaringType)) return false;
        if (methodName == null) {
            if (other.methodName != null) return false;
        } else if (!methodName.equals(other.methodName)) return false;
        if (modifiers == null) {
            if (other.modifiers != null) return false;
        } else if (!modifiers.equals(other.modifiers)) return false;
        if (packageName == null) {
            if (other.packageName != null) return false;
        } else if (!packageName.equals(other.packageName)) return false;
        if (paramCount != other.paramCount) return false;
        if (returnType == null) {
            if (other.returnType != null) return false;
        } else if (!returnType.equals(other.returnType)) return false;
        if (returnsPrimitive != other.returnsPrimitive) return false;
        return true;
    }

    @Override
    public String toString() {
        return "FNDSpecInfo [declaringType=" + declaringType + ", methodName=" + methodName + ", returnType="
                + returnType + ", modifiers=" + modifiers + ", packageName=" + packageName + ", paramCount="
                + paramCount
                + ", canReturnNull=" + canReturnNull + ", returnsPrimitive=" + returnsPrimitive + ", sinceVersion="
                + sinceVersion + ", params=" + params + "]";
    }
}
