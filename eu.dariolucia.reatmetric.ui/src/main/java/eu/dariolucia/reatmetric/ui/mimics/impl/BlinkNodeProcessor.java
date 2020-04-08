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
import javafx.scene.paint.Color;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BlinkNodeProcessor extends SvgAttributeProcessor {

    private static final Logger LOG = Logger.getLogger(BlinkNodeProcessor.class.getName());

    private final Element currentAnimationNode;
    private volatile boolean nodeAdded;

    public BlinkNodeProcessor(Element element, String name, String value) {
        super(element, name, value);
        currentAnimationNode = element.getOwnerDocument().createElement("animate");
        currentAnimationNode.setAttribute("attributeType", "XML");
        currentAnimationNode.setAttribute("attributeName", "fill");
        currentAnimationNode.setAttribute("dur", "1.0s");
        currentAnimationNode.setAttribute("repeatCount", "indefinite");
    }

    @Override
    public Runnable buildUpdate(ParameterData parameterData) {
        try {
            String valueToApply = expression.apply(parameterData);
            if (valueToApply.equals("true")) {
                // Derive colour
                final String colour = deriveStringColour(element.getAttribute("fill"));
                Runnable deferredSet = () -> currentAnimationNode.setAttribute("values", colour);
                LOG.log(Level.WARNING, "Applying blink with value " + colour);
                // Apply
                return new BlinkNodeApplier(element, currentAnimationNode, deferredSet);
            } else {
                // Remove
                return new BlinkNodeRemover(element, currentAnimationNode);
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
        private final Runnable deferredSet;

        public BlinkNodeApplier(Element element, Node toAdd, Runnable deferredSet) {
            this.element = element;
            this.toAdd = toAdd;
            this.deferredSet = deferredSet;
        }

        @Override
        public void run() {
            try {
                if(deferredSet != null) {
                    deferredSet.run();
                }
                LOG.warning("Appending: " + toAdd);
                if(!nodeAdded) {
                    this.element.appendChild(toAdd);
                    nodeAdded = true;
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error when running blink applier", e);
            }
        }
    }

    private class BlinkNodeRemover implements Runnable {

        private final Element element;
        private final Node toRemove;

        public BlinkNodeRemover(Element element, Node toRemove) {
            this.element = element;
            this.toRemove = toRemove;
        }

        @Override
        public void run() {
            try {
                if(nodeAdded) {
                    element.removeChild(toRemove);
                    nodeAdded = false;
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error when running blink remover", e);
            }
        }
    }

}
