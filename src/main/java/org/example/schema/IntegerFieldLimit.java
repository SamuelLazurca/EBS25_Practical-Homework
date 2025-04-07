package org.example.schema;

public class IntegerFieldLimit {
    public int min;
    public int max;

    public IntegerFieldLimit(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public int getRandomValue() {
        return (int) (Math.random() * (max - min + 1)) + min;
    }
}
