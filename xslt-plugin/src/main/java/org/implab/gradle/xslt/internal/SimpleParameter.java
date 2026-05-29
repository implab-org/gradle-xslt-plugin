package org.implab.gradle.xslt.internal;

public record SimpleParameter(Object value) implements XsltParameter {
    private static final long serialVersionUID = 1L;

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
