package io.castled.pipelines;

import io.castled.ObjectRegistry;
import io.castled.commons.errors.errorclassifications.IncompatibleMappingError;
import io.castled.commons.streams.ErrorOutputStream;
import io.castled.commons.streams.MessageInputStream;
import io.castled.schema.IncompatibleValueException;
import io.castled.schema.SchemaMapper;
import io.castled.schema.models.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;

@Slf4j
public class SchemaMappedMessageInputStream implements MessageInputStream {

    private final RecordSchema targetSchema;
    private final MessageInputStream messageInputStream;
    private final SchemaMapper schemaMapper;
    private final Map<String, List<String>> targetSourceMapping;
    private final Map<String, List<String>> sourceTargetMapping;
    private final ErrorOutputStream errorOutputStream;
    @Getter
    private long failedRecords = 0;

    public SchemaMappedMessageInputStream(RecordSchema targetSchema, MessageInputStream messageInputStream,
                                          Map<String, List<String>> targetSourceMapping, Map<String, List<String>> sourceTargetMapping, ErrorOutputStream errorOutputStream) {
        this.targetSchema = targetSchema;
        this.messageInputStream = messageInputStream;
        this.schemaMapper = ObjectRegistry.getInstance(SchemaMapper.class);
        this.targetSourceMapping = targetSourceMapping;
        this.errorOutputStream = errorOutputStream;
        this.sourceTargetMapping = sourceTargetMapping;
    }

    @Override
    public Message readMessage() throws Exception {

        while (true) {
            Message message = this.messageInputStream.readMessage();
            if (message == null) {
                return null;
            }
            if (this.sourceTargetMapping == null) {
                return message;
            }
            Message mappedMessage = mapMessage(message);
            if (mappedMessage == null) {
                continue;
            }
            return mappedMessage;
        }
    }

    private Message mapMessage(Message message) {

        if (targetSchema == null) {
            return mapMessageFromSourceSchema(message);
        }
        Tuple.Builder recordBuilder = Tuple.builder();
        for (FieldSchema field : targetSchema.getFieldSchemas()) {
            List<String> sourceFields = targetSourceMapping.get(field.getName());
            for (String sourceField : sourceFields) {
                if (sourceField != null) {
                    try {
                        recordBuilder.put(field, schemaMapper.transformValue(message.getRecord().getValue(sourceField), field.getSchema()));
                    } catch (IncompatibleValueException e) {
                        failedRecords++;
                        this.errorOutputStream.writeFailedRecord(message, new IncompatibleMappingError(sourceField, field.getSchema()));
                        return null;
                    }
                }
            }
        }
        return new Message(message.getOffset(), recordBuilder.build());
    }

    private Message mapMessageFromSourceSchema(Message message) {
        Tuple.Builder recordBuilder = Tuple.builder();
        for (Field field : message.getRecord().getFields()) {
            List<String> targetFields = sourceTargetMapping.get(field.getName());
            if(!CollectionUtils.isEmpty(targetFields)){
                targetFields.stream().forEach(targetField -> recordBuilder.put(new FieldSchema(targetField, field.getSchema(), field.getParams()), field.getValue()));
            }
        }
        return new Message(message.getOffset(), recordBuilder.build());
    }

    public void close() throws Exception {
        this.messageInputStream.close();
    }
}
