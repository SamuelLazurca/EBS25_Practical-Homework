package org.example.schema;

public class DoubleFieldLimit {
    public double min;
    public double max;

    public DoubleFieldLimit(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public double getRandomValue() {
        // round to 2 decimal places
        return Math.round((Math.random() * (max - min) + min) * 100.0) / 100.0;
    }
}
