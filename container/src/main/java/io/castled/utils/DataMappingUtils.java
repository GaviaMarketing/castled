package io.castled.utils;

import io.castled.models.CastledDataMapping;
import io.castled.models.DataMappingType;
import io.castled.models.FieldMapping;
import io.castled.models.TargetFieldsMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataMappingUtils {

    public Map<String, String> getMappingForAppFields(CastledDataMapping castledDataMapping, List<String> appFields) {
        if (castledDataMapping.getType() == DataMappingType.TARGET_TEMPLATE_MAPPING) {
            return null;
        }
        TargetFieldsMapping targetFieldMapping = (TargetFieldsMapping) castledDataMapping;
        return targetFieldMapping.getFieldMappings().stream().filter(fieldMapping -> appFields.contains(fieldMapping.getAppField()))
                .collect(Collectors.toMap(FieldMapping::getAppField, FieldMapping::getWarehouseField));
    }

    public Map<String, String> appWarehouseMapping(CastledDataMapping castledDataMapping) {
        if (castledDataMapping.getType() == DataMappingType.TARGET_TEMPLATE_MAPPING) {
            return null;
        }
        TargetFieldsMapping targetFieldMapping = (TargetFieldsMapping) castledDataMapping;
        return targetFieldMapping.getFieldMappings().stream().filter(fieldMapping -> !fieldMapping.isSkipped())
                .collect(Collectors.toMap(FieldMapping::getAppField, FieldMapping::getWarehouseField));
    }

    public Map<String, String> warehouseAppMapping(CastledDataMapping castledDataMapping) {
        if (castledDataMapping.getType() == DataMappingType.TARGET_TEMPLATE_MAPPING) {
            return null;
        }
        TargetFieldsMapping targetFieldMapping = (TargetFieldsMapping) castledDataMapping;
        return targetFieldMapping.getFieldMappings().stream().filter(fieldMapping -> !fieldMapping.isSkipped())
                .collect(Collectors.toMap(FieldMapping::getWarehouseField, FieldMapping::getAppField));
    }
}
