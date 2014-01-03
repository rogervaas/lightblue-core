package com.redhat.lightblue.query;

import java.util.List;

import java.math.BigInteger;
import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.skyscreamer.jsonassert.JSONAssert;

import com.redhat.lightblue.util.JsonUtils;

public class QueryParseTest {

    final String valueQuery1 = "{\"field\":\"x.y.z\", \"op\":\"$eq\", \"rvalue\":\"string\"}";
    final String valueQuery2 = "{\"field\":\"x.y.1\", \"op\":\"$gte\", \"rvalue\":1}";
    final String valueQuery3 = "{\"field\":\"x.y.1\", \"op\":\"$neq\", \"rvalue\":12345678901234567890}";
    final String valueQuery4 = "{\"field\":\"x.y.1\", \"op\":\"$eq\", \"rvalue\":true}";
    final String valueQuery5 = "{\"field\":\"x.y.-1.x\", \"op\":\"$neq\", \"rvalue\":12345678901234567890123456789123456789.123456}";

    final String fieldQuery1 = "{\"field\":\"x.-1.y\", \"op\":\"$eq\", \"rfield\":\"y.z.-1\"}";
    final String fieldQuery2 = "{\"field\":\"x.1.y\", \"op\":\"$neq\", \"rfield\":\"y\"}";

    final String naryQuery1 = "{\"field\":\"x.y\", \"op\":\"$in\", \"values\":[1,2,3,4,5]}";
    final String naryQuery2 = "{\"field\":\"x.y.x\", \"op\":\"$nin\", \"values\":[\"x\",\"y\",\"z\"]}";

    final String regexQuery1 = "{\"field\":\"x.y\", \"regex\":\"*pat*\"}";
    final String regexQuery2 = "{\"field\":\"x.y\", \"regex\":\"*pat*\",\"case_insensitive\":true}";

    final String unaryQuery1 = "{ \"$not\": " + valueQuery1 + "}";
    final String unaryQuery2 = "{ \"$not\": " + regexQuery1 + "}";

    final String naryLogQuery1 = "{ \"$and\" : [" + valueQuery1 + "," + fieldQuery1 + "," + naryQuery1 + "," + unaryQuery1 + "]}";

    final String arrContains1 = "{\"array\":\"x.y\", \"contains\":\"$any\", \"values\":[1,2,3,4,5]}";
    final String arrContains2 = "{\"array\":\"x.y\", \"contains\":\"$any\", \"values\":[\"x\", \"y\"]}";

    final String arrMatch1 = "{\"array\":\"x.y\",\"elemMatch\":" + regexQuery1 + "}";

    interface NestedTest {
        public void test(QueryExpression x);
    }

    @Test
    public void testValueQueries() throws Exception {
        testValueComparisonExpression(valueQuery1, "x.y.z", BinaryComparisonOperator._eq, "string");
        testValueComparisonExpression(valueQuery2, "x.y.1", BinaryComparisonOperator._gte, new Integer(1));
        testValueComparisonExpression(valueQuery3, "x.y.1", BinaryComparisonOperator._neq, new BigInteger("12345678901234567890"));
        testValueComparisonExpression(valueQuery4, "x.y.1", BinaryComparisonOperator._eq, Boolean.TRUE);
        testValueComparisonExpression(valueQuery5, "x.y.-1.x", BinaryComparisonOperator._neq, new BigDecimal("12345678901234567890123456789123456789.123456"));
    }

    @Test
    public void testFieldQueries() throws Exception {
        testFieldComparisonExpression(fieldQuery1, "x.-1.y", BinaryComparisonOperator._eq, "y.z.-1");
        testFieldComparisonExpression(fieldQuery2, "x.1.y", BinaryComparisonOperator._neq, "y");
    }

    @Test
    public void testNaryQueries() throws Exception {
        testNaryRelationalExpression(naryQuery1, "x.y", NaryRelationalOperator._in, 1, 2, 3, 4, 5);
        testNaryRelationalExpression(naryQuery2, "x.y.x", NaryRelationalOperator._not_in, "x", "y", "z");
    }

    @Test
    public void testRegexQueries() throws Exception {
        testRegexQuery(regexQuery1, "x.y", "*pat*", false, false, false, false);
        testRegexQuery(regexQuery2, "x.y", "*pat*", true, false, false, false);
    }

    private static NestedTest u1NestedTest = new NestedTest() {
        public void test(QueryExpression x) {
            asserts((ValueComparisonExpression) x, "x.y.z", BinaryComparisonOperator._eq, "string");
        }
    };

    @Test
    public void testUnaries() throws Exception {
        testUnaryQuery(unaryQuery1, u1NestedTest);
        testUnaryQuery(unaryQuery2, new NestedTest() {
            public void test(QueryExpression x) {
                asserts((RegexMatchExpression) x, "x.y", "*pat*", false, false, false, false);
            }
        });
    }

    @Test
    public void testNaries() throws Exception {
        testNaryQuery(naryLogQuery1, new NestedTest() {
            public void test(QueryExpression x) {
                asserts((ValueComparisonExpression) x, "x.y.z", BinaryComparisonOperator._eq, "string");
            }
        },
                new NestedTest() {
                    public void test(QueryExpression x) {
                        asserts((FieldComparisonExpression) x, "x.-1.y", BinaryComparisonOperator._eq, "y.z.-1");
                    }
                },
                new NestedTest() {
                    public void test(QueryExpression x) {
                        asserts((NaryRelationalExpression) x, "x.y", NaryRelationalOperator._in, 1, 2, 3, 4, 5);
                    }
                },
                new NestedTest() {
                    public void test(QueryExpression x) {
                        asserts((UnaryLogicalExpression) x, u1NestedTest);
                    }
                });
    }

    @Test
    public void testArrContains() throws Exception {
        testArrContains(arrContains1, "x.y", ContainsOperator._any, 1, 2, 3, 4, 5);
        testArrContains(arrContains2, "x.y", ContainsOperator._any, "x", "y");
    }

    @Test
    public void testArrMatch() throws Exception {
        testArrMatch(arrMatch1, "x.y", new NestedTest() {
            public void test(QueryExpression x) {
                asserts((RegexMatchExpression) x, "x.y", "*pat*", false, false, false, false);
            }
        });
    }

    private void testValueComparisonExpression(String q,
                                               String field,
                                               BinaryComparisonOperator op,
                                               Object value)
            throws Exception {
        QueryExpression query = QueryExpression.fromJson(JsonUtils.json(q));
        Assert.assertTrue(query instanceof ValueComparisonExpression);
        asserts((ValueComparisonExpression) query, field, op, value);
        JSONAssert.assertEquals(q, QueryExpression.fromJson(JsonUtils.json(q)).toString(), false);
    }

    private static void asserts(ValueComparisonExpression x,
                                String field,
                                BinaryComparisonOperator op,
                                Object value) {
        Assert.assertEquals(field, x.getField().toString());
        Assert.assertEquals(op, x.getOp());
        Assert.assertTrue(value.getClass().equals(x.getRvalue().getValue().getClass()));
        Assert.assertEquals(value.toString(), x.getRvalue().getValue().toString());
    }

    private void testFieldComparisonExpression(String q,
                                               String field,
                                               BinaryComparisonOperator op,
                                               String rfield)
            throws Exception {
        QueryExpression query = QueryExpression.fromJson(JsonUtils.json(q));
        Assert.assertTrue(query instanceof FieldComparisonExpression);
        asserts((FieldComparisonExpression) query, field, op, rfield);
        JSONAssert.assertEquals(q, QueryExpression.fromJson(JsonUtils.json(q)).toString(), false);
    }

    private void asserts(FieldComparisonExpression x, String field, BinaryComparisonOperator op, String rfield) {
        Assert.assertEquals(field, x.getField().toString());
        Assert.assertEquals(op, x.getOp());
        Assert.assertEquals(rfield, x.getRfield().toString());
    }

    private void testNaryRelationalExpression(String q,
                                              String field,
                                              NaryRelationalOperator op,
                                              Object... value)
            throws Exception {
        QueryExpression query = QueryExpression.fromJson(JsonUtils.json(q));
        Assert.assertTrue(query instanceof NaryRelationalExpression);
        asserts((NaryRelationalExpression) query, field, op, value);
        JSONAssert.assertEquals(q, QueryExpression.fromJson(JsonUtils.json(q)).toString(), false);
    }

    private static void asserts(NaryRelationalExpression x, String field, NaryRelationalOperator op, Object... value) {
        Assert.assertEquals(field, x.getField().toString());
        Assert.assertEquals(op, x.getOp());
        Assert.assertEquals(value.length, x.getValues().size());
        for (int i = 0; i < value.length; i++) {
            Assert.assertEquals(value[i].getClass(), x.getValues().get(i).getValue().getClass());
        }
    }

    private void testRegexQuery(String q,
                                String field,
                                String regex,
                                boolean c,
                                boolean m,
                                boolean x,
                                boolean d)
            throws Exception {
        QueryExpression query = QueryExpression.fromJson(JsonUtils.json(q));
        Assert.assertTrue(query instanceof RegexMatchExpression);
        RegexMatchExpression mx = (RegexMatchExpression) query;
        asserts(mx, field, regex, c, m, x, d);
        JSONAssert.assertEquals(q, QueryExpression.fromJson(JsonUtils.json(q)).toString(), false);
    }

    private static void asserts(RegexMatchExpression x,
                                String field,
                                String regex,
                                boolean c,
                                boolean m,
                                boolean ox,
                                boolean d) {
        Assert.assertEquals(field, x.getField().toString());
        Assert.assertEquals(regex, x.getRegex());
        Assert.assertEquals(c, x.isCaseInsensitive());
        Assert.assertEquals(m, x.isMultiline());
        Assert.assertEquals(ox, x.isExtended());
        Assert.assertEquals(d, x.isDotAll());
    }

    private void testUnaryQuery(String q, NestedTest t) throws Exception {
        QueryExpression query = QueryExpression.fromJson(JsonUtils.json(q));
        Assert.assertTrue(query instanceof UnaryLogicalExpression);
        UnaryLogicalExpression x = (UnaryLogicalExpression) query;
        JSONAssert.assertEquals(q, QueryExpression.fromJson(JsonUtils.json(q)).toString(), false);
    }

    private static void asserts(UnaryLogicalExpression x, NestedTest t) {
        t.test(x.getQuery());
    }

    private void testNaryQuery(String q, NestedTest... t) throws Exception {
        QueryExpression query = QueryExpression.fromJson(JsonUtils.json(q));
        Assert.assertTrue(query instanceof NaryLogicalExpression);
        NaryLogicalExpression x = (NaryLogicalExpression) query;
        List<QueryExpression> queries = x.getQueries();
        Assert.assertEquals(t.length, queries.size());
        for (int i = 0; i < t.length; i++) {
            t[i].test(queries.get(i));
        }
        JSONAssert.assertEquals(q, QueryExpression.fromJson(JsonUtils.json(q)).toString(), false);
    }

    private void testArrContains(String q, String field, ContainsOperator op, Object... value) throws Exception {
        QueryExpression query = QueryExpression.fromJson(JsonUtils.json(q));
        Assert.assertTrue(query instanceof ArrayContainsExpression);
        asserts((ArrayContainsExpression) query, field, op, value);
        JSONAssert.assertEquals(q, QueryExpression.fromJson(JsonUtils.json(q)).toString(), false);
    }

    private void asserts(ArrayContainsExpression x, String field, ContainsOperator op, Object... value) {
        Assert.assertEquals(field, x.getArray().toString());
        Assert.assertEquals(op, x.getOp());
        Assert.assertEquals(value.length, x.getValues().size());
        for (int i = 0; i < value.length; i++) {
            Assert.assertEquals(value[i].getClass(), x.getValues().get(i).getValue().getClass());
            Assert.assertEquals(value[i].toString(), x.getValues().get(i).getValue().toString());
        }
    }

    private void testArrMatch(String q, String field, NestedTest t) throws Exception {
        QueryExpression query = QueryExpression.fromJson(JsonUtils.json(q));
        Assert.assertTrue(query instanceof ArrayMatchExpression);
        asserts((ArrayMatchExpression) query, field, t);
        JSONAssert.assertEquals(q, QueryExpression.fromJson(JsonUtils.json(q)).toString(), false);
    }

    private void asserts(ArrayMatchExpression x, String field, NestedTest t) {
        Assert.assertEquals(field, x.getArray().toString());
        t.test(x.getElemMatch());
    }
}
