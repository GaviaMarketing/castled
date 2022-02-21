package io.castled.utils;

import io.castled.models.CastledDataMapping;
import io.castled.models.DataMappingType;
import io.castled.models.FieldMapping;
import io.castled.models.TargetFieldsMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class DataMappingUtils {

    public static Map<String, String> getMappingForAppFields(CastledDataMapping castledDataMapping, List<String> appFields) {
        if (castledDataMapping.getType() == DataMappingType.TARGET_REST_MAPPING) {
            return null;
        }
        TargetFieldsMapping targetFieldMapping = (TargetFieldsMapping) castledDataMapping;
        return targetFieldMapping.getFieldMappings().stream().filter(fieldMapping -> appFields.contains(fieldMapping.getAppField()))
                .collect(Collectors.toMap(FieldMapping::getAppField, FieldMapping::getWarehouseField));
    }

    public static Map<String, List<String>> appWarehouseMapping(CastledDataMapping castledDataMapping) {
        if (castledDataMapping.getType() == DataMappingType.TARGET_REST_MAPPING) {
            return null;
        }
        TargetFieldsMapping targetFieldMapping = (TargetFieldsMapping) castledDataMapping;
        return targetFieldMapping.getFieldMappings().stream().filter(fieldMapping -> !fieldMapping.isSkipped())
                .collect(groupingBy(FieldMapping::getAppField, mapping(FieldMapping::getWarehouseField, toList())));
    }

    public static Map<String, List<String>> warehouseAppMapping(CastledDataMapping castledDataMapping) {
        if (castledDataMapping.getType() == DataMappingType.TARGET_REST_MAPPING) {
            return null;
        }
        TargetFieldsMapping targetFieldMapping = (TargetFieldsMapping) castledDataMapping;
        return targetFieldMapping.getFieldMappings().stream().filter(fieldMapping -> !fieldMapping.isSkipped())
                .collect(groupingBy(FieldMapping::getWarehouseField, mapping(FieldMapping::getAppField, toList())));
    }

    public static List<String> getMappedAppFields(CastledDataMapping castledDataMapping) {
        if (castledDataMapping.getType() == DataMappingType.TARGET_REST_MAPPING) {
            return null;
        }
        TargetFieldsMapping targetFieldMapping = (TargetFieldsMapping) castledDataMapping;
        return targetFieldMapping.getFieldMappings().stream().filter(mapping -> !mapping.isSkipped())
                .map(FieldMapping::getAppField).collect(Collectors.toList());
    }

    public static void addAdditionalMappings(TargetFieldsMapping targetFieldsMapping, List<FieldMapping> additionalMappings) {
        targetFieldsMapping.getFieldMappings().addAll(additionalMappings);
    }
}
