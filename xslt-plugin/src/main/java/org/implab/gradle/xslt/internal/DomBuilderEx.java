package org.implab.gradle.xslt.internal;

import static java.util.Objects.requireNonNull;

import org.w3c.dom.Document;

import groovy.lang.Closure;
import groovy.xml.DOMBuilder;

/**
 * DOMBuilder extended with {@link #fragment(Closure)}, {@link #text(Object)}
 * methods.
 */
public class DomBuilderEx extends DOMBuilder {

    Document document;

    /**
     * Standard constructor used to create a new instance. The parameter
     * is passed to {@link DOMBuilder}.
     */
    public DomBuilderEx(Document document) {
        super(document);
        this.document = document;
    }

    /**
     * Creates a new document fragment. A document fragment is always detached
     * from the main document. All nodes created in the specified closure are
     * added to this fragment.
     * 
     * @param closure The closure with inner structure of the fragment
     * @return The created fragment
     */
    public Object fragment(Closure<?> closure) {
        requireNonNull(closure);

        var fragment = document.createDocumentFragment();

        var parent = getCurrent();

        try {
            setCurrent(fragment);

            setClosureDelegate(closure, fragment);
            closure.call();

        } finally {
            setCurrent(parent);
        }

        nodeCompleted(null, fragment);
        return postNodeCompletion(null, fragment);
    }

    /**
     * Creates a and alone text node. This method is useful to create a set
     * of text nodes, similar to a list of string values.
     * 
     * @param value A content of the text node. If the value is null then the
     *              empty text node will be created
     * @return The created text node
     */
    public Object text(Object value) {
        var text = document.createTextNode(value != null ? value.toString() : "");
        var parent = getCurrent();

        if (parent != null)
            setParent(parent, text);

        nodeCompleted(parent, text);
        return postNodeCompletion(parent, text);
    }
}
