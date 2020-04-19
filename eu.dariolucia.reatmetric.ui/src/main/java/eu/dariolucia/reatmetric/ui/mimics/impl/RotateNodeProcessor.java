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
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.logging.Level;
import java.util.logging.Logger;


public class RotateNodeProcessor extends SvgAttributeProcessor {

    private static final Logger LOG = Logger.getLogger(RotateNodeProcessor.class.getName());

    private volatile Runnable cachedOperation;

    public RotateNodeProcessor(Element element, String name, String value) {
        super(element, name, value);
    }

    @Override
    public Runnable buildUpdate(ParameterData parameterData) {
        try {
            String valueExpression = expression.apply(parameterData);
            // Build the node operation once
            if(cachedOperation == null) {
                if(valueExpression.equals(SvgConstants.NO_ROTATE)) {
                    cachedOperation = new RotateNodeRemover(element);
                } else {
                    cachedOperation = new RotateNodeApplier(element, deriveSplitValues(valueExpression));
                }
            } else {
                // If it is a rotate, set the new value
                if(cachedOperation instanceof RotateNodeApplier) {
                    ((RotateNodeApplier) cachedOperation).setRotateValues(deriveSplitValues(valueExpression));
                }
            }
            return cachedOperation;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error when building rotate update", e);
            return () -> {};
        }
    }

    private static String[] deriveSplitValues(String value) {
        String duration = value.substring(0, value.indexOf(" "));
        String center = value.substring(duration.length()).trim();
        return new String[] {duration, center};
    }

    /**
     * Remove the first "animateTransform" node from the provided element, if the node is not equal to the provided animateNode.
     *
     * @param element
     * @param animateNode
     * @return true if the animateTransform element was found and removed, or was not found, otherwise false
     */
    private static boolean removeAnimateNodeIfNotEqual(Node element, Node animateNode) {
        for(int i = 0; i < element.getChildNodes().getLength(); ++i) {
            Node child = element.getChildNodes().item(i);
            if(child instanceof Element) {
                if(((Element) child).getTagName().equals("animateTransform")) {
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

    private static class RotateNodeApplier implements Runnable {

        private final Element element;
        private volatile String duration;
        private volatile String center;
        private volatile boolean valueUpdates;
        private Element toAdd;

        public RotateNodeApplier(Element element, String[] values) {
            this.element = element;
            this.duration = values[0];
            this.center = values[1];
            this.valueUpdates = true;
        }

        public void setRotateValues(String[] values) {
            this.valueUpdates = !values[0].equals(this.duration) || !values[1].equals(this.center);
            this.duration = values[0];
            this.center = values[1];
        }

        @Override
        public void run() {
            try {
                // No update? Do nothing.
                if(toAdd != null && !this.valueUpdates) {
                    return;
                }
                if(toAdd == null) {
                    toAdd = element.getOwnerDocument().createElementNS("http://www.w3.org/2000/svg", "animateTransform");
                    toAdd.setAttribute("attributeType", "XML");
                    toAdd.setAttribute("attributeName", "transform");
                    toAdd.setAttribute("type", "rotate");
                    toAdd.setAttribute("repeatCount", "indefinite");
                }
                toAdd.setAttribute("from", "0 " + center);
                toAdd.setAttribute("to", "360 " + center);
                toAdd.setAttribute("dur", duration + "ms");
                // First remove the "animateTransform" node if any. If the node to remove is exactly equals to toAdd, don't do anything, you are done
                if(removeAnimateNodeIfNotEqual(element, toAdd)) {
                    element.appendChild(toAdd);
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error when running rotate applier", e);
            }
        }
    }

    private static class RotateNodeRemover implements Runnable {

        private final Element element;

        public RotateNodeRemover(Element element) {
            this.element = element;
        }

        @Override
        public void run() {
            try {
                removeAnimateNodeIfNotEqual(element, null);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error when running rotate remover", e);
            }
        }
    }

}
