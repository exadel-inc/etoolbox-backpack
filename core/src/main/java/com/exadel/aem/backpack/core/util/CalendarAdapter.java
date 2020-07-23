package com.exadel.aem.backpack.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * In {@link Gson} serialization, renders {@link Calendar} value as an ISO-formatted string with timezone
 */
public class CalendarAdapter extends TypeAdapter<Calendar> implements JsonSerializer<Calendar>, JsonDeserializer<Calendar> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarAdapter.class);

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final Gson GSON = new GsonBuilder().setDateFormat(DATE_PATTERN).create();
    private static final TypeAdapter<Date> DATE_ADAPTER = GSON.getAdapter(Date.class);

    /**
     * Serializes current {@link Calendar} instance as a JSON entity
     * @param calendar The object to be converted to a JSON value
     * @param type The actual type of the {@code Calendar} provided
     * @param context Current {@code JsonSerializationContext}
     * @return A {@code JsonElement} corresponding to the specified object
     */
    @Override
    public JsonElement serialize(Calendar calendar, Type type, JsonSerializationContext context) {
        if (calendar == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN);
        return new JsonPrimitive(format.format(calendar.getTime()));
    }

    /**
     * Reads a {@link Calendar} instance from the provided JSON entity
     * @param jsonElement The {@code JsonElement} to parse
     * @param type The actual type of the {@code Calendar} to restore
     * @param jsonDeserializationContext Current {@code JsonDeserializationContext}
     * @return {@code Calendar} instance, or null in case an exception occurred while parsing JSON
     */
    @Override
    public Calendar deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
        Date date;
        SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN);
        try {
            date = format.parse(jsonElement.getAsString());
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(date);
            return calendar;
        } catch (ParseException e) {
            LOGGER.error("Could not parse '{}' as date value", jsonElement);
        }
        return null;
    }

    /**
     * Writes a JSON value to an underlying output stream
     * @param out Current {@code JsonWriter} instance
     * @param calendar The {@code Calendar} value to serialize. May be a null reference
     */
    @Override
    public void write(JsonWriter out, Calendar calendar) throws IOException {
        if (calendar == null) {
            out.value(StringUtils.EMPTY);
            return;
        }
        DATE_ADAPTER.write(out, calendar.getTime());
    }

    /**
     * Writes a Calendar value from an underlying input stream
     * @param in Current {@code JsonReader} instance
     * @return The deserialized {@code Calendar} value
     */
    @Override
    public Calendar read(JsonReader in) throws IOException {
        Date read = DATE_ADAPTER.read(in);
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(read);
        return calendar;
    }
}
