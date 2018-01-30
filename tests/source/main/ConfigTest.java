package main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ConfigTest {
	public static final String KEY1 = "key1";
	public static final String KEY2 = "key2";
	public static final String VALUE1 = "value1";
	public static final String VALUE2 = "value2";

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test(timeout=1000)
	public void get() {
		Config config = new Config();
		Assert.assertNull(config.get(KEY1));
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
	public void getBoolean() {
		Config config = new Config();
		Assert.assertFalse(config.getBoolean(KEY1));
	}

	@Test(timeout=1000)
	public void putBoolean() {
		Config config = new Config();
		config.put(KEY1);
		Assert.assertTrue(config.getBoolean(KEY1));
		config.put(KEY2 + "=false");
		Assert.assertFalse(config.getBoolean(KEY2));
	}
}
