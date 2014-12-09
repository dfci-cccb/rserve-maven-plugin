rserve-maven-plugin
===================

Maven plugin for Rserve lifecycle

Supports rserve:start rserve:stop and rserve:run mojos. Start will 
start Rserve and exit with minimal output to maven console, stop
will stop and exit, run will block and dump debug information to
standard out.

Basic usage:
```
<project>
  <build>
    <plugins>
      <plugin>
        <groupId>edu.dfci.cccb</groupId>
        <artifactId>rserve-maven-plugin</artifactId>
        <version>0.0.4</version>
        <configuration>
        
          <!-- Path to R binary, the plugin will try this variable, "R"
               property of the maven project, R environment variable,
               relative to R_HOME environment variable and finally R
               available on the path, in this order -->
          <r>/path/to/executable/R</r>

          <!-- Rserve source tarball -->
          <rserveSource>http://rforge.net/src/contrib/Rserve_1.8-1.tar.gz</rserveSource>

          <setup>

            <!-- Key/value pairs corresponding to entries in Rserv.conf -->
            <port>6311</port>
          </setup>

          <!-- Actual R code to run in the plugin process before the
               server is started, this is good for package installation -->
          <onInitialize>print(paste("Hello","${basedir}"))</onInitialize>

          <!-- Actual R code to run in each request process -->
          <onRequest>print(paste("Hi","${basedir}"))</onRequest>
        </configuration>
      </plugin>
    </plugins>
  </build>
  <pluginRepositories>
    <pluginRepository>
      <id>cccb</id>
      <name>CCCB Maven Repository</name>
      <url>https://raw.github.com/dfci-cccb/maven-repo/master/releases/</url>
    </pluginRepository>
  </pluginRepositories>
</project>
```
My typical use case as part of a web application looks like this:
```
R=/path/to/my/R mvn rserve:start jetty:run rserve:stop
```