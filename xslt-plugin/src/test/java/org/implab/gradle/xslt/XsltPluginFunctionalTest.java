package org.implab.gradle.xslt;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class XsltPluginFunctionalTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void transformsXmlWithScalarAndNodeSetParameters() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-test'\n");
        write("build.gradle", """
                plugins {
                    id 'org.implab.gradle-xslt'
                }

                tasks.register('transform', XsltTask) {
                    source = file('src/model.xml')
                    stylesheet = file('src/model.xsl')
                    output = layout.buildDirectory.file('generated/result.xml')
                    indent = 2

                    parameters {
                        pkg = 'org.example.model'
                        annotation = nodeSet {
                            text('Generated')
                            text('Stable')
                        }
                    }
                }
                """);
        write("src/model.xml", """
                <model name="Sample"/>
                """);
        write("src/model.xsl", """
                <xsl:stylesheet version="1.0"
                    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                    <xsl:output method="xml" omit-xml-declaration="yes"/>
                    <xsl:param name="pkg"/>
                    <xsl:param name="annotation"/>

                    <xsl:template match="/model">
                        <result package="{$pkg}" name="{@name}">
                            <xsl:for-each select="$annotation">
                                <annotation><xsl:value-of select="."/></annotation>
                            </xsl:for-each>
                        </result>
                    </xsl:template>
                </xsl:stylesheet>
                """);

        var result = run("transform");
        var output = read("build/generated/result.xml");

        assertEquals(SUCCESS, result.task(":transform").getOutcome());
        assertTrue(output.contains("package=\"org.example.model\""));
        assertTrue(output.contains("name=\"Sample\""));
        assertTrue(output.contains("<annotation>Generated</annotation>"));
        assertTrue(output.contains("<annotation>Stable</annotation>"));
    }

    @Test
    public void addsMapParameters() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-test'\n");
        write("build.gradle", """
                plugins {
                    id 'org.implab.gradle-xslt'
                }

                tasks.register('transform', XsltTask) {
                    source = file('src/input.xml')
                    stylesheet = file('src/message.xsl')
                    output = layout.buildDirectory.file('message.txt')
                    parameters([
                        greeting: 'Hello',
                        suffix: providers.provider { '!' }
                    ])
                }
                """);
        write("src/input.xml", "<input name=\"world\"/>\n");
        write("src/message.xsl", """
                <xsl:stylesheet version="1.0"
                    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                    <xsl:output method="text"/>
                    <xsl:param name="greeting"/>
                    <xsl:param name="suffix"/>

                    <xsl:template match="/input">
                        <xsl:value-of select="$greeting"/>
                        <xsl:text>, </xsl:text>
                        <xsl:value-of select="@name"/>
                        <xsl:value-of select="$suffix"/>
                    </xsl:template>
                </xsl:stylesheet>
                """);

        var result = run("transform");

        assertEquals(SUCCESS, result.task(":transform").getOutcome());
        assertEquals("Hello, world!", read("build/message.txt").trim());
    }

    @Test
    public void tracksIncludedStylesheetInputs() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-test'\n");
        write("build.gradle", """
                plugins {
                    id 'org.implab.gradle-xslt'
                }

                tasks.register('transform', XsltTask) {
                    source = file('src/input.xml')
                    stylesheet = file('src/xslt/main.xsl')
                    includes.from('src/xslt/common.xsl')
                    output = layout.buildDirectory.file('message.txt')
                }
                """);
        write("src/input.xml", "<input/>\n");
        write("src/xslt/main.xsl", """
                <xsl:stylesheet version="1.0"
                    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                    <xsl:include href="common.xsl"/>
                    <xsl:output method="text"/>

                    <xsl:template match="/">
                        <xsl:call-template name="message"/>
                    </xsl:template>
                </xsl:stylesheet>
                """);
        writeCommonStylesheet("one");

        var firstRun = run("transform");
        var secondRun = run("transform");

        writeCommonStylesheet("two");
        var thirdRun = run("transform");

        assertEquals(SUCCESS, firstRun.task(":transform").getOutcome());
        assertEquals(UP_TO_DATE, secondRun.task(":transform").getOutcome());
        assertEquals(SUCCESS, thirdRun.task(":transform").getOutcome());
        assertEquals("two", read("build/message.txt").trim());
    }

    @Test
    public void transformsXmlWithFragmentParameter() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-test'\n");
        write("build.gradle", """
                plugins {
                    id 'org.implab.gradle-xslt'
                }

                tasks.register('transform', XsltTask) {
                    source = file('src/input.xml')
                    stylesheet = file('src/fragments.xsl')
                    output = layout.buildDirectory.file('fragments.txt')

                    parameters {
                        items = fragment {
                            item('one')
                            item('two')
                        }
                    }
                }
                """);
        write("src/input.xml", "<input/>\n");
        write("src/fragments.xsl", """
                <xsl:stylesheet version="1.0"
                    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                    <xsl:output method="text"/>
                    <xsl:param name="items"/>

                    <xsl:template match="/">
                        <xsl:for-each select="$items/item">
                            <xsl:if test="position() &gt; 1">,</xsl:if>
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </xsl:template>
                </xsl:stylesheet>
                """);

        var result = run("transform");

        assertEquals(SUCCESS, result.task(":transform").getOutcome());
        assertEquals("one,two", read("build/fragments.txt").trim());
    }

    @Test
    public void supportsConfigurationCacheWithNodeSetParameters() throws IOException {
        write("settings.gradle", "rootProject.name = 'xslt-test'\n");
        write("build.gradle", """
                plugins {
                    id 'org.implab.gradle-xslt'
                }

                tasks.register('transform', XsltTask) {
                    source = file('src/input.xml')
                    stylesheet = file('src/message.xsl')
                    output = layout.buildDirectory.file('message.txt')
                    parameters {
                        greeting = 'Hello'
                        words = nodeSet {
                            text('cache')
                        }
                    }
                }
                """);
        write("src/input.xml", "<input name=\"cache\"/>\n");
        write("src/message.xsl", """
                <xsl:stylesheet version="1.0"
                    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                    <xsl:output method="text"/>
                    <xsl:param name="greeting"/>
                    <xsl:param name="words"/>

                    <xsl:template match="/input">
                        <xsl:value-of select="$greeting"/>
                        <xsl:text>, </xsl:text>
                        <xsl:for-each select="$words">
                            <xsl:value-of select="."/>
                        </xsl:for-each>
                    </xsl:template>
                </xsl:stylesheet>
                """);

        var firstRun = run("transform", "--configuration-cache");
        var secondRun = run("transform", "--configuration-cache");

        assertEquals(SUCCESS, firstRun.task(":transform").getOutcome());
        assertEquals(UP_TO_DATE, secondRun.task(":transform").getOutcome());
        assertEquals("Hello, cache", read("build/message.txt").trim());
        assertTrue(secondRun.getOutput().contains("Configuration cache entry reused."));
    }

    private BuildResult run(String... arguments) {
        return GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(arguments)
                .withPluginClasspath()
                .build();
    }

    private void writeCommonStylesheet(String message) throws IOException {
        write("src/xslt/common.xsl", """
                <xsl:stylesheet version="1.0"
                    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                    <xsl:template name="message">%s</xsl:template>
                </xsl:stylesheet>
                """.formatted(message));
    }

    private void write(String relativePath, String content) throws IOException {
        var file = file(relativePath).toPath();
        var parent = file.getParent();

        if (parent != null)
            Files.createDirectories(parent);

        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(file(relativePath).toPath(), StandardCharsets.UTF_8);
    }

    private File file(String relativePath) {
        return Path.of(testProjectDir.getRoot().getPath(), relativePath).toFile();
    }
}
