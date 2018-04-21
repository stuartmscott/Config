package config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map.Entry;
import java.util.Set;

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

	@Test(timeout=1000)
	public void get() {
		Config config = new Config();
		Assert.assertNull(config.get(KEY1));
	}

	@Test(timeout=1000)
	public void getAll() {
		Config config = new Config();
		Assert.assertTrue(config.getAll(KEY1).isEmpty());
	}

	@Test(timeout=1000)
	public void put() {
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
		Set<Entry<String, String>> entries = config.getAll("k");
		Assert.assertEquals(2, entries.size());
	}

	@Test(timeout=1000)
	public void args() {
		String[] args = {
			KEY1 + "=" + VALUE1,
			KEY2 + "=" + VALUE2
		};
		Config config = Config.create(args);
		Assert.assertEquals(VALUE1, config.get(KEY1));
		Assert.assertEquals(VALUE2, config.get(KEY2));
	}

	@Test(timeout=1000)
	public void files() throws Exception {
		File file = folder.newFile("test");
		String[] files = new String[] {
			file.getAbsolutePath()
		};
		PrintWriter out = new PrintWriter(file);
		out.write(KEY1 + "=" + VALUE1 + "\n");
		out.write(KEY2 + "=" + VALUE2 + "\n");
		out.flush();
		out.close();
		Config config = Config.create(null, files, null, null, null);
		Assert.assertEquals(VALUE1, config.get(KEY1));
		Assert.assertEquals(VALUE2, config.get(KEY2));
	}

	@Test(timeout=1000)
	public void hierarchy() {
		Config parent = new Config();
		parent.put(KEY1, VALUE1);

		Config child = new Config(parent);
		child.put(KEY2, VALUE2);

		Assert.assertEquals(VALUE1, child.get(KEY1));
		Assert.assertEquals(VALUE2, child.get(KEY2));
	}

	@Test(timeout=1000)
	public void streams() {
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

	@Test(timeout=1000)
	public void hasBoolean() {
		Config config = new Config();
		Assert.assertFalse(config.hasBoolean(KEY1));
	}

	@Test(timeout=1000)
	public void getBoolean() {
		Config config = new Config();
		Assert.assertFalse(config.getBoolean(KEY1));
	}

	@Test(timeout=1000)
	public void putBoolean() {
		Config config = new Config();
		config.put(KEY1);
		Assert.assertTrue(config.getBoolean(KEY1));
		config.put(KEY2 + "=true");
		Assert.assertTrue(config.getBoolean(KEY2));
		config.put(KEY2 + "=false");
		Assert.assertFalse(config.getBoolean(KEY2));
	}

	@Test(timeout=1000)
	public void hasNumber() {
		Config config = new Config();
		Assert.assertFalse(config.hasNumber(KEY1));
	}

	@Test(timeout=1000)
	public void getNumber() {
		Config config = new Config();
		Assert.assertEquals(0.0, config.getNumber(KEY1), 0.0);
	}

	@Test(timeout=1000)
	public void putNumber() {
		Config config = new Config();
		config.put(KEY1);
		Assert.assertEquals(0.0, config.getNumber(KEY1), 0.0);
		config.put(KEY1, 123456);
		Assert.assertEquals(123456, config.getNumber(KEY1), 00);
		config.put(KEY2 + "=123");
		Assert.assertEquals(123, config.getNumber(KEY2), 00);
		config.put(KEY2 + "=.456");
		Assert.assertEquals(.456, config.getNumber(KEY2), 00);
	}

	@Test(timeout=1000)
	public void lookup() {
		Config parent = new Config();
		parent.put("Q1", QUERY1);
		parent.put("Q2", QUERY2);
		Config config = new Config(parent);
		config.put(KEY1, VALUE1);
		Assert.assertEquals(VALUE1, config.get("Q1"));
		Assert.assertEquals(QUERY2, config.get("Q2"));
	}
}
