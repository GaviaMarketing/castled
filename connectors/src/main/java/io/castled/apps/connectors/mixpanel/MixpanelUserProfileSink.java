package io.castled.apps.connectors.mixpanel;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import io.castled.ObjectRegistry;
import io.castled.apps.connectors.mixpanel.dto.UserProfileAndError;
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
public class MixpanelUserProfileSink extends MixpanelObjectSink<Message> {

    private final MixpanelRestClient mixpanelRestClient;
    private final MixpanelErrorParser mixpanelErrorParser;
    private final ErrorOutputStream errorOutputStream;
    private final MixpanelAppConfig mixpanelAppConfig;
    private final AtomicLong processedRecords = new AtomicLong(0);
    private long lastProcessedOffset = 0;

    private final AtomicLong failedRecords = new AtomicLong(0);

    private final CastledOffsetListQueue<Message> requestsBuffer =
            new CastledOffsetListQueue<>(new UpsertUserProfileConsumer(), 10, 10, true);

    public MixpanelUserProfileSink(DataSinkRequest dataSinkRequest) {
        this.mixpanelRestClient = new MixpanelRestClient(((MixpanelAppConfig) dataSinkRequest.getExternalApp().getConfig()).getProjectToken(),
                ((MixpanelAppConfig) dataSinkRequest.getExternalApp().getConfig()).getApiSecret());
        this.errorOutputStream = dataSinkRequest.getErrorOutputStream();
        this.mixpanelErrorParser = ObjectRegistry.getInstance(MixpanelErrorParser.class);
        this.mixpanelAppConfig = (MixpanelAppConfig) dataSinkRequest.getExternalApp().getConfig();
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

    private Object getDistinctID(Tuple record) {
        return record.getValue(MixpanelObjectFields.USER_PROFILE_FIELDS.DISTINCT_ID.getFieldName());
    }

    private Map<String,Object> constructUserProfileDetails(Tuple record) {

        Object distinctID = record.getValue(MixpanelObjectFields.USER_PROFILE_FIELDS.DISTINCT_ID.getFieldName());

        Map<String,Object> userProfileInfo = Maps.newHashMap();
        userProfileInfo.put("$token",mixpanelAppConfig.getProjectToken());
        userProfileInfo.put("$distinct_id",distinctID);
        userProfileInfo.put("$set",constructPropertyMap(record));

        return userProfileInfo;
    }

    private Map<String, Object> constructPropertyMap(Tuple record) {
        Map<String,Object> propertyMap = Maps.newHashMap();
        Map<String,Object> reservedPropertyMap = record.getFields().stream().
                filter(field -> isMixpanelReservedKeyword(field.getName())).collect(Collectors.toMap(field -> "$"+field.getName() , Field::getValue));
        if(!reservedPropertyMap.isEmpty()) {
            propertyMap.putAll(reservedPropertyMap);
        }

        Map<String,Object> nonReservedPropertyMap = record.getFields().stream().
                filter(field -> !isMixpanelReservedKeyword(field.getName())).collect(Collectors.toMap(Field::getName, Field::getValue));
        if(!nonReservedPropertyMap.isEmpty()) {
            propertyMap.putAll(nonReservedPropertyMap);
        }
        return propertyMap;
    }

    private boolean isMixpanelReservedKeyword(String fieldName)
    {
        return getReservedKeywords().contains(fieldName);
    }

    private List<String> getReservedKeywords()
    {
        return Lists.newArrayList("region","timezone","country_code","last_seen","city","first_name","last_name","email");
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

    private class UpsertUserProfileConsumer implements Consumer<List<Message>> {
        @Override
        public void accept(List<Message> messages) {
            if (CollectionUtils.isEmpty(messages)) {
                return;
            }
            processBulkUserProfileUpdate(messages);
        }
    }

    private void processBulkUserProfileUpdate(List<Message> messages) {
        List<UserProfileAndError> failedRecords = this.mixpanelRestClient.upsertUserProfileDetails(
                messages.stream().map(Message::getRecord).map(this::constructUserProfileDetails).collect(Collectors.toList()));

        Map<Object, Message> userProfileRecordMapper = messages.stream().filter(message -> getDistinctID(message.getRecord()) != null)
                .collect(Collectors.toMap(message -> getDistinctID(message.getRecord()), Function.identity()));

        failedRecords.forEach(failedRecord ->
                failedRecord.getFailureReasons().forEach(failureReason -> this.errorOutputStream.writeFailedRecord(userProfileRecordMapper.get(failedRecord.getDistinctID()),
                        mixpanelErrorParser.getPipelineError(failureReason))));

        this.processedRecords.addAndGet(messages.size());
        this.lastProcessedOffset = Math.max(lastProcessedOffset, Iterables.getLast(messages).getOffset());
    }
}
