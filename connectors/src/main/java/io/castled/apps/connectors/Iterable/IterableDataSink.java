package io.castled.apps.connectors.Iterable;

import io.castled.apps.DataSink;
import io.castled.apps.models.DataSinkRequest;
import io.castled.commons.models.AppSyncStats;
import io.castled.commons.models.DataSinkMessage;

import java.util.Optional;

public class IterableDataSink implements DataSink {

    private IterableBufferedObjectSink iterableBufferedObjectSink;

    @Override
    public void syncRecords(DataSinkRequest dataSinkRequest) throws Exception {

        this.iterableBufferedObjectSink = new IterableBufferedObjectSink((IterableAppConfig) dataSinkRequest.getExternalApp().getConfig(),
                (IterableSyncConfig) dataSinkRequest.getAppSyncConfig(), dataSinkRequest.getErrorOutputStream());

        DataSinkMessage msg;
        while ((msg = dataSinkRequest.getMessageInputStream().readMessage()) != null) {
            this.iterableBufferedObjectSink.writeRecord(msg);
        }
        this.iterableBufferedObjectSink.flushRecords();
    }

    @Override
    public AppSyncStats getSyncStats() {
        return Optional.ofNullable(iterableBufferedObjectSink).map(sinkRef -> sinkRef.getSyncStats())
                .orElse(new AppSyncStats(0, 0, 0));
    }
}
