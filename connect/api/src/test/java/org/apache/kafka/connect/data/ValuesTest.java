/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.connect.data;

import org.apache.kafka.connect.data.Schema.Type;
import org.apache.kafka.connect.data.Values.Parser;
import org.apache.kafka.connect.errors.DataException;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ValuesTest {

    private static final Map<String, String> STRING_MAP = new LinkedHashMap<>();
    private static final Schema STRING_MAP_SCHEMA = SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.STRING_SCHEMA).schema();

    private static final Map<String, Short> STRING_SHORT_MAP = new LinkedHashMap<>();
    private static final Schema STRING_SHORT_MAP_SCHEMA = SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.INT16_SCHEMA).schema();

    private static final Map<String, Integer> STRING_INT_MAP = new LinkedHashMap<>();
    private static final Schema STRING_INT_MAP_SCHEMA = SchemaBuilder.map(Schema.STRING_SCHEMA, Schema.INT32_SCHEMA).schema();

    private static final List<Integer> INT_LIST = new ArrayList<>();
    private static final Schema INT_LIST_SCHEMA = SchemaBuilder.array(Schema.INT32_SCHEMA).schema();

    private static final List<String> STRING_LIST = new ArrayList<>();
    private static final Schema STRING_LIST_SCHEMA = SchemaBuilder.array(Schema.STRING_SCHEMA).schema();

    static {
        STRING_MAP.put("foo", "123");
        STRING_MAP.put("bar", "baz");
        STRING_SHORT_MAP.put("foo", (short) 12345);
        STRING_SHORT_MAP.put("bar", (short) 0);
        STRING_SHORT_MAP.put("baz", (short) -4321);
        STRING_INT_MAP.put("foo", 1234567890);
        STRING_INT_MAP.put("bar", 0);
        STRING_INT_MAP.put("baz", -987654321);
        STRING_LIST.add("foo");
        STRING_LIST.add("bar");
        INT_LIST.add(1234567890);
        INT_LIST.add(-987654321);
    }

    @Test
    public void shouldEscapeStringsWithEmbeddedQuotesAndBackslashes() {
        String original = "three\"blind\\\"mice";
        String expected = "three\\\"blind\\\\\\\"mice";
        assertEquals(expected, Values.escape(original));
    }

    @Test
    public void shouldConvertNullValue() {
        assertRoundTrip(Schema.STRING_SCHEMA, Schema.STRING_SCHEMA, null);
        assertRoundTrip(Schema.OPTIONAL_STRING_SCHEMA, Schema.STRING_SCHEMA, null);
    }

    @Test
    public void shouldConvertSimpleString() {
        assertRoundTrip(Schema.STRING_SCHEMA,  "simple");
    }

    @Test
    public void shouldConvertEmptyString() {
        assertRoundTrip(Schema.STRING_SCHEMA, "");
    }

    @Test
    public void shouldConvertStringWithQuotesAndOtherDelimiterCharacters() {
        assertRoundTrip(Schema.STRING_SCHEMA, Schema.STRING_SCHEMA, "three\"blind\\\"mice");
        assertRoundTrip(Schema.STRING_SCHEMA, Schema.STRING_SCHEMA, "string with delimiters: <>?,./\\=+-!@#$%^&*(){}[]|;':");
    }

    @Test
    public void shouldConvertMapWithStringKeys() {
        assertRoundTrip(STRING_MAP_SCHEMA, STRING_MAP_SCHEMA, STRING_MAP);
    }

    @Test
    public void shouldParseStringOfMapWithStringValuesWithoutWhitespaceAsMap() {
        SchemaAndValue result = roundTrip(STRING_MAP_SCHEMA, "{\"foo\":\"123\",\"bar\":\"baz\"}");
        assertEquals(STRING_MAP_SCHEMA, result.schema());
        assertEquals(STRING_MAP, result.value());
    }

    @Test
    public void shouldParseStringOfMapWithStringValuesWithWhitespaceAsMap() {
        SchemaAndValue result = roundTrip(STRING_MAP_SCHEMA, "{ \"foo\" : \"123\", \n\"bar\" : \"baz\" } ");
        assertEquals(STRING_MAP_SCHEMA, result.schema());
        assertEquals(STRING_MAP, result.value());
    }

    @Test
    public void shouldConvertMapWithStringKeysAndShortValues() {
        assertRoundTrip(STRING_SHORT_MAP_SCHEMA, STRING_SHORT_MAP_SCHEMA, STRING_SHORT_MAP);
    }

    @Test
    public void shouldParseStringOfMapWithShortValuesWithoutWhitespaceAsMap() {
        SchemaAndValue result = roundTrip(STRING_SHORT_MAP_SCHEMA, "{\"foo\":12345,\"bar\":0,\"baz\":-4321}");
        assertEquals(STRING_SHORT_MAP_SCHEMA, result.schema());
        assertEquals(STRING_SHORT_MAP, result.value());
    }

    @Test
    public void shouldParseStringOfMapWithShortValuesWithWhitespaceAsMap() {
        SchemaAndValue result = roundTrip(STRING_SHORT_MAP_SCHEMA, " { \"foo\" :  12345 , \"bar\" : 0,  \"baz\" : -4321 }  ");
        assertEquals(STRING_SHORT_MAP_SCHEMA, result.schema());
        assertEquals(STRING_SHORT_MAP, result.value());
    }

    @Test
    public void shouldConvertMapWithStringKeysAndIntegerValues() {
        assertRoundTrip(STRING_INT_MAP_SCHEMA, STRING_INT_MAP_SCHEMA, STRING_INT_MAP);
    }

    @Test
    public void shouldParseStringOfMapWithIntValuesWithoutWhitespaceAsMap() {
        SchemaAndValue result = roundTrip(STRING_INT_MAP_SCHEMA, "{\"foo\":1234567890,\"bar\":0,\"baz\":-987654321}");
        assertEquals(STRING_INT_MAP_SCHEMA, result.schema());
        assertEquals(STRING_INT_MAP, result.value());
    }

    @Test
    public void shouldParseStringOfMapWithIntValuesWithWhitespaceAsMap() {
        SchemaAndValue result = roundTrip(STRING_INT_MAP_SCHEMA, " { \"foo\" :  1234567890 , \"bar\" : 0,  \"baz\" : -987654321 }  ");
        assertEquals(STRING_INT_MAP_SCHEMA, result.schema());
        assertEquals(STRING_INT_MAP, result.value());
    }

    @Test
    public void shouldConvertListWithStringValues() {
        assertRoundTrip(STRING_LIST_SCHEMA, STRING_LIST_SCHEMA, STRING_LIST);
    }

    @Test
    public void shouldConvertListWithIntegerValues() {
        assertRoundTrip(INT_LIST_SCHEMA, INT_LIST_SCHEMA, INT_LIST);
    }

    /**
     * The parsed array has byte values and one int value, so we should return list with single unified type of integers.
     */
    @Test
    public void shouldConvertStringOfListWithOnlyNumericElementTypesIntoListOfLargestNumericType() {
        int thirdValue = Short.MAX_VALUE + 1;
        List<?> list = Values.convertToList(Schema.STRING_SCHEMA, "[1, 2, " + thirdValue + "]");
        assertEquals(3, list.size());
        assertEquals(1, ((Number) list.get(0)).intValue());
        assertEquals(2, ((Number) list.get(1)).intValue());
        assertEquals(thirdValue, ((Number) list.get(2)).intValue());
    }

    /**
     * The parsed array has byte values and one int value, so we should return list with single unified type of integers.
     */
    @Test
    public void shouldConvertStringOfListWithMixedElementTypesIntoListWithDifferentElementTypes() {
        String str = "[1, 2, \"three\"]";
        List<?> list = Values.convertToList(Schema.STRING_SCHEMA, str);
        assertEquals(3, list.size());
        assertEquals(1, ((Number) list.get(0)).intValue());
        assertEquals(2, ((Number) list.get(1)).intValue());
        assertEquals("three", list.get(2));
    }

    /**
     * We parse into different element types, but cannot infer a common element schema.
     */
    @Test
    public void shouldParseStringListWithMultipleElementTypesAndReturnListWithNoSchema() {
        String str = "[1, 2, 3, \"four\"]";
        SchemaAndValue result = Values.parseString(str);
        assertNull(result.schema());
        List<?> list = (List<?>) result.value();
        assertEquals(4, list.size());
        assertEquals(1, ((Number) list.get(0)).intValue());
        assertEquals(2, ((Number) list.get(1)).intValue());
        assertEquals(3, ((Number) list.get(2)).intValue());
        assertEquals("four", list.get(3));
    }

    /**
     * We can't infer or successfully parse into a different type, so this returns the same string.
     */
    @Test
    public void shouldParseStringListWithExtraDelimitersAndReturnString() {
        String str = "[1, 2, 3,,,]";
        SchemaAndValue result = Values.parseString(str);
        assertEquals(Type.STRING, result.schema().type());
        assertEquals(str, result.value());
    }

    /**
     * This is technically invalid JSON, and we don't want to simply ignore the blank elements.
     */
    @Test(expected = DataException.class)
    public void shouldFailToConvertToListFromStringWithExtraDelimiters() {
        Values.convertToList(Schema.STRING_SCHEMA, "[1, 2, 3,,,]");
    }

    /**
     * Schema of type ARRAY requires a schema for the values, but Connect has no union or "any" schema type.
     * Therefore, we can't represent this.
     */
    @Test(expected = DataException.class)
    public void shouldFailToConvertToListFromStringWithNonCommonElementTypeAndBlankElement() {
        Values.convertToList(Schema.STRING_SCHEMA, "[1, 2, 3, \"four\",,,]");
    }

    /**
     * This is technically invalid JSON, and we don't want to simply ignore the blank entry.
     */
    @Test(expected = DataException.class)
    public void shouldFailToParseStringOfMapWithIntValuesWithBlankEntry() {
        Values.convertToList(Schema.STRING_SCHEMA, " { \"foo\" :  1234567890 ,, \"bar\" : 0,  \"baz\" : -987654321 }  ");
    }

    /**
     * This is technically invalid JSON, and we don't want to simply ignore the malformed entry.
     */
    @Test(expected = DataException.class)
    public void shouldFailToParseStringOfMalformedMap() {
        Values.convertToList(Schema.STRING_SCHEMA, " { \"foo\" :  1234567890 , \"a\", \"bar\" : 0,  \"baz\" : -987654321 }  ");
    }

    /**
     * This is technically invalid JSON, and we don't want to simply ignore the blank entries.
     */
    @Test(expected = DataException.class)
    public void shouldFailToParseStringOfMapWithIntValuesWithOnlyBlankEntries() {
        Values.convertToList(Schema.STRING_SCHEMA, " { ,,  , , }  ");
    }

    /**
     * This is technically invalid JSON, and we don't want to simply ignore the blank entry.
     */
    @Test(expected = DataException.class)
    public void shouldFailToParseStringOfMapWithIntValuesWithBlankEntries() {
        Values.convertToList(Schema.STRING_SCHEMA, " { \"foo\" :  \"1234567890\" ,, \"bar\" : \"0\",  \"baz\" : \"boz\" }  ");
    }

    /**
     * Schema for Map requires a schema for key and value, but we have no key or value and Connect has no "any" type
     */
    @Test(expected = DataException.class)
    public void shouldFailToParseStringOfEmptyMap() {
        Values.convertToList(Schema.STRING_SCHEMA, " { }  ");
    }

    @Test
    public void shouldParseStringsWithoutDelimiters() {
        //assertParsed("");
        assertParsed("  ");
        assertParsed("simple");
        assertParsed("simple string");
        assertParsed("simple \n\t\bstring");
        assertParsed("'simple' string");
        assertParsed("si\\mple");
        assertParsed("si\\\\mple");
    }

    @Test
    public void shouldParseStringsWithEscapedDelimiters() {
        assertParsed("si\\\"mple");
        assertParsed("si\\{mple");
        assertParsed("si\\}mple");
        assertParsed("si\\]mple");
        assertParsed("si\\[mple");
        assertParsed("si\\:mple");
        assertParsed("si\\,mple");
    }

    @Test
    public void shouldParseStringsWithSingleDelimiter() {
        assertParsed("a{b", "a", "{", "b");
        assertParsed("a}b", "a", "}", "b");
        assertParsed("a[b", "a", "[", "b");
        assertParsed("a]b", "a", "]", "b");
        assertParsed("a:b", "a", ":", "b");
        assertParsed("a,b", "a", ",", "b");
        assertParsed("a\"b", "a", "\"", "b");
        assertParsed("{b", "{", "b");
        assertParsed("}b", "}", "b");
        assertParsed("[b", "[", "b");
        assertParsed("]b", "]", "b");
        assertParsed(":b", ":", "b");
        assertParsed(",b", ",", "b");
        assertParsed("\"b", "\"", "b");
        assertParsed("{", "{");
        assertParsed("}", "}");
        assertParsed("[", "[");
        assertParsed("]", "]");
        assertParsed(":", ":");
        assertParsed(",", ",");
        assertParsed("\"", "\"");
    }

    @Test
    public void shouldParseStringsWithMultipleDelimiters() {
        assertParsed("\"simple\" string", "\"", "simple", "\"", " string");
        assertParsed("a{bc}d", "a", "{", "bc", "}", "d");
        assertParsed("a { b c } d", "a ", "{", " b c ", "}", " d");
        assertParsed("a { b c } d", "a ", "{", " b c ", "}", " d");
    }

    @Test
    public void canConsume() {
    }

    @Test
    public void shouldParseBigIntegerAsDecimalWithZeroScale() {
        BigInteger value = BigInteger.valueOf(Long.MAX_VALUE).add(new BigInteger("1"));
        SchemaAndValue schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Decimal.schema(0), schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof BigDecimal);
        assertEquals(value, ((BigDecimal) schemaAndValue.value()).unscaledValue());
        value = BigInteger.valueOf(Long.MIN_VALUE).subtract(new BigInteger("1"));
        schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Decimal.schema(0), schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof BigDecimal);
        assertEquals(value, ((BigDecimal) schemaAndValue.value()).unscaledValue());
    }

    @Test
    public void shouldParseByteAsInt8() {
        Byte value = Byte.MAX_VALUE;
        SchemaAndValue schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Schema.INT8_SCHEMA, schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof Byte);
        assertEquals(value.byteValue(), ((Byte) schemaAndValue.value()).byteValue());
        value = Byte.MIN_VALUE;
        schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Schema.INT8_SCHEMA, schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof Byte);
        assertEquals(value.byteValue(), ((Byte) schemaAndValue.value()).byteValue());
    }

    @Test
    public void shouldParseShortAsInt16() {
        Short value = Short.MAX_VALUE;
        SchemaAndValue schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Schema.INT16_SCHEMA, schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof Short);
        assertEquals(value.shortValue(), ((Short) schemaAndValue.value()).shortValue());
        value = Short.MIN_VALUE;
        schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Schema.INT16_SCHEMA, schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof Short);
        assertEquals(value.shortValue(), ((Short) schemaAndValue.value()).shortValue());
    }

    @Test
    public void shouldParseIntegerAsInt32() {
        Integer value = Integer.MAX_VALUE;
        SchemaAndValue schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Schema.INT32_SCHEMA, schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof Integer);
        assertEquals(value.intValue(), ((Integer) schemaAndValue.value()).intValue());
        value = Integer.MIN_VALUE;
        schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Schema.INT32_SCHEMA, schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof Integer);
        assertEquals(value.intValue(), ((Integer) schemaAndValue.value()).intValue());
    }

    @Test
    public void shouldParseLongAsInt64() {
        Long value = Long.MAX_VALUE;
        SchemaAndValue schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Schema.INT64_SCHEMA, schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof Long);
        assertEquals(value.longValue(), ((Long) schemaAndValue.value()).longValue());
        value = Long.MIN_VALUE;
        schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Schema.INT64_SCHEMA, schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof Long);
        assertEquals(value.longValue(), ((Long) schemaAndValue.value()).longValue());
    }

    @Test
    public void shouldParseFloatAsFloat32() {
        Float value = Float.MAX_VALUE;
        SchemaAndValue schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Schema.FLOAT32_SCHEMA, schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof Float);
        assertEquals(value.floatValue(), ((Float) schemaAndValue.value()).floatValue(), 0);
        value = -Float.MAX_VALUE;
        schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Schema.FLOAT32_SCHEMA, schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof Float);
        assertEquals(value.floatValue(), ((Float) schemaAndValue.value()).floatValue(), 0);
    }

    @Test
    public void shouldParseDoubleAsFloat64() {
        Double value = Double.MAX_VALUE;
        SchemaAndValue schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Schema.FLOAT64_SCHEMA, schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof Double);
        assertEquals(value.doubleValue(), ((Double) schemaAndValue.value()).doubleValue(), 0);
        value = -Double.MAX_VALUE;
        schemaAndValue = Values.parseString(
            String.valueOf(value)
        );
        assertEquals(Schema.FLOAT64_SCHEMA, schemaAndValue.schema());
        assertTrue(schemaAndValue.value() instanceof Double);
        assertEquals(value.doubleValue(), ((Double) schemaAndValue.value()).doubleValue(), 0);
    }

    protected void assertParsed(String input) {
        assertParsed(input, input);
    }

    protected void assertParsed(String input, String... expectedTokens) {
        Parser parser = new Parser(input);
        if (!parser.hasNext()) {
            assertEquals(1, expectedTokens.length);
            assertTrue(expectedTokens[0].isEmpty());
            return;
        }

        for (String expectedToken : expectedTokens) {
            assertTrue(parser.hasNext());
            int position = parser.mark();
            assertEquals(expectedToken, parser.next());
            assertEquals(position + expectedToken.length(), parser.position());
            assertEquals(expectedToken, parser.previous());
            parser.rewindTo(position);
            assertEquals(position, parser.position());
            assertEquals(expectedToken, parser.next());
            int newPosition = parser.mark();
            assertEquals(position + expectedToken.length(), newPosition);
            assertEquals(expectedToken, parser.previous());
        }
        assertFalse(parser.hasNext());

        // Rewind and try consuming expected tokens ...
        parser.rewindTo(0);
        assertConsumable(parser, expectedTokens);

        // Parse again and try consuming expected tokens ...
        parser = new Parser(input);
        assertConsumable(parser, expectedTokens);
    }

    protected void assertConsumable(Parser parser, String... expectedTokens) {
        for (String expectedToken : expectedTokens) {
            if (!expectedToken.trim().isEmpty()) {
                int position = parser.mark();
                assertTrue(parser.canConsume(expectedToken.trim()));
                parser.rewindTo(position);
                assertTrue(parser.canConsume(expectedToken.trim(), true));
                parser.rewindTo(position);
                assertTrue(parser.canConsume(expectedToken, false));
            }
        }
    }

    protected SchemaAndValue roundTrip(Schema desiredSchema, String currentValue) {
        return roundTrip(desiredSchema, new SchemaAndValue(Schema.STRING_SCHEMA, currentValue));
    }


    protected SchemaAndValue roundTrip(Schema desiredSchema, SchemaAndValue input) {
        String serialized = Values.convertToString(input.schema(), input.value());
        if (input != null && input.value() != null) {
            assertNotNull(serialized);
        }
        if (desiredSchema == null) {
            desiredSchema = Values.inferSchema(input);
            assertNotNull(desiredSchema);
        }
        Object newValue = null;
        Schema newSchema = null;
        switch (desiredSchema.type()) {
            case STRING:
                newValue = Values.convertToString(Schema.STRING_SCHEMA, serialized);
                break;
            case INT8:
                newValue = Values.convertToByte(Schema.STRING_SCHEMA, serialized);
                break;
            case INT16:
                newValue = Values.convertToShort(Schema.STRING_SCHEMA, serialized);
                break;
            case INT32:
                newValue = Values.convertToInteger(Schema.STRING_SCHEMA, serialized);
                break;
            case INT64:
                newValue = Values.convertToLong(Schema.STRING_SCHEMA, serialized);
                break;
            case FLOAT32:
                newValue = Values.convertToFloat(Schema.STRING_SCHEMA, serialized);
                break;
            case FLOAT64:
                newValue = Values.convertToDouble(Schema.STRING_SCHEMA, serialized);
                break;
            case BOOLEAN:
                newValue = Values.convertToBoolean(Schema.STRING_SCHEMA, serialized);
                break;
            case ARRAY:
                newValue = Values.convertToList(Schema.STRING_SCHEMA, serialized);
                break;
            case MAP:
                newValue = Values.convertToMap(Schema.STRING_SCHEMA, serialized);
                break;
            case STRUCT:
                newValue = Values.convertToStruct(Schema.STRING_SCHEMA, serialized);
                break;
            case BYTES:
                fail("unexpected schema type");
                break;
        }
        newSchema = Values.inferSchema(newValue);
        return new SchemaAndValue(newSchema, newValue);
    }

    protected void assertRoundTrip(Schema schema, String value) {
        assertRoundTrip(schema, Schema.STRING_SCHEMA, value);
    }

    protected void assertRoundTrip(Schema schema, Schema currentSchema, Object value) {
        SchemaAndValue result = roundTrip(schema, new SchemaAndValue(currentSchema, value));

        if (value == null) {
            assertNull(result.schema());
            assertNull(result.value());
        } else {
            assertEquals(value, result.value());
            assertEquals(schema, result.schema());

            SchemaAndValue result2 = roundTrip(result.schema(), result);
            assertEquals(schema, result2.schema());
            assertEquals(value, result2.value());
            assertEquals(result, result2);
        }
    }

}
