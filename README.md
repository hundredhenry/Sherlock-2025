# Sherlock

Wiki: https://github.com/hundredhenry/Sherlock-2025/wiki

## Requirements
  - JDK 25

## Running
To run Sherlock, run the pre-built JAR file with `java -jar Sherlock-release.jar` with JDK 25 installed on your machine. Alternatively, double-click `Sherlock-release.jar`.

Sherlock will start a local web server and automatically open your browser to **http://localhost:2218**. When running locally, you are logged in automatically (no account or password needed).

## Building
To build Sherlock use `gradlew(.bat) build`.

The standard and development jars, along with the war file for running Sherlock on a server, will be built into the `./build/out/` directory.

## CSS and JavaScript
This project uses Sass (https://sass-lang.com/) to compile the CSS files for the web interface, the Sass files are stored in `src/main/sass`. The original JavaScript files are stored in `src/main/javascript`. Please do not edit the CSS or JavaScript files in `src/main/resources/static`, any changes you make to these files will be overwritten. Gradle will automatically compile the CSS files and minify the Javascript files when you run `gradlew(.bat) build`.
