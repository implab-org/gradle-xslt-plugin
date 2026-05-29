package org.implab.gradle.xslt.internal;

import java.io.Serializable;

public interface XsltParameter extends Serializable {

    void accept(Visitor visitor);

    interface Visitor {
        void visit(SimpleParameter parameter);

        void visit(NodeParameter parameter);

        void visit(NodeSetParameter parameter);

        void visit(FragmentParameter parameter);
    }

    public static XsltParameter of(Object value) {
        return XsltParameterFactory.parameter(value);
    }
}
