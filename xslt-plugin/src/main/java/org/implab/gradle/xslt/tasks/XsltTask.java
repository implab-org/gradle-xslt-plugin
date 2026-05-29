package org.implab.gradle.xslt.tasks;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xpath.NodeSet;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.implab.gradle.xslt.internal.DomBuilderEx;
import org.implab.gradle.xslt.internal.DomSupport;
import org.implab.gradle.xslt.internal.TransformerParameterVisitor;
import org.implab.gradle.xslt.internal.XsltParameter;
import org.w3c.dom.Node;

import groovy.lang.Closure;

/**
 * The task for applying an XSLT transformation. This task applies a single
 * transformation to a single document and produces a single result.
 */
@DisableCachingByDefault(because = "XSLT transformations may resolve external resources not declared as task inputs.")
public abstract class XsltTask extends DefaultTask {

    private transient final DomBuilderEx dom;

    private transient NodeSet currentSet;

    private final MapProperty<String, XsltParameter> parameters;

    @Inject
    public XsltTask(ObjectFactory objects) {
        var document = DomSupport.newDocument();

        parameters = objects.mapProperty(String.class, XsltParameter.class);
        dom = new DomBuilderEx(document) {
            @Override
            protected void nodeCompleted(Object parent, Object node) {
                if (currentSet != null && parent == null)
                    currentSet.addElement((Node) node);

                super.nodeCompleted(parent, node);
            }
        };
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getStylesheet();

    private File getStylesheetFile() {
        return getStylesheet().get().getAsFile();
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSource();

    private File getSourceFile() {
        return getSource().get().getAsFile();
    }

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getIncludes();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    private File getOutputFile() {
        return getOutput().get().getAsFile();
    }

    @Input
    public Provider<Map<String, XsltParameter>> getParameters() {
        return parameters.map(Map::copyOf);
    }

    /**
     * Conventional method, applies closure to the transformation parameters.
     *
     * <p>
     * The delegate for this closure is the map of values where the keys are
     * the names of the parameters. Values could be either:
     * <ul>
     * <li>simple values,
     * <li>nodes, nodeSets ({@link #nodeSet(Closure)}),
     * <li>providers which supply simple values.
     * </ul>
     *
     * @param configure A closure for configuration of transformation parameters
     */
    public void parameters(Closure<?> configure) {
        requireNonNull(configure);
        var props = new HashMap<String, Object>();
        configure.setDelegate(props);
        configure.call();

        parameters(props);
    }

    /**
     * Adds transformation parameters from a map.
     *
     * @param parameters The map of transformation parameters
     */
    public void parameters(Map<String, ?> parameters) {
        requireNonNull(parameters);
        parameters.forEach(this::parameter);
    }

    /**
     * Applies action to a temporary parameter map and adds collected values to
     * transformation parameters.
     *
     * @param configure An action for configuration of transformation parameters
     */
    public void parameters(Action<? super Map<String, Object>> configure) {
        requireNonNull(configure);
        var props = new HashMap<String, Object>();
        configure.execute(props);

        parameters(props);
    }

    private void parameter(String name, Object value) {
        if (value instanceof Provider<?>)
            parameters.put(name, ((Provider<?>) value).map(XsltParameter::of));
        else
            parameters.put(name, XsltParameter.of(value));
    }

    /**
     * Creates a nodeset. Can be used to construct a transformation parameter.
     *
     * @param configure A closure with inner nodes for this nodeSet.
     */
    public NodeSet nodeSet(Closure<?> configure) {
        requireNonNull(configure);
        var prevSet = currentSet;
        try {
            currentSet = new NodeSet();

            configure.setDelegate(dom);
            configure.call();

            return currentSet;
        } finally {
            currentSet = prevSet;
        }
    }

    /**
     * Creates an XML document fragment. Can be used to construct a transformation
     * parameter when the stylesheet expects a single fragment instead of a node
     * set.
     *
     * @param configure A closure with inner nodes for this fragment.
     */
    public Object fragment(Closure<?> configure) {
        return dom.fragment(configure);
    }

    @Input
    @Optional
    public abstract Property<Integer> getIndent();

    @TaskAction
    public void run() throws IOException, TransformerException {
        var factory = TransformerFactory.newInstance();
        var transformer = factory.newTransformer(new StreamSource(getStylesheetFile()));
        var outputFile = getOutputFile();
        var outputDirectory = outputFile.toPath().getParent();
        var parameters = new TransformerParameterVisitor(transformer);

        if (outputDirectory != null)
            Files.createDirectories(outputDirectory);

        this.parameters.get().forEach((name, value) -> parameters.setParameter(name, value));

        if (getIndent().isPresent()) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                    getIndent().get().toString());
        }

        transformer.transform(
                new StreamSource(getSourceFile()),
                new StreamResult(outputFile));
    }
}
