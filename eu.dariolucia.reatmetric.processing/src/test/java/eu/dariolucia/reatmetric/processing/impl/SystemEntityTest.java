/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.impl;

import eu.dariolucia.reatmetric.api.common.AbstractDataItem;
import eu.dariolucia.reatmetric.api.model.*;
import eu.dariolucia.reatmetric.api.processing.IProcessingModel;
import eu.dariolucia.reatmetric.api.processing.IProcessingModelOutput;
import eu.dariolucia.reatmetric.api.processing.exceptions.ProcessingModelException;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
import eu.dariolucia.reatmetric.api.processing.input.ParameterSample;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SystemEntityTest {

    @Test
    void testSystemEntityQueries() throws JAXBException, ProcessingModelException, InterruptedException {
        ProcessingDefinition pd = ProcessingDefinition.load(this.getClass().getClassLoader().getResourceAsStream("processing_definitions_system_entity.xml"));
        ProcessingModelFactoryImpl factory = new ProcessingModelFactoryImpl();
        List<AbstractDataItem> outList = new CopyOnWriteArrayList<>();
        // All output data items go in the outList
        IProcessingModelOutput output = outList::addAll;
        IProcessingModel model = factory.build(pd, output, null);

        SystemEntity root = model.getRoot();
        assertEquals("ROOT", root.getName());
        assertEquals(Status.ENABLED, root.getStatus());
        assertEquals(SystemEntityType.CONTAINER, root.getType());

        List<SystemEntity> entities = model.getContainedEntities(root.getPath());
        assertEquals(3, entities.size()); // EVT, ELEMENT1, BATTERY

        // Get children of ROOT.EVT
        entities = model.getContainedEntities(SystemEntityPath.fromString("ROOT.EVT"));
        assertEquals(4, entities.size()); // PARAM1, PARAM2, EVT1, EVT2
        for(SystemEntity se : entities) {
            if(se.getName().startsWith("EVT")) {
                assertEquals(SystemEntityType.EVENT, se.getType());
            } else {
                assertEquals(SystemEntityType.PARAMETER, se.getType());
            }
        }

        // Get children of ROOT.ELEMENT1
        entities = model.getContainedEntities(SystemEntityPath.fromString("ROOT.ELEMENT1"));
        assertEquals(3, entities.size()); // PARAM1, PARAM2, ELEMENT2
        for(SystemEntity se : entities) {
            if(se.getName().startsWith("PARAM")) {
                assertEquals(SystemEntityType.PARAMETER, se.getType());
            } else {
                assertEquals(SystemEntityType.CONTAINER, se.getType());
            }
        }

        SystemEntity se = model.getSystemEntityAt(SystemEntityPath.fromString("ROOT.ELEMENT1.ELEMENT2.PARAM1"));
        assertNotNull(se);
        assertEquals(Status.ENABLED, se.getStatus());
        assertEquals(SystemEntityType.PARAMETER, se.getType());
        assertEquals(AlarmState.UNKNOWN, se.getAlarmState());

        ParameterSample sample = ParameterSample.of(1005, 3);
        model.injectParameters(Collections.singletonList(sample));

        //
        AwaitUtil.awaitAndVerify(5000, outList::size, 2);
        se = model.getSystemEntityAt(SystemEntityPath.fromString("ROOT.ELEMENT1.ELEMENT2.PARAM1"));
        assertNotNull(se);
        assertEquals(AlarmState.NOT_CHECKED, se.getAlarmState());

        SystemEntity se2 = model.getSystemEntityOf(1005);
        assertNotNull(se2);
        assertEquals(AlarmState.NOT_CHECKED, se2.getAlarmState());
        assertEquals(se, se2);

        assertEquals(2001, model.getExternalIdOf(SystemEntityPath.fromString("ROOT.EVT.EVT1")));
        assertEquals(SystemEntityPath.fromString("ROOT.EVT.EVT1"), model.getPathOf(2001));
        assertEquals(2002, model.getExternalIdOf(SystemEntityPath.fromString("ROOT.EVT.EVT2")));
        assertEquals(1005, model.getExternalIdOf(SystemEntityPath.fromString("ROOT.ELEMENT1.ELEMENT2.PARAM1")));
    }
}
