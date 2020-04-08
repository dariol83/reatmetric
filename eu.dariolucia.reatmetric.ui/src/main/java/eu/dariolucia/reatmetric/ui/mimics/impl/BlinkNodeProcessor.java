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
import eu.dariolucia.reatmetric.ui.mimics.MimicsEngine;
import eu.dariolucia.reatmetric.ui.mimics.SvgAttributeProcessor;
import eu.dariolucia.reatmetric.ui.mimics.SvgConstants;
import javafx.scene.paint.Color;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.logging.Level;
import java.util.logging.Logger;


public class BlinkNodeProcessor extends SvgAttributeProcessor {

    private static final Logger LOG = Logger.getLogger(BlinkNodeProcessor.class.getName());

    private final Element currentAnimationNode;

    public BlinkNodeProcessor(Element element, String name, String value) {
        super(element, name, value);
        // For this type of processor, the expression has a value independent of the parameter state
        String colourText = expression.apply(null);
        if(colourText.equals(SvgConstants.NO_BLINK)) {
            currentAnimationNode = null;
        } else {
            currentAnimationNode = element.getOwnerDocument().createElementNS("http://www.w3.org/2000/svg", "animate");
            currentAnimationNode.setAttribute("attributeType", "XML");
            currentAnimationNode.setAttribute("attributeName", "fill");
            currentAnimationNode.setAttribute("dur", "1.0s");
            currentAnimationNode.setAttribute("repeatCount", "indefinite");
            currentAnimationNode.setAttribute("values", deriveStringColour(colourText));
        }
    }

    @Override
    public Runnable buildUpdate(ParameterData parameterData) {
        try {
            if(currentAnimationNode != null) {
                // Apply
                return new BlinkNodeApplier(element, currentAnimationNode);
            } else {
                // Remove
                return new BlinkNodeRemover(element);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error when building blink update", e);
            return () -> {};
        }
    }

    private String deriveStringColour(String fill) {
        Color baseColor = Color.web(fill);
        // Resulting string is baseColor/2, baseColor/2, baseColor, baseColor/2
        Color half = Color.color(baseColor.getRed()/2, baseColor.getGreen()/2, baseColor.getBlue()/2, 1);
        return toString(half) + ";" + toString(half) + ";" + toString(baseColor) + ";" + toString(half);
    }

    private String toString(Color c) {
        return "#" + hex(c.getRed()) + hex(c.getGreen()) + hex(c.getBlue()) + "FF";
    }

    private String hex(double v) {
        int vv = (int) (v * 255); // 0 - 255
        return String.format("%02X", vv);
    }

    private class BlinkNodeApplier implements Runnable {

        private final Element element;
        private final Node toAdd;

        public BlinkNodeApplier(Element element, Node toAdd) {
            this.element = element;
            this.toAdd = toAdd;
        }

        @Override
        public void run() {
            try {
                LOG.warning("Appending: " + toAdd);
                LOG.log(Level.SEVERE, "Before add");
                MimicsEngine.fullPrint(BlinkNodeProcessor.super.element);

                // First remove the "animate" node if any. If the node to remove is exactly equals to toAdd, don't do anything, you are done
                for(int i = 0; i < element.getChildNodes().getLength(); ++i) {
                    Node child = element.getChildNodes().item(i);
                    if(child instanceof Element) {
                        if(((Element) child).getTagName().equals("animate")) {
                            if(child != toAdd) {
                                element.removeChild(child);
                                break;
                            } else {
                                // Do nothing
                                return;
                            }
                        }
                    }
                }
                element.appendChild(toAdd);
                // TODO: remove
                LOG.log(Level.SEVERE, "Before add");
                MimicsEngine.fullPrint(BlinkNodeProcessor.super.element);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error when running blink applier", e);
            }
        }
    }

    private class BlinkNodeRemover implements Runnable {

        private final Element element;

        public BlinkNodeRemover(Element element) {
            this.element = element;
        }

        @Override
        public void run() {
            try {
                LOG.log(Level.SEVERE, "Before remove");
                MimicsEngine.fullPrint(BlinkNodeProcessor.super.element);

                for(int i = 0; i < element.getChildNodes().getLength(); ++i) {
                    Node child = element.getChildNodes().item(i);
                    if(child instanceof Element) {
                        if(((Element) child).getTagName().equals("animate")) {
                            element.removeChild(child);
                            return;
                        }
                    }
                }
                // TODO: remove
                LOG.log(Level.SEVERE, "After remove");
                MimicsEngine.fullPrint(BlinkNodeProcessor.super.element);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error when running blink remover", e);
            }
        }
    }

}
