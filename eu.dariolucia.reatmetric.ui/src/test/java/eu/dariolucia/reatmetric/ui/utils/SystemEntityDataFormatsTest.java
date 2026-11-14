/*
 * Copyright (c)  2026 Dario Lucia (https://www.dariolucia.eu)
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

import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.Status;
import eu.dariolucia.reatmetric.api.model.SystemEntity;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.model.SystemEntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats.deserialize;
import static eu.dariolucia.reatmetric.ui.utils.SystemEntityDataFormats.serialize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemEntityDataFormatsTest {

    @ParameterizedTest
    @EnumSource(SystemEntityType.class)
    void roundTripPreservesEveryEntityType(SystemEntityType type) {
        SystemEntity original = entity(17, "Spacecraft.Subsystem.Entity", type);

        assertEquals(List.of(reference(original)), deserialize(serialize(List.of(original)), type));
        assertEquals(type.name(), SystemEntityDataFormats.getByType(type).getIdentifiers().iterator().next());
    }

    @Test
    void containerRetainsRootAndVisibleChildrenInTheirOriginalOrder() {
        List<SystemEntity> entities = List.of(
                entity(Integer.MIN_VALUE, "Spacecraft.Subsystem", SystemEntityType.CONTAINER),
                entity(7, "Spacecraft.Subsystem.Temperature", SystemEntityType.PARAMETER),
                entity(9, "Spacecraft.Subsystem.Alarm", SystemEntityType.EVENT),
                entity(1, "Spacecraft.Subsystem.PowerOn", SystemEntityType.ACTIVITY),
                entity(Integer.MAX_VALUE, "Spacecraft.Subsystem.Summary", SystemEntityType.REPORT),
                entity(3, "Spacecraft.Subsystem.Nested", SystemEntityType.CONTAINER));

        var restored = deserialize(serialize(entities), SystemEntityType.CONTAINER);

        assertEquals(entities.stream().map(SystemEntityDataFormatsTest::reference).toList(), restored);
        assertThrows(UnsupportedOperationException.class, () -> restored.add(restored.get(0)));
    }

    @Test
    void pathsPreserveUnicodeAndWireDelimiters() {
        SystemEntity original = entity(0, "Spacecraft.T\u00e9l\u00e9m\u00e9trie_\u6e29\u5ea6_\ud83d\ude80\t\n\r|+/=", SystemEntityType.PARAMETER);

        assertEquals(List.of(reference(original)), deserialize(serialize(List.of(original)), SystemEntityType.PARAMETER));
    }

    @Test
    void wrongRootTypeAndMultipleNonContainerEntitiesAreRejected() {
        SystemEntity root = entity(1, "Root", SystemEntityType.PARAMETER);
        SystemEntity child = entity(2, "Root.Child", SystemEntityType.PARAMETER);

        assertTrue(deserialize(serialize(List.of(root)), SystemEntityType.EVENT).isEmpty());
        assertTrue(deserialize(serialize(List.of(root, child)), SystemEntityType.PARAMETER).isEmpty());
    }

    @Test
    void containerOnlyAcceptsItsOwnDirectChildren() {
        SystemEntity root = entity(1, "Root.Container", SystemEntityType.CONTAINER);
        for (String path : List.of("Root.Other.Child", "Root.Container.Nested.Child", "Root.Container", "Unrelated")) {
            var child = entity(2, path, SystemEntityType.PARAMETER);
            assertTrue(deserialize(serialize(List.of(root, child)), SystemEntityType.CONTAINER).isEmpty(), path);
        }
    }

    @Test
    void malformedAndUnsupportedWirePayloadsAreRejected() {
        // Literal fixtures deliberately cover the wire boundary independently of the encoder.
        for (String payload : List.of(
                "", "reatmetric-system-entities:1", "reatmetric-system-entities:1\n",
                "reatmetric-system-entities:2\nPARAMETER\t1\tUm9vdA==",
                "different-format:1\nPARAMETER\t1\tUm9vdA==",
                "reatmetric-system-entities:1\nUNKNOWN\t1\tUm9vdA==",
                "reatmetric-system-entities:1\nPARAMETER\tunknown\tUm9vdA==",
                "reatmetric-system-entities:1\nPARAMETER\t2147483648\tUm9vdA==",
                "reatmetric-system-entities:1\nPARAMETER\t1",
                "reatmetric-system-entities:1\nPARAMETER\t1\tUm9vdA==\textra",
                "reatmetric-system-entities:1\nPARAMETER\t1\t%invalid%",
                "reatmetric-system-entities:1\nPARAMETER\t1\tU",
                "reatmetric-system-entities:1\nPARAMETER\t1\twyg=",
                "reatmetric-system-entities:1\nPARAMETER\t1\t",
                "reatmetric-system-entities:1\nPARAMETER\t1\tUm9vdA==\n")) {
            assertTrue(deserialize(buffer(payload), SystemEntityType.PARAMETER).isEmpty(), payload);
        }
        assertTrue(deserialize(ByteBuffer.wrap(new byte[] {(byte) 0xc3, 0x28}), SystemEntityType.PARAMETER).isEmpty());
    }

    @Test
    void legacyObjectsAndJavaSerializedGraphsAreRejected() throws Exception {
        SystemEntity original = entity(1, "Root", SystemEntityType.PARAMETER);
        for (Object payload : List.of(original, List.of(original), new byte[] {1, 2}, "Root")) {
            assertTrue(deserialize(payload, SystemEntityType.PARAMETER).isEmpty());
        }
        assertTrue(deserialize(null, SystemEntityType.PARAMETER).isEmpty());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(bytes)) {
            stream.writeObject(List.of(original));
        }
        assertTrue(deserialize(ByteBuffer.wrap(bytes.toByteArray()), SystemEntityType.PARAMETER).isEmpty());
    }

    @Test
    void bufferViewsUseOnlyRemainingBytesAndPreservePositionLimitAndMark() {
        SystemEntity original = entity(42, "Spacecraft.Temperature", SystemEntityType.PARAMETER);
        ByteBuffer encoded = serialize(List.of(original));
        for (boolean direct : List.of(false, true)) {
            ByteBuffer padded = direct ? ByteBuffer.allocateDirect(encoded.remaining() + 8)
                    : ByteBuffer.allocate(encoded.remaining() + 8);
            padded.position(3);
            padded.put(encoded.duplicate());
            padded.limit(padded.position());
            padded.position(3);
            for (ByteBuffer view : List.of(padded, padded.asReadOnlyBuffer(), padded.slice(), padded.slice().asReadOnlyBuffer())) {
                int position = view.position();
                int limit = view.limit();
                view.mark();

                assertEquals(List.of(reference(original)), deserialize(view, SystemEntityType.PARAMETER));
                assertEquals(position, view.position());
                assertEquals(limit, view.limit());
                view.reset();
                assertEquals(position, view.position());
            }
        }
    }

    @Test
    void emptyAndTruncatedBuffersAreRejectedWithoutBeingConsumed() {
        ByteBuffer truncated = serialize(List.of(entity(1, "Root", SystemEntityType.PARAMETER)));
        truncated.limit(12);
        for (ByteBuffer buffer : List.of(ByteBuffer.allocate(0), truncated, truncated.asReadOnlyBuffer())) {
            int position = buffer.position();
            int limit = buffer.limit();
            assertTrue(deserialize(buffer, SystemEntityType.PARAMETER).isEmpty());
            assertEquals(position, buffer.position());
            assertEquals(limit, buffer.limit());
        }
        assertThrows(IllegalArgumentException.class, () -> serialize(List.of()));
    }

    private static SystemEntity entity(int id, String path, SystemEntityType type) {
        return new SystemEntity(null, id, SystemEntityPath.fromString(path), Status.ENABLED, AlarmState.NOMINAL, type);
    }

    private static SystemEntityDataFormats.EntityReference reference(SystemEntity entity) {
        return new SystemEntityDataFormats.EntityReference(entity.getExternalId(), entity.getPath(), entity.getType());
    }

    private static ByteBuffer buffer(String text) {
        return ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
    }
}
