/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package net.unit8.maven.plugins.handlebars;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Handlebars precompile
 *
 * @author kawasima
 * @author Kellner
 * @phase compile
 * @goal precompile
 */
public class PrecompileMojo extends AbstractMojo {
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter
     */
    protected String[] templateExtensions;

    /**
     * @paremeter
     */

    protected String partialPrefix;
    /**
     * @parameter
     */
    protected Boolean purgeWhitespace;

    /**
     * @required
     * @parameter expression="${sourceDirectory}"
     */
    protected File sourceDirectory;

    /**
     * @parameter expression="${outputDirectory}"
     */
    protected File outputDirectory;

    /**
     * @required
     * @parameter
     */
    protected String outputFileName;

    /**
     * Handlebars script filename
     *
     * @parameter expression="${handlebarsVersion}" default-value="1.0.0"
     */
    protected String handlebarsVersion;

    /**
     * @parameter expression="${encoding}" default-value="UTF-8"
     */
    protected String encoding = "UTF-8";

    private HandlebarsEngine handlebarsEngine;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (outputDirectory == null)
            outputDirectory = new File(sourceDirectory.getAbsolutePath());
        if (!outputDirectory.exists()) {
            try {
                FileUtils.forceMkdir(outputDirectory);
            } catch (IOException e) {
                throw new MojoExecutionException("Failure to make an output directory.", e);
            }
        }
        if (templateExtensions == null)
            templateExtensions = new String[]{"html", "htm", "hbs"};

        if (purgeWhitespace == null)
            purgeWhitespace = false;

        if (partialPrefix == null)
            partialPrefix = "partial_";

        if (outputFileName == null) {
            outputFileName = "template.js";
        }

        handlebarsEngine = new HandlebarsEngine(handlebarsVersion);
        handlebarsEngine.setEncoding(encoding);


        if (project != null) {
            handlebarsEngine.setCacheDir(
                    new File(project.getBuild().getDirectory(), "handlebars-maven-plugins/script"));
        }
        handlebarsEngine.startup();

        try {
            precompile(sourceDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Failure to precompile handlebars templates.", e);
        }

    }

    protected void precompile(File _startdir) throws IOException {
        final List<File> templates = new ArrayList<File>();
        Path startdir = Paths.get(_startdir.toURI());

        Files.walkFileTree(startdir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                templates.add(file.toFile());
                return FileVisitResult.CONTINUE;
            }
        });
        handlebarsEngine.precompile(templates, new File(outputDirectory.getPath()+File.separator+outputFileName)/*getOutputFile(directory)*/, purgeWhitespace, partialPrefix);
    }

}
