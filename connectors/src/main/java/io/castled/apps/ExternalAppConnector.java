package io.castled.apps;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import io.castled.apps.dtos.AppSyncConfigDTO;
import io.castled.apps.models.ExternalAppSchema;
import io.castled.apps.syncconfigs.AppSyncConfig;
import io.castled.commons.models.AppSyncMode;
import io.castled.dtos.PipelineConfigDTO;
import io.castled.exceptions.connect.InvalidConfigException;
import io.castled.forms.FormUtils;
import io.castled.forms.dtos.FormFieldOption;
import io.castled.forms.dtos.FormFieldsDTO;
import io.castled.schema.mapping.MappingGroup;
import io.castled.schema.models.RecordSchema;
import io.castled.utils.JsonUtils;

import javax.ws.rs.BadRequestException;
import java.util.List;

public interface ExternalAppConnector<CONFIG extends AppConfig, DATASINK extends DataSink, MAPPINGCONFIG extends AppSyncConfig> {

    List<FormFieldOption> getAllObjects(CONFIG config, MAPPINGCONFIG mappingConfig);

    default FormFieldsDTO getMappingFields() {
        Class<? extends AppSyncConfig> mappingConfigClass = getMappingConfigType();
        return FormUtils.getFormFields(mappingConfigClass);
    }

    default FormFieldsDTO getFormFields() {
        Class<? extends AppConfig> appConfigClass = getAppConfigType();
        return FormUtils.getFormFields(appConfigClass);
    }

    DATASINK getDataSink();

    ExternalAppSchema getSchema(CONFIG config, MAPPINGCONFIG mappingconfig);

    default List<AppSyncMode> getSyncModes(CONFIG config, MAPPINGCONFIG appSyncConfig) {
        return Lists.newArrayList(AppSyncMode.INSERT, AppSyncMode.UPDATE, AppSyncMode.UPSERT);
    }

    default CONFIG enrichAppConfig(CONFIG appConfig) {
        return appConfig;
    }

    default void validateAppConfig(CONFIG appConfig) throws InvalidConfigException {

    }

    default PipelineConfigDTO validateAndEnrichPipelineConfig(PipelineConfigDTO pipelineConfig) throws BadRequestException {
        return pipelineConfig;
    }

    default RecordSchema enrichWarehouseASchema(AppSyncConfigDTO appSyncConfigDTO, RecordSchema warehouseSchema) {
        return warehouseSchema;
    }

    default CONFIG getAppConfig(JsonNode jsonNode) {
        return JsonUtils.jsonNodeToObject(jsonNode, getAppConfigType());
    }

    default MAPPINGCONFIG getAppSyncConfig(JsonNode jsonNode) {
        return JsonUtils.jsonNodeToObject(jsonNode, getMappingConfigType());
    }

    List<MappingGroup> getMappingGroups(CONFIG config, MAPPINGCONFIG mappingconfig);

    Class<MAPPINGCONFIG> getMappingConfigType();

    Class<CONFIG> getAppConfigType();
}
