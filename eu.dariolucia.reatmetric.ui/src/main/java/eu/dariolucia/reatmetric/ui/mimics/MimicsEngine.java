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

package eu.dariolucia.reatmetric.ui.mimics;

import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import eu.dariolucia.reatmetric.ui.controller.MimicsSvgViewController;
import javafx.application.Platform;
import org.w3c.dom.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MimicsEngine {

    private static final Logger LOG = Logger.getLogger(MimicsEngine.class.getName());

    public static final String DATA_RTMT_BINDING_ID = "data-rtmt-binding-id";

    private Document svgDom;

    private Map<SystemEntityPath, List<SvgElementProcessor>> path2processors;

    public MimicsEngine(Document svgDom) {
        this.svgDom = svgDom;
        this.path2processors = new HashMap<>();
    }

    public void initialise() {
        // Navigate and get all elements that have an id set and a data-rtmt-binding-id set. For each element matching this,
        // process it further to initialise the SvgElementProcessor
        navigate(this.svgDom);
    }

    private void navigate(Node node) {
        if(node instanceof Element) {
            processElement((Element) node);
        }
        for(int i = 0; i < node.getChildNodes().getLength(); ++i) {
            navigate(node.getChildNodes().item(i));
        }
    }

    private void processElement(Element element) {
        NamedNodeMap map = element.getAttributes();
        String reatmetricParameter = null;
        // need to iterate because getAttribute does not work...
        for(int i = 0; i < map.getLength(); ++i) {
            if(map.item(i).getNodeName().equals(DATA_RTMT_BINDING_ID)) {
                reatmetricParameter = map.item(i).getNodeValue();
            }
        }
        if(reatmetricParameter != null && !reatmetricParameter.isBlank()) {
            // Good element, build an SvgElementProcessor and initialise it
            SvgElementProcessor processor = new SvgElementProcessor(element);
            processor.initialise();
            path2processors.computeIfAbsent(SystemEntityPath.fromString(reatmetricParameter), o -> new ArrayList<>()).add(processor);
        }
        // FIXME: remove
        if(element.getTagName().equals("animate")) {
            fullPrint(element);
        }
    }

    public static void fullPrint(Element element) {
        System.out.println("Element: " + element.getTagName() + " - NS URI: " + element.getNamespaceURI() + " - Base URI: " + element.getBaseURI() + " - Local Name: " + element.getLocalName());
        System.out.println("Node Name: " + element.getNodeName() + " - Node Value: " + element.getNodeValue() + " - Node Type: " + element.getNodeType() + " - Prefix: " + element.getPrefix());
        NamedNodeMap map = element.getAttributes();
        // need to iterate because getAttribute does not work...
        for(int i = 0; i < map.getLength(); ++i) {
            Attr attr = (Attr) map.item(i);
            System.out.println("\t Attribute: " + attr.getName() + " - NS URI: " + attr.getNamespaceURI() + " - Base URI: " + attr.getBaseURI() + " - Local Name: " + attr.getLocalName() + " - Value: " + attr.getValue());
            System.out.println("\t\tNode Name: " + attr.getNodeName() + " - Node Value: " + attr.getNodeValue() + " - Node Type: " + attr.getNodeType() + " - Prefix: " + attr.getPrefix());
        }
        for(int i = 0; i < element.getChildNodes().getLength(); ++i) {
            if (element.getChildNodes().item(i) instanceof Element){
                fullPrint((Element) element.getChildNodes().item(i));
            }
        }
    }

    public Set<String> getParameters() {
        return path2processors.keySet().stream().map(SystemEntityPath::asString).collect(Collectors.toSet());
    }

    public void refresh(Map<SystemEntityPath, ParameterData> parameters, Set<SystemEntityPath> updatedItems) {
        final List<ParameterData> toProcess = new ArrayList<>(updatedItems.size());
        for(SystemEntityPath updated : updatedItems) {
            ParameterData parameterData = parameters.get(updated);
            toProcess.add(parameterData);
        }
        // The construction of the operations to be done is done by a different thread
        // The application is done by the UI thread
        ReatmetricUI.threadPool(MimicsSvgViewController.class).execute(() -> {
            List<Runnable> toBeApplied = new ArrayList<>();
            for(ParameterData parameterData : toProcess) {
                List<SvgElementProcessor> processors = path2processors.get(parameterData.getPath());
                if (processors != null) {
                    for (SvgElementProcessor proc : processors) {
                        Runnable run = proc.buildUpdate(parameterData); // The Runnable is a mere applier of the change to the DOM. Good for further optimization.
                        toBeApplied.add(run);
                    }
                }
            }
            // Now run
            Platform.runLater(() -> {
                toBeApplied.forEach(Runnable::run);
            });
        });
    }
}
