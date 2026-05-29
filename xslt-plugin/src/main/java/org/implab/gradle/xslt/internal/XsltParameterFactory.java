package org.implab.gradle.xslt.internal;

import java.util.ArrayList;

import org.apache.xpath.NodeSet;
import org.w3c.dom.Node;

final class XsltParameterFactory {

    private XsltParameterFactory() {
    }

    static XsltParameter parameter(Object value) {
        if (value instanceof XsltParameter)
            return (XsltParameter) value;
        else if (value instanceof NodeSet)
            return nodeSetParameter((NodeSet) value);
        else if (value instanceof Node)
            return nodeParameter((Node) value);
        else
            return new SimpleParameter(value);
    }

    private static XsltParameter nodeParameter(Node node) {
        if (node.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE)
            return new FragmentParameter(XmlParameterCodec.encode(node));
        else
            return new NodeParameter(XmlParameterCodec.encode(node));
    }

    private static NodeSetParameter nodeSetParameter(NodeSet nodeSet) {
        var nodes = new ArrayList<XmlParameterNode>();

        for (var i = 0; i < nodeSet.getLength(); i++)
            nodes.add(XmlParameterCodec.encode(nodeSet.item(i)));

        return new NodeSetParameter(nodes);
    }
}
