package com.blss.blss.camunda;

public record CamundaVariable(
        Object value,
        String type
) {
    public static CamundaVariable string(Object value) {
        return new CamundaVariable(value == null ? null : value.toString(), "String");
    }

    public static CamundaVariable integer(Integer value) {
        return new CamundaVariable(value, "Integer");
    }

    public static CamundaVariable bool(Boolean value) {
        return new CamundaVariable(value, "Boolean");
    }

    public static CamundaVariable json(Object value) {
        return new CamundaVariable(value, "Json");
    }
}
