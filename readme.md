# eyebrowse

A way to render html/svg strings to images for Clojure.

## Status
An ugly prototype

## How to

0. Run a compatible JVM. I'm using:
```
java --version
openjdk 17.0.4 2022-07-19 LTS
OpenJDK Runtime Environment Zulu17.36+13-CA (build 17.0.4+8-LTS)
OpenJDK 64-Bit Server VM Zulu17.36+13-CA (build 17.0.4+8-LTS, mixed mode, sharing)
```

This is mostly a Proof of Concept at the moment, so other JVMs haven't been tried. Though I'd love this to work generally.

1. Build the .java file to get the .class file(s)
```
clj -X:build-java
```

2. Run a REPL with the `:headless` alias
```
clj -A:headless
```

3. Render HTML to PNG!!
```
(require 'eyebrowse.main)

(-> "This is some HTML"
    (eyebrowse.main/html->image 200 200)
    (eyebrowse.main/save-image! "out.png"))
```
