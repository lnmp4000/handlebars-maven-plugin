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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Handlebars script engine.
 *
 * @author kawasima
 * @author Kellner
 */
public class HandlebarsEngine {
    /**
     * The cache directory of handlebars script
     */
    private File cacheDir;

    /**
     * The encoding of handlebars templates
     */
    private String encoding;

    /**
     * The name of handlebars script
     */
    private String handlebarsVersion;

    /**
     * The url of handlebars script
     */
    private URL handlebarsUrl;

    private static final Log LOG = new SystemStreamLog();

    public HandlebarsEngine(String handlebarsVersion) throws MojoExecutionException {
        this.handlebarsVersion = handlebarsVersion;
    }

    public void startup() throws MojoExecutionException {
        handlebarsUrl = getClass().getClassLoader().getResource("script/1.0.0");
        if (handlebarsUrl == null) {
            File cacheFile = new File(cacheDir, handlebarsVersion);
            if (!cacheFile.exists()) {
                fetchHandlebars(handlebarsVersion);
            }
            try {
                handlebarsUrl = cacheFile.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new MojoExecutionException("Invalid handlebars cache file.", e);
            }
        }


    }

    private void doPrecompilationPartials(Collection<File> templates, boolean purgeWhitespace, ScriptableObject global, Context cx, PrintWriter out) throws IOException {
        for (File template : templates) {
            String data = FileUtils.readFileToString(template, encoding);

            if (purgeWhitespace)
                data = StringUtils.replaceEach(data, new String[]{"\n", "\r", "\t"}, new String[]{"", "", ""});


            ScriptableObject.putProperty(global, "data", data);
            Object obj = cx.evaluateString(global, "Handlebars.precompile(String(data));", "<cmd>", 1, null);
            String name = FilenameUtils.getBaseName(template.getName());
            out.println("Handlebars.registerPartial('" +
                    name + "', " +
                    "templates['" + name + "']=template(" + obj.toString() + "));");
        }
    }

    private void doPrecompilation(Collection<File> templates, boolean purgeWhitespace, ScriptableObject global, Context cx, PrintWriter out) throws IOException {

        for (File template : templates) {
            String data = FileUtils.readFileToString(template, encoding);

            if (purgeWhitespace)
                data = StringUtils.replaceEach(data, new String[]{"\n", "\r", "\t"}, new String[]{"", "", ""});

            ScriptableObject.putProperty(global, "data", data);
            Object obj = cx.evaluateString(global, "Handlebars.precompile(String(data));", "<cmd>", 1, null);
            out.println("templates['" + FilenameUtils.getBaseName(template.getName()) + "']=template(" + obj.toString() + ");");
        }
    }

    private Collection[] filerCollections(Collection<File> files, String partial_prefix) {
        boolean scanForPartials = partial_prefix!=null && partial_prefix.length() > 0;
        List<File> partials = new ArrayList<File>();
        List<File> templates = new ArrayList<File>();
        for (File file : files) {
            if (scanForPartials && file.getName().startsWith(partial_prefix)) {
                partials.add(file);
            } else {
                templates.add(file);
            }
        }
        Collection[] collections = new Collection[2];
        collections[0] = partials;
        collections[1] = templates;
        return collections;
    }

    public void precompile(Collection<File> templates, File outputFile, boolean purgeWhitespace, String partialPrefix) throws IOException {
        Context cx = Context.enter();
        PrintWriter out = null;

        Collection<File> filteredList_partials;
        Collection<File> filteredList_templates;

        LOG.info("precompiling " + templates + " to " + outputFile);
        try {
            out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), encoding));
            out.print("(function() {\n  var template = Handlebars.template, "
                    + "templates = Handlebars.templates = Handlebars.templates || {};\n");
            // Rhino for Handlebars Template
            ScriptableObject global = cx.initStandardObjects();
            InputStreamReader in = new InputStreamReader(handlebarsUrl.openStream());
            cx.evaluateReader(global, in, handlebarsVersion, 1, null);
            IOUtils.closeQuietly(in);

            Collection[] collections = filerCollections(templates, partialPrefix);

            filteredList_partials = collections[0];
            filteredList_templates = collections[1];

            doPrecompilationPartials(filteredList_partials, purgeWhitespace, global, cx, out);
            doPrecompilation(filteredList_templates, purgeWhitespace, global, cx, out);

        } finally {
            Context.exit();
            if (out != null)
                out.println("})();");
            IOUtils.closeQuietly(out);
        }
    }

    protected void fetchHandlebars(String handlebarsVersion) throws MojoExecutionException {
        URLConnection conn = null;

        try {
            if (!cacheDir.exists()) {
                FileUtils.forceMkdir(cacheDir);
            }
            conn = new URL("https://raw.github.com/wycats/handlebars.js/" + handlebarsVersion + "/dist/handlebars.js").openConnection();
            if (((HttpURLConnection) conn).getResponseCode() == 302) {
                String location = conn.getHeaderField("Location");
                ((HttpURLConnection) conn).disconnect();
                conn = new URL(location).openConnection();
            }
            LOG.info("Fetch handlebars.js from GitHub (" + conn.getURL() + ")");
            IOUtils.copy(conn.getInputStream(), new FileOutputStream(new File(cacheDir, handlebarsVersion)));
        } catch (Exception e) {
            throw new MojoExecutionException("Failure fetch handlebars.", e);
        } finally {
            if (conn != null) {
                ((HttpURLConnection) conn).disconnect();
            }
        }
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
