/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.painless;

/**
 * Tests binary operators across different types
 */
// TODO: NaN/Inf/overflow/...
public class BinaryOperatorTests extends ScriptTestCase {

    // TODO: move to per-type tests and test for each type
    public void testBasics() {
        assertEquals(2.25F / 1.5F, exec("return 2.25F / 1.5F;"));
        assertEquals(2.25F % 1.5F, exec("return 2.25F % 1.5F;"));
        assertEquals(2 - 1, exec("return 2 - 1;"));
        assertEquals(1 << 2, exec("return 1 << 2;"));
        assertEquals(4 >> 2, exec("return 4 >> 2;"));
        assertEquals(-1 >>> 29, exec("return -1 >>> 29;"));
        assertEquals(5 & 3, exec("return 5 & 3;"));
        assertEquals(5 & 3L, exec("return 5 & 3L;"));
        assertEquals(5L & 3, exec("return 5L & 3;"));
        assertEquals(5 | 3, exec("return 5 | 3;"));
        assertEquals(5L | 3, exec("return 5L | 3;"));
        assertEquals(5 | 3L, exec("return 5 | 3L;"));
        assertEquals(9 ^ 3, exec("return 9 ^ 3;"));
        assertEquals(9L ^ 3, exec("return 9L ^ 3;"));
        assertEquals(9 ^ 3L, exec("return 9 ^ 3L;"));
    }

    public void testLongShifts() {
        // note: we always promote the results of shifts too (unlike java)
        assertEquals(1L << 2, exec("long x = 1L; int y = 2; return x << y;"));
        assertEquals(1L << 2L, exec("long x = 1L; long y = 2L; return x << y;"));
        assertEquals(4L >> 2L, exec("long x = 4L; long y = 2L; return x >> y;"));
        assertEquals(4L >> 2, exec("long x = 4L; int y = 2; return x >> y;"));
        assertEquals(-1L >>> 29, exec("long x = -1L; int y = 29; return x >>> y;"));
        assertEquals(-1L >>> 29L, exec("long x = -1L; long y = 29L; return x >>> y;"));
    }

    public void testLongShiftsConst() {
        assertEquals(1L << 2, exec("return 1L << 2;"));
        assertEquals(1 << 2L, exec("return 1 << 2L;"));
        assertEquals(4 >> 2L, exec("return 4 >> 2L;"));
        assertEquals(4L >> 2, exec("return 4L >> 2;"));
        assertEquals(-1L >>> 29, exec("return -1L >>> 29;"));
        assertEquals(-1 >>> 29L, exec("return -1 >>> 29L;"));
    }

    public void testMixedTypes() {
        assertEquals(8, exec("int x = 4; char y = 2; return x*y;"));
        assertEquals(0.5, exec("double x = 1; float y = 2; return x / y;"));
        assertEquals(1, exec("int x = 3; int y = 2; return x % y;"));
        assertEquals(3.0, exec("double x = 1; byte y = 2; return x + y;"));
        assertEquals(-1, exec("int x = 1; char y = 2; return x - y;"));
        assertEquals(4, exec("int x = 1; char y = 2; return x << y;"));
        assertEquals(-1, exec("int x = -1; char y = 29; return x >> y;"));
        assertEquals(3, exec("int x = -1; char y = 30; return x >>> y;"));
        assertEquals(1L, exec("int x = 5; long y = 3; return x & y;"));
        assertEquals(7, exec("short x = 5; byte y = 3; return x | y;"));
        assertEquals(10, exec("short x = 9; char y = 3; return x ^ y;"));
    }

    public void testBinaryPromotion() throws Exception {
        // byte/byte
        assertEquals((byte)1 + (byte)1, exec("byte x = 1; byte y = 1; return x+y;"));
        // byte/char
        assertEquals((byte)1 + (char)1, exec("byte x = 1; char y = 1; return x+y;"));
        // byte/short
        assertEquals((byte)1 + (short)1, exec("byte x = 1; short y = 1; return x+y;"));
        // byte/int
        assertEquals((byte)1 + 1, exec("byte x = 1; int y = 1; return x+y;"));
        // byte/long
        assertEquals((byte)1 + 1L, exec("byte x = 1; long y = 1; return x+y;"));
        // byte/float
        assertEquals((byte)1 + 1F, exec("byte x = 1; float y = 1; return x+y;"));
        // byte/double
        assertEquals((byte)1 + 1.0, exec("byte x = 1; double y = 1; return x+y;"));

        // char/byte
        assertEquals((char)1 + (byte)1, exec("char x = 1; byte y = 1; return x+y;"));
        // char/char
        assertEquals((char)1 + (char)1, exec("char x = 1; char y = 1; return x+y;"));
        // char/short
        assertEquals((char)1 + (short)1, exec("char x = 1; short y = 1; return x+y;"));
        // char/int
        assertEquals((char)1 + 1, exec("char x = 1; int y = 1; return x+y;"));
        // char/long
        assertEquals((char)1 + 1L, exec("char x = 1; long y = 1; return x+y;"));
        // char/float
        assertEquals((char)1 + 1F, exec("char x = 1; float y = 1; return x+y;"));
        // char/double
        assertEquals((char)1 + 1.0, exec("char x = 1; double y = 1; return x+y;"));

        // short/byte
        assertEquals((short)1 + (byte)1, exec("short x = 1; byte y = 1; return x+y;"));
        // short/char
        assertEquals((short)1 + (char)1, exec("short x = 1; char y = 1; return x+y;"));
        // short/short
        assertEquals((short)1 + (short)1, exec("short x = 1; short y = 1; return x+y;"));
        // short/int
        assertEquals((short)1 + 1, exec("short x = 1; int y = 1; return x+y;"));
        // short/long
        assertEquals((short)1 + 1L, exec("short x = 1; long y = 1; return x+y;"));
        // short/float
        assertEquals((short)1 + 1F, exec("short x = 1; float y = 1; return x+y;"));
        // short/double
        assertEquals((short)1 + 1.0, exec("short x = 1; double y = 1; return x+y;"));

        // int/byte
        assertEquals(1 + (byte)1, exec("int x = 1; byte y = 1; return x+y;"));
        // int/char
        assertEquals(1 + (char)1, exec("int x = 1; char y = 1; return x+y;"));
        // int/short
        assertEquals(1 + (short)1, exec("int x = 1; short y = 1; return x+y;"));
        // int/int
        assertEquals(1 + 1, exec("int x = 1; int y = 1; return x+y;"));
        // int/long
        assertEquals(1 + 1L, exec("int x = 1; long y = 1; return x+y;"));
        // int/float
        assertEquals(1 + 1F, exec("int x = 1; float y = 1; return x+y;"));
        // int/double
        assertEquals(1 + 1.0, exec("int x = 1; double y = 1; return x+y;"));

        // long/byte
        assertEquals(1L + (byte)1, exec("long x = 1; byte y = 1; return x+y;"));
        // long/char
        assertEquals(1L + (char)1, exec("long x = 1; char y = 1; return x+y;"));
        // long/short
        assertEquals(1L + (short)1, exec("long x = 1; short y = 1; return x+y;"));
        // long/int
        assertEquals(1L + 1, exec("long x = 1; int y = 1; return x+y;"));
        // long/long
        assertEquals(1L + 1L, exec("long x = 1; long y = 1; return x+y;"));
        // long/float
        assertEquals(1L + 1F, exec("long x = 1; float y = 1; return x+y;"));
        // long/double
        assertEquals(1L + 1.0, exec("long x = 1; double y = 1; return x+y;"));

        // float/byte
        assertEquals(1F + (byte)1, exec("float x = 1; byte y = 1; return x+y;"));
        // float/char
        assertEquals(1F + (char)1, exec("float x = 1; char y = 1; return x+y;"));
        // float/short
        assertEquals(1F + (short)1, exec("float x = 1; short y = 1; return x+y;"));
        // float/int
        assertEquals(1F + 1, exec("float x = 1; int y = 1; return x+y;"));
        // float/long
        assertEquals(1F + 1L, exec("float x = 1; long y = 1; return x+y;"));
        // float/float
        assertEquals(1F + 1F, exec("float x = 1; float y = 1; return x+y;"));
        // float/double
        assertEquals(1F + 1.0, exec("float x = 1; double y = 1; return x+y;"));

        // double/byte
        assertEquals(1.0 + (byte)1, exec("double x = 1; byte y = 1; return x+y;"));
        // double/char
        assertEquals(1.0 + (char)1, exec("double x = 1; char y = 1; return x+y;"));
        // double/short
        assertEquals(1.0 + (short)1, exec("double x = 1; short y = 1; return x+y;"));
        // double/int
        assertEquals(1.0 + 1, exec("double x = 1; int y = 1; return x+y;"));
        // double/long
        assertEquals(1.0 + 1L, exec("double x = 1; long y = 1; return x+y;"));
        // double/float
        assertEquals(1.0 + 1F, exec("double x = 1; float y = 1; return x+y;"));
        // double/double
        assertEquals(1.0 + 1.0, exec("double x = 1; double y = 1; return x+y;"));
    }

    public void testBinaryPromotionConst() throws Exception {
        // byte/byte
        assertEquals((byte)1 + (byte)1, exec("return (byte)1 + (byte)1;"));
        // byte/char
        assertEquals((byte)1 + (char)1, exec("return (byte)1 + (char)1;"));
        // byte/short
        assertEquals((byte)1 + (short)1, exec("return (byte)1 + (short)1;"));
        // byte/int
        assertEquals((byte)1 + 1, exec("return (byte)1 + 1;"));
        // byte/long
        assertEquals((byte)1 + 1L, exec("return (byte)1 + 1L;"));
        // byte/float
        assertEquals((byte)1 + 1F, exec("return (byte)1 + 1F;"));
        // byte/double
        assertEquals((byte)1 + 1.0, exec("return (byte)1 + 1.0;"));

        // char/byte
        assertEquals((char)1 + (byte)1, exec("return (char)1 + (byte)1;"));
        // char/char
        assertEquals((char)1 + (char)1, exec("return (char)1 + (char)1;"));
        // char/short
        assertEquals((char)1 + (short)1, exec("return (char)1 + (short)1;"));
        // char/int
        assertEquals((char)1 + 1, exec("return (char)1 + 1;"));
        // char/long
        assertEquals((char)1 + 1L, exec("return (char)1 + 1L;"));
        // char/float
        assertEquals((char)1 + 1F, exec("return (char)1 + 1F;"));
        // char/double
        assertEquals((char)1 + 1.0, exec("return (char)1 + 1.0;"));

        // short/byte
        assertEquals((short)1 + (byte)1, exec("return (short)1 + (byte)1;"));
        // short/char
        assertEquals((short)1 + (char)1, exec("return (short)1 + (char)1;"));
        // short/short
        assertEquals((short)1 + (short)1, exec("return (short)1 + (short)1;"));
        // short/int
        assertEquals((short)1 + 1, exec("return (short)1 + 1;"));
        // short/long
        assertEquals((short)1 + 1L, exec("return (short)1 + 1L;"));
        // short/float
        assertEquals((short)1 + 1F, exec("return (short)1 + 1F;"));
        // short/double
        assertEquals((short)1 + 1.0, exec("return (short)1 + 1.0;"));

        // int/byte
        assertEquals(1 + (byte)1, exec("return 1 + (byte)1;"));
        // int/char
        assertEquals(1 + (char)1, exec("return 1 + (char)1;"));
        // int/short
        assertEquals(1 + (short)1, exec("return 1 + (short)1;"));
        // int/int
        assertEquals(1 + 1, exec("return 1 + 1;"));
        // int/long
        assertEquals(1 + 1L, exec("return 1 + 1L;"));
        // int/float
        assertEquals(1 + 1F, exec("return 1 + 1F;"));
        // int/double
        assertEquals(1 + 1.0, exec("return 1 + 1.0;"));

        // long/byte
        assertEquals(1L + (byte)1, exec("return 1L + (byte)1;"));
        // long/char
        assertEquals(1L + (char)1, exec("return 1L + (char)1;"));
        // long/short
        assertEquals(1L + (short)1, exec("return 1L + (short)1;"));
        // long/int
        assertEquals(1L + 1, exec("return 1L + 1;"));
        // long/long
        assertEquals(1L + 1L, exec("return 1L + 1L;"));
        // long/float
        assertEquals(1L + 1F, exec("return 1L + 1F;"));
        // long/double
        assertEquals(1L + 1.0, exec("return 1L + 1.0;"));

        // float/byte
        assertEquals(1F + (byte)1, exec("return 1F + (byte)1;"));
        // float/char
        assertEquals(1F + (char)1, exec("return 1F + (char)1;"));
        // float/short
        assertEquals(1F + (short)1, exec("return 1F + (short)1;"));
        // float/int
        assertEquals(1F + 1, exec("return 1F + 1;"));
        // float/long
        assertEquals(1F + 1L, exec("return 1F + 1L;"));
        // float/float
        assertEquals(1F + 1F, exec("return 1F + 1F;"));
        // float/double
        assertEquals(1F + 1.0, exec("return 1F + 1.0;"));

        // double/byte
        assertEquals(1.0 + (byte)1, exec("return 1.0 + (byte)1;"));
        // double/char
        assertEquals(1.0 + (char)1, exec("return 1.0 + (char)1;"));
        // double/short
        assertEquals(1.0 + (short)1, exec("return 1.0 + (short)1;"));
        // double/int
        assertEquals(1.0 + 1, exec("return 1.0 + 1;"));
        // double/long
        assertEquals(1.0 + 1L, exec("return 1.0 + 1L;"));
        // double/float
        assertEquals(1.0 + 1F, exec("return 1.0 + 1F;"));
        // double/double
        assertEquals(1.0 + 1.0, exec("return 1.0 + 1.0;"));
    }
}
