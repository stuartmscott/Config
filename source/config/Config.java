package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Config {
    /**
    * Creates a new config with the given args.
    */
    public static Config create(String... args) {
        return create(args, null, null, null, null);
    }
    /**
    * Creates a new config with the given args, files, parent, input-, and output-streams.
    */
    public static Config create(String[] args, String[] files, Config parent, InputStream i, OutputStream o) {
        final Config config = new Config(parent, i, o);
        config.put(args);
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
    /**
    * Reads a line from the given inputstream.
    */
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
    // TODO consider Map<ByteString, ByteString>
    private final Map<String, String> configs = new ConcurrentHashMap<>();
    private final Config parent;
    private final InputStream in;
    private final OutputStream out;
    /**
    * Creates an empty config.
    */
    public Config() {
        this(null, null, null);
    }
    /**
    * Creates an config with the given parent.
    */
    public Config(Config parent) {
        this(parent, null, null);
    }
    /**
    * Creates an config with the given parent, input-, and output-stream.
    */
    public Config(Config parent, InputStream in, OutputStream out) {
        this.parent = parent;
        this.in = in;
        this.out = out;
    }
    /**
    * Returns true iff this config or its hierarchy has the given key.
    */
    public boolean has(String key) {
        if (configs.containsKey(key)) {
            return true;
        }
        if (parent != null) {
            return parent.has(key);
        }
        return false;
    }
    /**
    * Returns true iff key == "true"
    * in this config or its hierarchy.
    */
    public boolean hasBoolean(String key) {
        return has(key) && getBoolean(key);
    }
    /**
    * Returns true iff key is mapped to a number,
    * which can exist in a simple math equation,
    * in this config or its hierarchy.
    */
    public boolean hasNumber(String key) {
        return has(key) && getNumber(key) * 0 == 0;
    }
    /**
    * Returns the value of the given key in this config or its hierarchy.
    */
    public String get(String key) {
        return get(key, null);
    }
    /**
    * Returns the value of the given key in this config or its hierarchy
    * iff the set of options is null or the value is an element of the set.
    */
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
        if (value != null && value.startsWith("?")) {
            return lookup(value);
        }
        return value;
    }
    /**
    * Returns a new set of all key/value pairs where key matches the given prefix.
    */
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
    /**
    * Returns true iff the given key == "true".
    */
    public boolean getBoolean(String key) {
        try {
            return Boolean.parseBoolean(get(key));
        } catch (Exception e) {
            try {
                return Boolean.parseBoolean(get(key, new HashSet<>(Arrays.asList("true", "false"))));
            } catch (Exception e2) {
                return false;
            }
        }
    }
    /**
    * Returns the double parsed from the value with the given key.
    */
    public double getNumber(String key) {
        try {
            return Double.parseDouble(get(key));
        } catch (Exception e) {
            try {
                return Double.parseDouble(get(key, new HashSet<>(Arrays.asList("-10.00", "0", "1.0", "200.", "300030.030003", ".."))));
            } catch (Exception e2) {
                return 0.0;
            }
        }
    }
    /**
    * Looks up the given query and returns it as an actualized string.
    *
    * Look ups start with an '?' and use arrow brackets for arguments:
    *
    *   !Hello <name>
    *
    *   Config c = new Config();
    *   c.put("name", "Alice");
    *   ...
    *   c.put("lookup=?Hello <name>");
    *   out.println(c.get("lookup"));
    *
    *   Hello Alice
    *
    */
    public String lookup(String query) {
        if (query == null || !query.startsWith("?")) {
            return query;
        }
        String result = parseLookup(query);
        int index = result.indexOf('<');
        if (index < 0) {
            // All arguments resolved, remove '?'
            return result.substring(1);
        }
        return result;
    }
    private String parseLookup(String lookup) {
        int startIndex = lookup.indexOf('<');
        int endIndex = lookup.indexOf('>');
        if (startIndex < 0 || endIndex <= startIndex) {
            return lookup;
        }
        String prefix = lookup.substring(0, startIndex);
        String key = lookup.substring(startIndex + 1, endIndex);
        String suffix = lookup.substring(endIndex + 1);
        String result = get(key);
        StringBuilder sb = new StringBuilder(prefix);
        if (result == null) {
            sb.append("<");
            sb.append(key);// Replace key
            sb.append(">");
        } else {
            sb.append(result);
        }
        if (suffix.length() > 0) {
            sb.append(parseLookup(suffix));
        }
        String r = sb.toString();
        return r;
    }
    /**
    * Parses the given array of lines into a key/value pairs.
    * Puts the key/value pairs into this config.
    * Returns itself for convenient chaining.
    */
    public Config put(String[] lines) {
        if (lines != null) for (int l = 0; l < lines.length; l++) {
            put(lines[l]);
        }
        return this;
    }
    /**
    * Parses the given collection of lines into a key/value pairs.
    * Puts the key/value pairs into this config.
    * Returns itself for convenient chaining.
    */
    public Config put(List<String> lines) {
        if (lines != null) for (String l : lines) {
            put(l);
        }
        return this;
    }
    /**
    * Parses the given line into a key/value pair.
    * Puts the key/value pair into this config.
    * Returns itself for convenient chaining.
    */
    public Config put(String line) {
        // TODO consider using first index of '=' so values containing '=' aren't split
        // int index = line.indexOf('=');
        // if (index < 0) {
        // } else {
        // }
        String[] parts = line.split("=");
        if (parts.length == 1) {
            put(parts[0], "true");
        } else if (parts.length == 2) {
            put(parts[0], parts[1]);
        }
        return this;
    }
    /**
    * Puts the key/value pair into this config.
    * Returns itself for convenient chaining.
    */
    public Config put(Object key, Object value) {
        return put(key.toString(), value.toString());
    }
    /**
    * Puts the key/value pair into this config.
    * Returns itself for convenient chaining.
    */
    public Config put(String key, String value) {
        if (!key.isEmpty()) {
            configs.put(key, value);
        }
        return this;
    }
    /**
    * Returns a new config containing all of the given keys,
    * and their associated values from this config.
    */
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