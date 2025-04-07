package org.example.schema;

public class SubscriptionValue {
    private final Operator operator;
    private final String value;

    public SubscriptionValue(Operator operator, String value) {
        this.operator = operator;
        this.value = value;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }
}
