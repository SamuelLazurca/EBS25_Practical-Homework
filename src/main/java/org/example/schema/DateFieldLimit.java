package org.example.schema;

import java.util.Date;

public class DateFieldLimit {
    public Date min;
    public Date max;

    public DateFieldLimit(Date min, Date max) {
        this.min = min;
        this.max = max;
    }

    public Date getRandomValue() {
        long minMillis = min.getTime();
        long maxMillis = max.getTime();
        long randomMillis = (long) (Math.random() * (maxMillis - minMillis)) + minMillis;
        return new Date(randomMillis);
    }
}
