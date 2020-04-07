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

import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.ui.mimics.impl.FillAttributeProcessor;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.*;

public class SvgElementProcessor {

    public static final String FILL_PREFIX = "data-rtmt-fill-color";
    public static final String STROKE_PREFIX = "data-rtmt-stroke-color";
    public static final String VISIBILITY_PREFIX = "data-rtmt-visibility";
    public static final String TEXT_PREFIX = "data-rtmt-text";
    public static final String TRANSFORM_PREFIX = "data-rtmt-transform";
    public static final String BLINK_PREFIX = "data-rtmt-blink";

    private Element element;
    private String reatmetricParameter;

    private Map<SvgAttributeType, List<SvgAttributeProcessor>> type2processorList = new EnumMap<>(SvgAttributeType.class);

    public SvgElementProcessor(String reatmetricParameter, Element element) {
        this.element = element;
        this.reatmetricParameter = reatmetricParameter;
    }

    public void initialise() {
        NamedNodeMap attributesMap = element.getAttributes();
        if(attributesMap != null) {
            for(int i = 0; i < attributesMap.getLength(); ++i) {
                Attr attribute = (Attr) attributesMap.item(i);
                if(attribute.getName().startsWith(FILL_PREFIX)) {
                    buildFillProcessor(attribute);
                }
                if(attribute.getName().startsWith(STROKE_PREFIX)) {
                    buildStrokeProcessor(attribute);
                }
                if(attribute.getName().startsWith(VISIBILITY_PREFIX)) {
                    buildVisibilityProcessor(attribute);
                }
                if(attribute.getName().startsWith(TEXT_PREFIX)) {
                    buildTextProcessor(attribute);
                }
                if(attribute.getName().startsWith(TRANSFORM_PREFIX)) {
                    buildTransformProcessor(attribute);
                }
                if(attribute.getName().startsWith(BLINK_PREFIX)) {
                    buildBlinkProcessor(attribute);
                }
            }
        }
        // Now sort lists
        for(List<SvgAttributeProcessor> list : type2processorList.values()) {
            Collections.sort(list);
        }
        // Ready
    }

    private void buildBlinkProcessor(Attr attribute) {
    }

    private void buildTransformProcessor(Attr attribute) {
    }

    private void buildTextProcessor(Attr attribute) {
    }

    private void buildVisibilityProcessor(Attr attribute) {
    }

    private void buildStrokeProcessor(Attr attribute) {
    }

    private void buildFillProcessor(Attr attribute) {
        FillAttributeProcessor proc = new FillAttributeProcessor(element, attribute.getName(), attribute.getValue());
        type2processorList.computeIfAbsent(SvgAttributeType.FILL, o -> new ArrayList<>()).add(proc);
    }

    public Runnable buildUpdate(ParameterData parameterData) {
        CompositeRunnable runnable = new CompositeRunnable();
        for(SvgAttributeType type : SvgAttributeType.values()) {
            List<SvgAttributeProcessor> procs = type2processorList.get(type);
            if(procs != null) {
                for(SvgAttributeProcessor processor : procs) {
                    if(processor.apply(parameterData)) {
                        runnable.add(processor.buildUpdate(parameterData));
                        break;
                    }
                }
            }
        }
        return runnable;
    }

    private static class CompositeRunnable implements Runnable {

        private List<Runnable> runnables = new ArrayList<>();

        public void add(Runnable r) {
            runnables.add(r);
        }

        @Override
        public void run() {
            runnables.forEach(Runnable::run);
        }
    }
}
