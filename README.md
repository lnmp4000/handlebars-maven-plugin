handlebars-maven-plugin
=======================

A maven plugin for precompiling handlebars templates

Originally written by:
@kawasimi --> https://github.com/kawasima/handlebars-maven-plugin

Introduction
------------

handlebars-maven-plugin is used to precompile the handlebars templates of your project.
With this fork, you can precompile partial templates as well.

Usage
-----
In your maven pom, use the plugin like this:
    
```
 <plugin>
      <groupId>net.unit8.maven.plugins</groupId>
      <artifactId>handlebars-maven-plugin</artifactId>
      <version>0.3.5</version>
      <executions>
      	<execution>
        	<goals>
            	<goal>precompile</goal>
            </goals>
        </execution>
       </executions>
       <configuration>
       	<sourceDirectory>${project.basedir}/src/main/templates/</sourceDirectory>
        <outputDirectory>${project.build.directory}/classes/template/</outputDirectory>
        <outputFileName>template.js</outputFileName>
        <partialPrefix>partial_</partialPrefix>
        </configuration>
</plugin>
```

#### Optional parameters

Name              |Type    |Description
------------------|--------|--------------------------------------
sourceDirectory   |String  |The directory of handlebars templates
outputDirectory   |String  |The directory of precompiled templates
outputFileName    |String  |Name of the js File that will be generated (default is template.js)
partialPrefix     |String  |Name of the prefix to detect it is a partial which must be processed in a different way. If nothing is set, every template will be handled as a normal template
purgeWhitespace   |Boolean |true if whitespace [\r\n\t] needs to be purged. Defaults to false.
encoding          |String  |charset of template files.
templateExtensions|String[]|The extensions of handlebars templates
handlebarsVersion |String  |The handlebars version using by precompile. Default value is "1.0.0". If you want to use an older version, add this parameter (e.g. 1.0.rc.2 ). And this plugin will fetch the version from GitHub.
