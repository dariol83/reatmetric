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
import eu.dariolucia.reatmetric.ui.mimics.impl.*;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.*;

import static eu.dariolucia.reatmetric.ui.mimics.SvgConstants.*;

public class SvgElementProcessor {

    private final Element element;
    private final Map<SvgAttributeType, List<SvgAttributeProcessor>> type2processorList = new EnumMap<>(SvgAttributeType.class);

    public SvgElementProcessor(Element element) {
        this.element = element;
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
                if(attribute.getName().startsWith(STROKE_WIDTH_PREFIX)) {
                    buildStrokeWidthProcessor(attribute);
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
        BlinkNodeProcessor proc = new BlinkNodeProcessor(element, attribute.getName(), attribute.getValue());
        type2processorList.computeIfAbsent(SvgAttributeType.BLINK, o -> new ArrayList<>()).add(proc);
    }

    private void buildTransformProcessor(Attr attribute) {
        TransformAttributeProcessor proc = new TransformAttributeProcessor(element, attribute.getName(), attribute.getValue());
        type2processorList.computeIfAbsent(SvgAttributeType.TRANSFORM, o -> new ArrayList<>()).add(proc);
    }

    private void buildTextProcessor(Attr attribute) {
        TextNodeProcessor proc = new TextNodeProcessor(element, attribute.getName(), attribute.getValue());
        type2processorList.computeIfAbsent(SvgAttributeType.TEXT, o -> new ArrayList<>()).add(proc);
    }

    private void buildVisibilityProcessor(Attr attribute) {
        VisibilityAttributeProcessor proc = new VisibilityAttributeProcessor(element, attribute.getName(), attribute.getValue());
        type2processorList.computeIfAbsent(SvgAttributeType.VISIBILITY, o -> new ArrayList<>()).add(proc);
    }

    private void buildStrokeProcessor(Attr attribute) {
        StrokeAttributeProcessor proc = new StrokeAttributeProcessor(element, attribute.getName(), attribute.getValue());
        type2processorList.computeIfAbsent(SvgAttributeType.STROKE, o -> new ArrayList<>()).add(proc);
    }

    private void buildStrokeWidthProcessor(Attr attribute) {
        StrokeWidthAttributeProcessor proc = new StrokeWidthAttributeProcessor(element, attribute.getName(), attribute.getValue());
        type2processorList.computeIfAbsent(SvgAttributeType.STROKE_WIDTH, o -> new ArrayList<>()).add(proc);
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
                    if(processor.test(parameterData)) {
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
