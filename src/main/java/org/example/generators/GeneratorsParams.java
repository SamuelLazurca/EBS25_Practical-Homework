package org.example.generators;

import org.example.schema.DateFieldLimit;
import org.example.schema.DoubleFieldLimit;
import org.example.schema.IntegerFieldLimit;

import java.text.SimpleDateFormat;
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
    public final static DateFieldLimit dateLimit = new DateFieldLimit(new Date(2025-1900, 1, 1), new Date(2025-1900, 12, 31));
}
