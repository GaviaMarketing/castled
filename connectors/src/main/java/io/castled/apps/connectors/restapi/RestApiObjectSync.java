package io.castled.apps.connectors.restapi;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.castled.ObjectRegistry;
import io.castled.apps.BufferedObjectSink;
import io.castled.apps.models.DataSinkRequest;
import io.castled.commons.errors.errorclassifications.UnclassifiedError;
import io.castled.commons.models.MessageSyncStats;
import io.castled.commons.streams.ErrorOutputStream;
import io.castled.core.CastledOffsetListQueue;
import io.castled.models.TargetRestApiMapping;
import io.castled.schema.SchemaUtils;
import io.castled.schema.models.Field;
import io.castled.schema.models.Message;
import io.castled.schema.models.Tuple;
import io.castled.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class RestApiObjectSync extends BufferedObjectSink<Message> {


    private final RestApiTemplateClient restApiRestClient;
    private final RestApiErrorParser restApiErrorParser;
    private final ErrorOutputStream errorOutputStream;
    private final AtomicLong processedRecords = new AtomicLong(0);
    private final Integer batchSize;
    private final CastledOffsetListQueue<Message> requestsBuffer;
    private long lastProcessedOffset = 0;

    public RestApiObjectSync(DataSinkRequest dataSinkRequest) {
        RestApiAppSyncConfig restApiAppSyncConfig = (RestApiAppSyncConfig) dataSinkRequest.getAppSyncConfig();
        this.batchSize = Optional.ofNullable(restApiAppSyncConfig.getBatchSize()).orElse(1);
        this.restApiRestClient = new RestApiTemplateClient((TargetRestApiMapping) dataSinkRequest.getMapping(),
                (RestApiAppSyncConfig) dataSinkRequest.getAppSyncConfig());
        this.errorOutputStream = dataSinkRequest.getErrorOutputStream();
        this.restApiErrorParser = ObjectRegistry.getInstance(RestApiErrorParser.class);

        this.requestsBuffer = new CastledOffsetListQueue<>(new UpsertRestApiObjectConsumer(), restApiAppSyncConfig.getParallelism()
                , restApiAppSyncConfig.getParallelism(), true);
    }

    @Override
    protected void writeRecords(List<Message> messages) {
        try {
            requestsBuffer.writePayload(Lists.newArrayList(messages), 5, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            log.error("Unable to publish records to records queue", e);
            for (Message record : messages) {
                errorOutputStream.writeFailedRecord(record,
                        new UnclassifiedError("Internal error!! Unable to publish records to records queue. Please contact support"));
            }
        }
    }

    @Override
    public long getMaxBufferedObjects() {
        return batchSize;
    }

    public MessageSyncStats getSyncStats() {
        return new MessageSyncStats(0, 0);
    }

    public void flushRecords() throws Exception {
        super.flushRecords();
        requestsBuffer.flush(TimeUtils.minutesToMillis(10));
    }

    private Map<String, Object> constructProperties(Tuple record) {
        Map<String, Object> recordProperties = Maps.newHashMap();
        for (Field field : record.getFields()) {
            Object value = record.getValue(field.getName());
            if (value != null) {
                if (SchemaUtils.isZonedTimestamp(field.getSchema())) {
                    recordProperties.put(field.getName(), ((ZonedDateTime) value).toEpochSecond());
                } else {
                    recordProperties.put(field.getName(), value);
                }
            }
        }
        return recordProperties;
    }

    private void upsertRestApiObjects(List<Message> messages) {
        ErrorAndCode errorObject = this.restApiRestClient.upsertDetails(messages.stream()
                .map(Message::getRecord).map(this::constructProperties).collect(Collectors.toList()));

        Optional.ofNullable(errorObject).ifPresent((objectAndErrorRef) -> messages.
                forEach(message -> this.errorOutputStream.writeFailedRecord(message, restApiErrorParser.getPipelineError(objectAndErrorRef.getCode(), objectAndErrorRef.getMessage()))));

        this.processedRecords.addAndGet(messages.size());
        this.lastProcessedOffset = Math.max(lastProcessedOffset, Iterables.getLast(messages).getOffset());
    }

    private class UpsertRestApiObjectConsumer implements Consumer<List<Message>> {
        @Override
        public void accept(List<Message> messages) {
            if (CollectionUtils.isEmpty(messages)) {
                return;
            }
            upsertRestApiObjects(messages);
        }
    }
}
