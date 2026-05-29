package org.implab.gradle.xslt.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.xpath.NodeSet;
import org.gradle.api.GradleException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

final class XmlParameterCodec {

    private XmlParameterCodec() {
    }

    static XmlParameterNode encode(Node node) {
        return switch (node.getNodeType()) {
            case Node.ELEMENT_NODE -> new XmlParameterNode(
                    node.getNodeType(),
                    node.getNodeName(),
                    node.getNamespaceURI(),
                    null,
                    attributes(node.getAttributes()),
                    children(node));
            case Node.TEXT_NODE, Node.CDATA_SECTION_NODE, Node.COMMENT_NODE -> new XmlParameterNode(
                    node.getNodeType(),
                    null,
                    null,
                    node.getNodeValue(),
                    List.of(),
                    List.of());
            case Node.PROCESSING_INSTRUCTION_NODE -> new XmlParameterNode(
                    node.getNodeType(),
                    node.getNodeName(),
                    null,
                    node.getNodeValue(),
                    List.of(),
                    List.of());
            case Node.DOCUMENT_FRAGMENT_NODE -> new XmlParameterNode(
                    node.getNodeType(),
                    null,
                    null,
                    null,
                    List.of(),
                    children(node));
            default -> throw new GradleException("Unsupported XML parameter node type: " + node.getNodeType());
        };
    }

    static Node toNode(Document document, XmlParameterNode node) {
        return switch (node.type()) {
            case Node.ELEMENT_NODE -> element(document, node);
            case Node.TEXT_NODE -> document.createTextNode(node.value());
            case Node.CDATA_SECTION_NODE -> document.createCDATASection(node.value());
            case Node.COMMENT_NODE -> document.createComment(node.value());
            case Node.PROCESSING_INSTRUCTION_NODE -> document.createProcessingInstruction(node.name(), node.value());
            case Node.DOCUMENT_FRAGMENT_NODE -> fragment(document, node);
            default -> throw new GradleException("Unsupported XML parameter node type: " + node.type());
        };
    }

    static NodeSet toNodeSet(Document document, NodeSetParameter parameter) {
        var nodeSet = new NodeSet();

        parameter.nodes().stream()
                .map(node -> toNode(document, node))
                .forEach(nodeSet::addElement);

        return nodeSet;
    }

    private static Node element(Document document, XmlParameterNode node) {
        var element = node.namespaceUri() != null
                ? document.createElementNS(node.namespaceUri(), node.name())
                : document.createElement(node.name());

        node.attributes().forEach(attribute -> {
            if (attribute.namespaceUri() != null)
                element.setAttributeNS(attribute.namespaceUri(), attribute.name(), attribute.value());
            else
                element.setAttribute(attribute.name(), attribute.value());
        });

        node.children().stream()
                .map(child -> toNode(document, child))
                .forEach(element::appendChild);

        return element;
    }

    private static Node fragment(Document document, XmlParameterNode node) {
        var fragment = document.createDocumentFragment();

        node.children().stream()
                .map(child -> toNode(document, child))
                .forEach(fragment::appendChild);

        return fragment;
    }

    private static List<XmlParameterAttribute> attributes(NamedNodeMap attributes) {
        var result = new ArrayList<XmlParameterAttribute>();

        for (var i = 0; i < attributes.getLength(); i++) {
            var attribute = attributes.item(i);
            result.add(new XmlParameterAttribute(
                    attribute.getNodeName(),
                    attribute.getNamespaceURI(),
                    attribute.getNodeValue()));
        }

        return result;
    }

    private static List<XmlParameterNode> children(Node node) {
        var result = new ArrayList<XmlParameterNode>();
        var children = node.getChildNodes();

        for (var i = 0; i < children.getLength(); i++)
            result.add(encode(children.item(i)));

        return result;
    }
}
