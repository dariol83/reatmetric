package eu.dariolucia.reatmetric.ui.mimics;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

import static eu.dariolucia.reatmetric.ui.mimics.SvgConstants.DATA_RTMT_BINDING_ID;

/**
 * This application helps in the preparation of SVG mimics for the ReatMetric system:
 *
 * Usage: SvgMimicInitialiser [source file] [destination file]
 */
public class SvgMimicInitialiser {

    public static void main(String[] args) throws Exception {
        if(args.length != 2) {
            System.err.println("Usage: SvgMimicInitialiser <source file> <destination file>");
            System.exit(1);
        }

        // Load SVG DOM
        File xmlFile = new File(args[0]);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = factory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        // Navigate SVG
        navigate(doc, doc.getDocumentElement());
        // Save SVG DOM
        File output = new File(args[1]);
        if(output.exists()) {
            output.delete();
            output.createNewFile();
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);

        transformer.transform(source, result);
    }

    private static void navigate(Document doc, Node n) {
        if(!(n instanceof Element)) {
            return;
        }
        Element e = (Element) n;
        String attPresence = e.getAttribute(DATA_RTMT_BINDING_ID);

        if(attPresence != null && !attPresence.trim().isEmpty()) {
            // Check the type of the element
            switch(e.getTagName()) {
                case "rect":
                {
                    e.setAttribute("data-rtmt-fill-color-00","$validity EQ INVALID := #C9C9C9");
                    e.setAttribute("data-rtmt-fill-color-10","$alarm EQ ALARM := #FF0000");
                    e.setAttribute("data-rtmt-fill-color-20","$alarm EQ ERROR := #FF0000");
                    e.setAttribute("data-rtmt-fill-color-30","$alarm EQ WARNING := #FFD700");
                    e.setAttribute("data-rtmt-fill-color-40","$alarm EQ VIOLATED := #FFE4B5");
                    e.setAttribute("data-rtmt-fill-color-50",":= #00FF00");
                }
                break;
                case "circle":
                case "ellipse":
                {
                    e.setAttribute("data-rtmt-fill-color-00","$validity EQ INVALID := #C9C9C9");
                    e.setAttribute("data-rtmt-fill-color-10",":= #00FF00");

                    e.setAttribute("data-rtmt-blink-00","$alarm EQ ALARM := #FF0000");
                    e.setAttribute("data-rtmt-blink-10","$alarm EQ ERROR := #FF0000");
                    e.setAttribute("data-rtmt-blink-20","$alarm EQ WARNING := #FFD700");
                    e.setAttribute("data-rtmt-blink-30","$alarm EQ VIOLATED := #FFE4B5");
                    e.setAttribute("data-rtmt-blink-40",":= none");
                }
                break;
                case "path":
                {
                    e.setAttribute("data-rtmt-stroke-color-00","$validity EQ INVALID := #C9C9C9");
                    e.setAttribute("data-rtmt-stroke-color-10","$eng EQ ACTIVE := #1E90FF");
                    e.setAttribute("data-rtmt-stroke-color-20",":= #000000");
                    e.setAttribute("data-rtmt-stroke-width-00","$validity EQ INVALID := .200");
                    e.setAttribute("data-rtmt-stroke-width-10","$eng EQ ACTIVE := .800");
                    e.setAttribute("data-rtmt-stroke-width-20",":= .200");
                }
                break;
                case "text":
                case "tspan":
                {
                    e.setAttribute("data-rtmt-text-00",":= $eng");
                }
                break;
            }
        }
        for(int i = 0; i < e.getChildNodes().getLength(); ++i) {
            navigate(doc, e.getChildNodes().item(i));
        }
    }
}
