package io.castled.apps.connectors.mixpanel;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import io.castled.ObjectRegistry;
import io.castled.apps.connectors.mixpanel.dto.GroupProfileAndError;
import io.castled.apps.models.DataSinkRequest;
import io.castled.commons.errors.errorclassifications.UnclassifiedError;
import io.castled.commons.models.MessageSyncStats;
import io.castled.commons.streams.ErrorOutputStream;
import io.castled.core.CastledOffsetListQueue;
import io.castled.schema.models.Field;
import io.castled.schema.models.Message;
import io.castled.schema.models.Tuple;
import io.castled.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


@Singleton
@Slf4j
public class MixpanelGroupProfileSink extends MixpanelObjectSink<Message> {

    private final MixpanelRestClient mixpanelRestClient;
    private final MixpanelErrorParser mixpanelErrorParser;
    private final ErrorOutputStream errorOutputStream;
    private final AtomicLong processedRecords = new AtomicLong(0);
    private long lastProcessedOffset = 0;
    private final MixpanelAppSyncConfig syncConfig;

    private final CastledOffsetListQueue<Message> requestsBuffer =
            new CastledOffsetListQueue<>(new UpsertGroupProfileConsumer(), 10, 10, true);

    public MixpanelGroupProfileSink(DataSinkRequest dataSinkRequest) {
        this.mixpanelRestClient = new MixpanelRestClient(((MixpanelAppConfig) dataSinkRequest.getExternalApp().getConfig()).getProjectToken(),
                ((MixpanelAppConfig) dataSinkRequest.getExternalApp().getConfig()).getApiSecret());
        this.errorOutputStream = dataSinkRequest.getErrorOutputStream();
        this.mixpanelErrorParser = ObjectRegistry.getInstance(MixpanelErrorParser.class);
        this.syncConfig = (MixpanelAppSyncConfig) dataSinkRequest.getAppSyncConfig();
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

    private Object getGroupID(Tuple record) {
        return record.getValue(MixpanelObjectFields.GROUP_PROFILE_FIELDS.GROUP_ID.getFieldName());
    }

    private Map<String,Object> constructGroupProfileDetails(Tuple record) {
        Map<String,Object> groupProfileInfo = Maps.newHashMap();
        groupProfileInfo.put("$group_key",syncConfig.getGroupKey());
        groupProfileInfo.put("$group_id",getGroupID(record));
        groupProfileInfo.put("$set",constructPropertyMap(record));
        return groupProfileInfo;
    }

    private Map<String, Object> constructPropertyMap(Tuple record) {
        return record.getFields().stream().
                filter(field -> !isMixpanelReservedKeyword(field.getName())).collect(Collectors.toMap(Field::getName, Field::getValue));
    }

    private boolean isMixpanelReservedKeyword(String fieldName)
    {
        return getReservedKeywords().contains(fieldName);
    }

    private List<String> getReservedKeywords()
    {
        return Lists.newArrayList("group_id","group_key");
    }

    @Override
    public MessageSyncStats getSyncStats() {
        return new MessageSyncStats(processedRecords.get(), lastProcessedOffset);
    }

    @Override
    public long getMaxBufferedObjects() {
        return 200;
    }

    public void flushRecords() throws Exception {
        super.flushRecords();
        requestsBuffer.flush(TimeUtils.minutesToMillis(10));
    }

    private class UpsertGroupProfileConsumer implements Consumer<List<Message>> {
        @Override
        public void accept(List<Message> messages) {
            if (CollectionUtils.isEmpty(messages)) {
                return;
            }
            processBulkGroupProfileUpdate(messages);
        }
    }

    private void processBulkGroupProfileUpdate(List<Message> messages) {
        List<GroupProfileAndError> failedRecords = this.mixpanelRestClient.upsertGroupProfileDetails(
                messages.stream().map(Message::getRecord).map(this::constructGroupProfileDetails).collect(Collectors.toList()));

        Map<Object, Message> groupProfileRecordMapper = messages.stream().filter(message -> getGroupID(message.getRecord()) != null)
                .collect(Collectors.toMap(message -> getGroupID(message.getRecord()), Function.identity()));

        failedRecords.forEach(failedRecord ->
                failedRecord.getFailureReasons().forEach(failureReason -> this.errorOutputStream.writeFailedRecord(groupProfileRecordMapper.get(failedRecord.getGroupID()),
                        mixpanelErrorParser.getPipelineError(failureReason))));

        this.processedRecords.addAndGet(messages.size());
        this.lastProcessedOffset = Math.max(lastProcessedOffset, Iterables.getLast(messages).getOffset());
    }
}
