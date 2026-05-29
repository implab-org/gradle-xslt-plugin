package org.implab.gradle.xslt.internal;

import java.io.Serializable;

record XmlParameterAttribute(String name, String namespaceUri, String value) implements Serializable {
    private static final long serialVersionUID = 1L;
}
