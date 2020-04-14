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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TextNodeProcessor extends SvgAttributeProcessor {

    private static final Logger LOG = Logger.getLogger(TextNodeProcessor.class.getName());

    private volatile Runnable cachedOperation;

    public TextNodeProcessor(Element element, String name, String value) {
        super(element, name, value);
    }

    @Override
    public Runnable buildUpdate(ParameterData parameterData) {
        try {
            String textToDisplay = expression.apply(parameterData);
            // Build the node operation once
            if(cachedOperation == null) {
                cachedOperation = new TextNodeApplier(element, textToDisplay);
            } else {
                // If it is an applier, set the new value
                if(cachedOperation instanceof TextNodeApplier) {
                    ((TextNodeApplier) cachedOperation).setText(textToDisplay);
                }
            }
            return cachedOperation;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error when building text update", e);
            return () -> {};
        }
    }

    /**
     * Remove the first "text" node from the provided element, if the node is not equal to the provided textNode.
     *
     * @param element
     * @param textNode
     * @return true if the text element was found and removed, or was not found, otherwise false
     */
    private static boolean removeTextNodeIfNotEqual(Node element, Node textNode) {
        for(int i = 0; i < element.getChildNodes().getLength(); ++i) {
            Node child = element.getChildNodes().item(i);
            if(child instanceof Text) {
                // Found the node: if not equal, remove, otherwise done
                if(child != textNode) {
                    element.removeChild(child);
                    return true;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private static class TextNodeApplier implements Runnable {

        private final Element element;
        private volatile String text;
        private volatile boolean textUpdate;
        private Text toAdd;

        public TextNodeApplier(Element element, String text) {
            this.element = element;
            this.text = text;
            if(this.text == null) {
                this.text = "";
            }
            this.textUpdate = true;
        }

        public void setText(String text) {
            if(text == null) {
                text = "";
            }
            this.textUpdate = Objects.equals(text, this.text);
            this.text = text;
        }

        @Override
        public void run() {
            try {
                // No text update? Do nothing.
                if(!this.textUpdate) {
                    return;
                }
                if(toAdd == null) {
                    toAdd = element.getOwnerDocument().createTextNode(this.text);
                }
                toAdd.setTextContent(this.text);
                // First remove the text node if any. If the node to remove is exactly equals to toAdd, don't do anything, you are done
                if(removeTextNodeIfNotEqual(element, toAdd)) {
                    element.appendChild(toAdd);
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error when running blink applier", e);
            }
        }
    }
}
