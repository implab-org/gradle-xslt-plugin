package org.implab.gradle.xslt.internal;

import javax.xml.parsers.ParserConfigurationException;

import org.gradle.api.GradleException;
import org.w3c.dom.Document;

import groovy.xml.FactorySupport;

public final class DomSupport {

    private DomSupport() {
    }

    public static Document newDocument() {
        try {
            var factory = FactorySupport.createDocumentBuilderFactory();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            return factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new GradleException("Failed to create DOM document", e);
        }
    }
}
