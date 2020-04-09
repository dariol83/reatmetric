/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.ui.mimics.impl;

import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.ui.mimics.SvgAttributeProcessor;
import eu.dariolucia.reatmetric.ui.mimics.SvgConstants;
import javafx.scene.paint.Color;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.logging.Level;
import java.util.logging.Logger;


public class BlinkNodeProcessor extends SvgAttributeProcessor {

    private static final Logger LOG = Logger.getLogger(BlinkNodeProcessor.class.getName());

    private volatile Runnable cachedOperation;

    public BlinkNodeProcessor(Element element, String name, String value) {
        super(element, name, value);
    }

    @Override
    public Runnable buildUpdate(ParameterData parameterData) {
        try {
            if(cachedOperation == null) {
                // Build the operation once
                String colourText = expression.apply(parameterData);
                if(colourText.equals(SvgConstants.NO_BLINK)) {
                    cachedOperation = new BlinkNodeRemover(element);
                } else {
                    cachedOperation = new BlinkNodeApplier(element, colourText);
                }
            }
            return cachedOperation;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error when building blink update", e);
            return () -> {};
        }
    }

    private static String deriveStringColour(String fill) {
        Color baseColor = Color.web(fill);
        // Resulting string is baseColor/2, baseColor/2, baseColor, baseColor/2
        Color half = Color.color(baseColor.getRed()/2, baseColor.getGreen()/2, baseColor.getBlue()/2, 1);
        return toString(half) + ";" + toString(half) + ";" + toString(baseColor) + ";" + toString(half);
    }

    private static String toString(Color c) {
        return "#" + hex(c.getRed()) + hex(c.getGreen()) + hex(c.getBlue()) + "FF";
    }

    private static String hex(double v) {
        int vv = (int) (v * 255); // 0 - 255
        return String.format("%02X", vv);
    }

    /**
     * Remove the first "animate" node from the provided element, if the node is not equal to the provided animateNode.
     *
     * @param element
     * @param animateNode
     * @return true if the animate element was found and removed, or was not found, otherwise false
     */
    private static boolean removeAnimateNodeIfNotEqual(Node element, Node animateNode) {
        for(int i = 0; i < element.getChildNodes().getLength(); ++i) {
            Node child = element.getChildNodes().item(i);
            if(child instanceof Element) {
                if(((Element) child).getTagName().equals("animate")) {
                    // Found the node: if not equal, remove, otherwise done
                    if(child != animateNode) {
                        element.removeChild(child);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static class BlinkNodeApplier implements Runnable {

        private final Element element;
        private final String colourText;
        private Element toAdd;

        public BlinkNodeApplier(Element element, String colourText) {
            this.element = element;
            this.colourText = colourText;
        }

        @Override
        public void run() {
            try {
                if(toAdd == null) {
                    toAdd = element.getOwnerDocument().createElementNS("http://www.w3.org/2000/svg", "animate");
                    toAdd.setAttribute("attributeType", "XML");
                    toAdd.setAttribute("attributeName", "fill");
                    toAdd.setAttribute("dur", "1.0s");
                    toAdd.setAttribute("repeatCount", "indefinite");
                    toAdd.setAttribute("values", deriveStringColour(colourText));
                }
                // First remove the "animate" node if any. If the node to remove is exactly equals to toAdd, don't do anything, you are done
                if(removeAnimateNodeIfNotEqual(element, toAdd)) {
                    element.appendChild(toAdd);
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error when running blink applier", e);
            }
        }
    }

    private static class BlinkNodeRemover implements Runnable {

        private final Element element;

        public BlinkNodeRemover(Element element) {
            this.element = element;
        }

        @Override
        public void run() {
            try {
                removeAnimateNodeIfNotEqual(element, null);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error when running blink remover", e);
            }
        }
    }

}
