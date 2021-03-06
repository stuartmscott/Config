/*
 * Copyright 2018 Stuart Scott
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ConfigTest {

    public static final String KEY1 = "key1";
    public static final String KEY2 = "key2";
    public static final String VALUE1 = "value1";
    public static final String VALUE2 = "value2";

    public static final String QUERY1 = "?<" + KEY1 + ">";
    public static final String QUERY2 = "?<" + KEY2 + ">";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void get() throws Exception {
        Config config = new Config();
        Assert.assertNull(config.get(KEY1));
    }

    @Test
    public void getAll() throws Exception {
        Config config = new Config();
        Assert.assertTrue(config.getAll(KEY1).isEmpty());
    }

    @Test
    public void put() throws Exception {
        Config config = new Config();
        // Insert
        config.put(KEY1, VALUE1);
        Assert.assertEquals(VALUE1, config.get(KEY1));
        config.put(KEY2 + "=" + VALUE2);
        Assert.assertEquals(VALUE2, config.get(KEY2));
        // Replace
        config.put(KEY1, VALUE2);
        Assert.assertEquals(VALUE2, config.get(KEY1));
        config.put(KEY2 + "=" + VALUE1);
        Assert.assertEquals(VALUE1, config.get(KEY2));
        // Retrieve all
        Map<String, String> entries = config.getAll("k");
        Assert.assertEquals(2, entries.size());
    }

    @Test
    public void args() throws Exception {
        String[] args = {
            KEY1 + "=" + VALUE1,
            KEY2 + "=" + VALUE2
        };
        Config config = Config.create(args);
        Assert.assertEquals(VALUE1, config.get(KEY1));
        Assert.assertEquals(VALUE2, config.get(KEY2));
    }

    @Test
    public void files() throws Exception {
        File fileA = folder.newFile("testA");
        PrintWriter out = new PrintWriter(fileA);
        out.write(KEY1 + "=" + VALUE1 + "\n");
        out.write(KEY2 + "=" + VALUE2 + "\n");
        out.flush();
        out.close();
        Config configA = Config.create(fileA);
        Assert.assertEquals(VALUE1, configA.get(KEY1));
        Assert.assertEquals(VALUE2, configA.get(KEY2));

        File fileB = folder.newFile("testB");
        configA.writeAllLines(fileB);
        Config configB = new Config();
        configB.readAllLines(fileB);
        Assert.assertEquals(VALUE1, configB.get(KEY1));
        Assert.assertEquals(VALUE2, configB.get(KEY2));
    }

    @Test
    public void hierarchy() throws Exception {
        Config parent = new Config();
        parent.put(KEY1, VALUE1);

        Config child = new Config(parent);
        child.put(KEY2, VALUE2);

        Assert.assertEquals(VALUE1, child.get(KEY1));
        Assert.assertEquals(VALUE2, child.get(KEY2));
    }

    @Test
    public void streams() throws Exception {
        // Give 1 & 2
        String input = VALUE1 + "\n" + VALUE2 + "\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Config config = new Config(null, in, out);
        // Expect 1 & 2
        Assert.assertEquals(VALUE1, config.get(KEY1));
        Assert.assertEquals("Config: " + KEY1 + "?\n", new String(out.toByteArray()));// Should have asked
        Assert.assertEquals(VALUE2, config.get(KEY1));// Should have asked the stream instead of remembering

        // Give 1 & 2
        input = VALUE1 + "\n" + VALUE2 + "\n";
        in = new ByteArrayInputStream(input.getBytes());
        out = new ByteArrayOutputStream();
        config = new Config(null, in, out);
        config.put("save");
        // Expect 1 & 1
        Assert.assertEquals(VALUE1, config.get(KEY1));
        Assert.assertEquals("Config: " + KEY1 + "?\n", new String(out.toByteArray()));// Should have asked
        Assert.assertEquals(VALUE1, config.get(KEY1));// Should have remembered instead of asking stream
    }

    @Test
    public void hasBoolean() throws Exception {
        Config config = new Config();
        Assert.assertFalse(config.hasBoolean(KEY1));
    }

    @Test
    public void getBoolean() throws Exception {
        Config config = new Config();
        Assert.assertFalse(config.getBoolean(KEY1));
    }

    @Test
    public void putBoolean() throws Exception {
        Config config = new Config();
        config.put(KEY1);
        Assert.assertTrue(config.getBoolean(KEY1));
        config.put(KEY2 + "=true");
        Assert.assertTrue(config.getBoolean(KEY2));
        config.put(KEY2 + "=false");
        Assert.assertFalse(config.getBoolean(KEY2));
    }

    @Test
    public void hasNumber() throws Exception {
        Config config = new Config();
        Assert.assertFalse(config.hasNumber(KEY1));
    }

    @Test
    public void getNumber() throws Exception {
        Config config = new Config();
        Assert.assertEquals(0.0, config.getNumber(KEY1), 0.0);
    }

    @Test
    public void putNumber() throws Exception {
        Config config = new Config();
        config.put(KEY1);
        Assert.assertEquals(0.0, config.getNumber(KEY1), 0.0);
        config.put(KEY1, 123456);
        Assert.assertEquals(123456, config.getNumber(KEY1), 0.0);
        config.put(KEY2 + "=123");
        Assert.assertEquals(123, config.getNumber(KEY2), 0.0);
        config.put(KEY2 + "=.456");
        Assert.assertEquals(.456, config.getNumber(KEY2), 0.0);
    }

    @Test
    public void lookup() throws Exception {
        Config parent = new Config();
        parent.put("Q1", QUERY1);
        parent.put("Q2", QUERY2);
        Config config = new Config(parent);
        config.put(KEY1, VALUE1);
        Assert.assertEquals(VALUE1, config.get("Q1"));
        Assert.assertEquals(QUERY2, config.get("Q2"));
    }
}
