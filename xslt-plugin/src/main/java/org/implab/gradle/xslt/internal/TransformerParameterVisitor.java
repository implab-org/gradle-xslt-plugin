package org.implab.gradle.xslt.internal;

import javax.xml.transform.Transformer;

import org.w3c.dom.Document;

public final class TransformerParameterVisitor {

    private final Transformer transformer;

    private final Document document;

    public TransformerParameterVisitor(Transformer transformer) {
        this.transformer = transformer;
        this.document = DomSupport.newDocument();
    }

    public void setParameter(String name, XsltParameter parameter) {
        parameter.accept(new XsltParameterVisitor(name));
    }

    private class XsltParameterVisitor implements XsltParameter.Visitor {

        private final String name;

        XsltParameterVisitor(String name) {
            this.name = name;
        }

        @Override
        public void visit(SimpleParameter parameter) {
            transformer.setParameter(name, parameter.value());
        }

        @Override
        public void visit(NodeParameter parameter) {
            transformer.setParameter(name, XmlParameterCodec.toNode(document, parameter.node()));
        }

        @Override
        public void visit(NodeSetParameter parameter) {
            transformer.setParameter(name, XmlParameterCodec.toNodeSet(document, parameter));
        }

        @Override
        public void visit(FragmentParameter parameter) {
            transformer.setParameter(name, XmlParameterCodec.toNode(document, parameter.fragment()));
        }

    }
}
