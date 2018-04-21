Config
======

Interactive Nestable Map

## Build
    $> ./lite-em # Outputs to out/java/jar/Config.jar
## Usage
    // Config c = Config.create(String[] args, String[] files, Config parent, InputStream i, OutputStream o);
    // Or
    Config c = Config.create(new String[] {"a","r","g","s"});
    c.put("b=c");
    c.put("c","d");
    c.put("e");
    c.put("f=false");
    if (!c.has("a") && c.has("b")) {
        if (c.getBoolean("r") && !c.getBoolean("f")) {
            c.put("s", c.get("g"));
        }
    }
