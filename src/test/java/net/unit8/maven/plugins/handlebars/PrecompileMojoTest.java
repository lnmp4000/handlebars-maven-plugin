package net.unit8.maven.plugins.handlebars;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class PrecompileMojoTest extends PrecompileMojo {
	private PrecompileMojo mojo;

	@Before
	public void setUp() {
		mojo = new PrecompileMojo();
		mojo.sourceDirectory = new File("src/test/resources/templates");
		mojo.outputDirectory = new File("target/output");
        mojo.handlebarsVersion =  "1.0.0";
        mojo.alwaysPrecompile = true;
	}


	@Test
	public void testPreserveHierarchy() throws MojoExecutionException, MojoFailureException {
		mojo.execute();
		assertTrue(new File(mojo.outputDirectory, "templates.js").exists());
		assertTrue(new File(mojo.outputDirectory, "hoge/hoge.js").exists());
		assertTrue(new File(mojo.outputDirectory, "hoge/fuga/fuga.js").exists());
	}


    @Test
    public void testPurgingWhitespace() throws MojoExecutionException, MojoFailureException, IOException {
        mojo.purgeWhitespace = true;
        mojo.execute();
        File precompiled = new File(mojo.outputDirectory, "templates.js");
        assertTrue(precompiled.exists());

        String evaluationString = "Handlebars.templates['root1']({hello:'I am '})";
        Object obj = evaluateString(precompiled,evaluationString);

        assertEquals("I am root1", obj.toString());
    }


    private Object evaluateString(File precompiled, String evaluationStr) throws IOException{
        try {
            Context cx = Context.enter();
            ScriptableObject global = cx.initStandardObjects();

            URL handlebarsUrl = getClass().getClassLoader().getResource("script/1.0.0");
            if (handlebarsUrl == null)
                throw new IllegalArgumentException("can't find resource handlebars.");
            InputStreamReader in = new InputStreamReader(handlebarsUrl.openStream());
            try {
                cx.evaluateReader(global, in, handlebarsVersion, 1, null);
            } finally {
                IOUtils.closeQuietly(in);
            }

            FileReader inSource = new FileReader(precompiled);
            try {
                cx.evaluateReader(global, inSource, precompiled.getName(), 1, null);
            } finally {
                IOUtils.closeQuietly(inSource);
            }
            Object obj = cx.evaluateString(global, evaluationStr, "<inline>", 1, null);
            return obj;
        } finally {
            Context.exit();
        }
    }
}
