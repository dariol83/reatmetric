package eu.dariolucia.reatmetric.persist.services;

import eu.dariolucia.reatmetric.api.common.RetrievalDirection;
import eu.dariolucia.reatmetric.api.messages.OperationalMessage;
import eu.dariolucia.reatmetric.api.messages.OperationalMessageFilter;
import eu.dariolucia.reatmetric.persist.Archive;

import java.sql.Connection;
import java.time.Instant;
import java.util.List;

public class OperationalMessageArchive extends AbstractDataItemArchive<OperationalMessage, OperationalMessageFilter> {

    public OperationalMessageArchive(Archive controller) {
        super(controller);
    }

    @Override
    protected void doStore(Connection connection, List<OperationalMessage> itemsToStore) {
        // https://stackoverflow.com/questions/18134561/how-to-insert-list-products-into-database
    }

    @Override
    protected List<OperationalMessage> doRetrieve(Connection connection, Instant startTime, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) {
        return null;
    }

    @Override
    protected List<OperationalMessage> doRetrieve(Connection connection, OperationalMessage startItem, int numRecords, RetrievalDirection direction, OperationalMessageFilter filter) {
        return null;
    }
}
