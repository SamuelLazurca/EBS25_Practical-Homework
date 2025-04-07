package org.example.schema;

public class SchemaFields {
    public static final SchemaField STATION = new SchemaField(SchemaFieldNames.Station, SchemaFieldDataTypes.Integer);
    public static final SchemaField CITY = new SchemaField(SchemaFieldNames.City, SchemaFieldDataTypes.String);
    public static final SchemaField TEMP = new SchemaField(SchemaFieldNames.Temp, SchemaFieldDataTypes.Integer);
    public static final SchemaField RAIN = new SchemaField(SchemaFieldNames.Rain, SchemaFieldDataTypes.Double);
    public static final SchemaField WIND = new SchemaField(SchemaFieldNames.Wind, SchemaFieldDataTypes.Integer);
    public static final SchemaField DIRECTION = new SchemaField(SchemaFieldNames.Direction, SchemaFieldDataTypes.String);
    public static final SchemaField DATE = new SchemaField(SchemaFieldNames.Date, SchemaFieldDataTypes.Date);
}
