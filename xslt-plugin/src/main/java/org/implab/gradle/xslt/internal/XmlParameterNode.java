package org.implab.gradle.xslt.internal;

import java.io.Serializable;
import java.util.List;

public record XmlParameterNode(
        short type,
        String name,
        String namespaceUri,
        String value,
        List<XmlParameterAttribute> attributes,
        List<XmlParameterNode> children) implements Serializable {

    private static final long serialVersionUID = 1L;

    public XmlParameterNode {
        attributes = attributes != null ? List.copyOf(attributes) : List.of();
        children = children != null ? List.copyOf(children) : List.of();
    }
}
