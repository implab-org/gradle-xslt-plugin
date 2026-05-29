package org.implab.gradle.xslt.internal;

import java.util.List;

public record NodeSetParameter(List<XmlParameterNode> nodes) implements XsltParameter {
    private static final long serialVersionUID = 1L;

    public NodeSetParameter {
        nodes = List.copyOf(nodes);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
