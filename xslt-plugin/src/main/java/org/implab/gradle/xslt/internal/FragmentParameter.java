package org.implab.gradle.xslt.internal;

public record FragmentParameter(XmlParameterNode fragment) implements XsltParameter {
    private static final long serialVersionUID = 1L;

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
