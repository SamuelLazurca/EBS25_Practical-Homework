package org.example.generators;

import org.example.schema.DateFieldLimit;
import org.example.schema.DoubleFieldLimit;
import org.example.schema.IntegerFieldLimit;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class GeneratorsParams {
    public final static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    public final static List<String> directions = List.of("N", "NE", "E", "SE", "S", "SW", "W", "NW");
    public final static List<String> cities = List.of("Bucharest", "Cluj", "Timisoara", "Iasi", "Constanta");
    public final static IntegerFieldLimit tempLimit = new IntegerFieldLimit(-30, 50);
    public final static IntegerFieldLimit windLimit= new IntegerFieldLimit(0, 100);
    public final static IntegerFieldLimit stationLimit = new IntegerFieldLimit(1, 100);
    public final static DoubleFieldLimit rainLimit = new DoubleFieldLimit(0.0, 100.0);

    public final static DateFieldLimit dateLimit;
    static {
        Calendar cal = Calendar.getInstance();
        // start date: January 1, 2025
        cal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startDate = cal.getTime();
        // end date: December 31, 2025
        cal.set(2025, Calendar.DECEMBER, 31, 23, 59, 59);
        Date endDate = cal.getTime();

        dateLimit = new DateFieldLimit(startDate, endDate);
    }

    public final static List<String> MapOperatorToString = List.of(
            ">",
            ">=",
            "<",
            "<=",
            "==",
            "!="
    );
}
