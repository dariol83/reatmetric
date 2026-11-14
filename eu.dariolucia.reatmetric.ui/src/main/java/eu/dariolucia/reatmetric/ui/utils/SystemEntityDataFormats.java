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


package eu.dariolucia.reatmetric.ui.utils;

import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Formats and explicit wire encoding for model-browser drag-and-drop data.
 * Only entity references cross the dragboard, never Java-serialized model objects.
 *
 * @author dario
 */
public class SystemEntityDataFormats {
    private static final String PAYLOAD_HEADER = "reatmetric-system-entities:1";

    public final static DataFormat CONTAINER = new DataFormat(SystemEntityType.CONTAINER.name());
    public final static DataFormat PARAMETER = new DataFormat(SystemEntityType.PARAMETER.name());
    public final static DataFormat EVENT = new DataFormat(SystemEntityType.EVENT.name());
    public final static DataFormat REPORT = new DataFormat(SystemEntityType.REPORT.name());
    public final static DataFormat ACTIVITY = new DataFormat(SystemEntityType.ACTIVITY.name());

    public record EntityReference(int externalId, SystemEntityPath path, SystemEntityType type) {
    }

    /**
     * Encode one entity, or a container followed by its visible direct children.
     * The UTF-8 payload starts with a version header and contains one tab-separated
     * type, external ID and Base64-encoded UTF-8 path per line. Encoding the path
     * separately preserves Unicode and any delimiters in entity names.
     * A ByteBuffer bypasses JavaFX's Java object serialization.
     *
     * @param entities ordered entities to transfer, with the dragged entity first
     * @return encoded bytes, ready to be put on a dragboard
     */
    public static ByteBuffer serialize(List<SystemEntity> entities) {
        if (entities.isEmpty()) {
            throw new IllegalArgumentException("A drag payload must contain an entity");
        }
        StringBuilder payload = new StringBuilder(PAYLOAD_HEADER);
        for (SystemEntity entity : entities) {
            payload.append('\n').append(entity.getType().name()).append('\t')
                    .append(entity.getExternalId()).append('\t')
                    .append(Base64.getEncoder().encodeToString(entity.getPath().asString().getBytes(StandardCharsets.UTF_8)));
        }
        return ByteBuffer.wrap(payload.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Read a payload for the requested entity type. Missing, malformed or
     * unsupported payloads return an empty list so a drop can be rejected.
     *
     * @param clipboard the dragboard to read
     * @param expectedType type of the dragged entity
     * @return validated references, with the dragged entity first, or an empty list
     */
    public static List<EntityReference> read(Clipboard clipboard, SystemEntityType expectedType) {
        return deserialize(clipboard.getContent(getByType(expectedType)), expectedType);
    }

    /**
     * Decode and validate a payload without modifying its buffer position or limit.
     * No Java object deserialization or model-service lookup is performed.
     *
     * @param payload raw clipboard content
     * @param expectedType type of the dragged entity
     * @return validated references, or an empty list for an invalid payload
     */
    public static List<EntityReference> deserialize(Object payload, SystemEntityType expectedType) {
        if (!(payload instanceof ByteBuffer buffer)) {
            return List.of();
        }
        try {
            String text = StandardCharsets.UTF_8.newDecoder().decode(buffer.asReadOnlyBuffer()).toString();
            String[] lines = text.split("\n", -1);
            if (lines.length < 2 || !PAYLOAD_HEADER.equals(lines[0])) {
                return List.of();
            }
            List<EntityReference> entities = new ArrayList<>(lines.length - 1);
            for (int i = 1; i < lines.length; ++i) {
                String[] fields = lines[i].split("\t", -1);
                if (fields.length != 3) {
                    return List.of();
                }
                SystemEntityType type = SystemEntityType.valueOf(fields[0]);
                int externalId = Integer.parseInt(fields[1]);
                String path = StandardCharsets.UTF_8.newDecoder()
                        .decode(ByteBuffer.wrap(Base64.getDecoder().decode(fields[2]))).toString();
                if (path.isEmpty()) {
                    return List.of();
                }
                entities.add(new EntityReference(externalId, SystemEntityPath.fromString(path), type));
            }
            EntityReference root = entities.get(0);
            if (root.type() != expectedType || (expectedType != SystemEntityType.CONTAINER && entities.size() != 1)) {
                return List.of();
            }
            for (int i = 1; i < entities.size(); ++i) {
                if (!root.path().equals(entities.get(i).path().getParent())) {
                    return List.of();
                }
            }
            return List.copyOf(entities);
        } catch (IllegalArgumentException | CharacterCodingException e) {
            return List.of();
        }
    }
    
    public static DataFormat getByType(SystemEntityType t) {
        switch(t) {
            case ACTIVITY: return ACTIVITY;
            case CONTAINER: return CONTAINER;
            case EVENT: return EVENT;
            case REPORT: return REPORT;
            case PARAMETER: return PARAMETER;
            default: throw new IllegalArgumentException("System entity type " + t + " unknown");
        }
    }
}
