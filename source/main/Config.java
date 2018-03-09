package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Config {
    public static Config create(String... args) {
        return create(args, null, null, null, null);
    }
    public static Config create(String[] args, String[] files, Config parent, InputStream i, OutputStream o) {
        final Config config = new Config(parent, i, o);
        if (args != null) for (int a = 0; a < args.length; a++) {
            config.put(args[a]);
        }
        if (files != null) for (int a = 0; a < files.length; a++) {
            try {
                final FileInputStream file = new FileInputStream(new File(files[a]));
                String line;
                while ((line = readLine(file)) != null) {
                    config.put(line);
                }
            } catch (Exception e) {
                System.err.println("Config Error: " + e.getMessage());
            }
        }
        return config;
    }
    public static String readLine(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = in.read(); i != -1 && i != '\n'; i = in.read()) {
            sb.append((char) i);
        }
        if (sb.length() > 0) {
            return sb.toString();
        }
        return null;
    }
    private final Map<String, String> configs = new ConcurrentHashMap<>();
    private final Config parent;
    private final InputStream in;
    private final OutputStream out;
    public Config() {
        this(null, null, null);
    }
    public Config(Config parent) {
        this(parent, null, null);
    }
    public Config(Config parent, InputStream in, OutputStream out) {
        this.parent = parent;
        this.in = in;
        this.out = out;
    }
    public boolean has(String key) {
        if (configs.containsKey(key)) {
            return true;
        }
        if (parent != null) {
            return parent.has(key);
        }
        return false;
    }
    public boolean hasBoolean(String key) {
        return has(key) && getBoolean(key);
    }
    public String get(String key) {
        return get(key, null);
    }
    public String get(String key, Set<String> options) {
        String value = configs.get(key);
        if (value == null || (options != null && !options.contains(value))) {
            if (parent == null) {
                if (out != null) try {
                    out.write(("Config: " + key + (options == null ? "?\n" : "? (" + options + ")\n")).getBytes());
                    if (in != null) value = readLine(in);
                    if (!key.equals("save") && value != null && hasBoolean("save")) {
                        put(key, value);
                    }
                } catch (Exception e) {/* ignored */}
            } else {
                value = parent.get(key, options);
            }
        }
        return value;
    }
    public Set<Entry<String, String>> getAll(String prefix) {
        Set<Entry<String, String>> entries = new HashSet<>();
        if (parent != null) {
            entries.addAll(parent.getAll(prefix));
        }
        for (Entry<String, String> e : configs.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                entries.add(e);
            }
        }
        return entries;
    }
    public boolean getBoolean(String key) {
        try {
            return Boolean.parseBoolean(get(key));
        } catch (Exception e) {
            return Boolean.parseBoolean(get(key, new HashSet<>(Arrays.asList("true", "false"))));
        }
    }
    public Config put(String line) {
        String[] parts = line.split("=");
        if (parts.length == 1) {
            put(parts[0], "true");
        } else if (parts.length == 2) {
            put(parts[0], parts[1]);
        }
        return this;
    }
    public Config put(String key, String value) {
        configs.put(key, value);
        return this;
    }
    public Config copy(String... keys) {
        Config c = new Config();
        for (String k : keys) {
            if (has(k)) {
                c.put(k, get(k));
            }
        }
        return c;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("\n");
        sb.append("configs: ");
        sb.append(configs);
        sb.append("\n");
        if (parent != null) {
            sb.append("parent: ");
            sb.append(parent.toString());
            sb.append("\n");
        }
        if (in != null) {
            sb.append("in: ");
            sb.append(in);
            sb.append("\n");
        }
        if (out != null) {
            sb.append("out: ");
            sb.append(out);
            sb.append("\n");
        }
        return sb.toString();
    }
}
