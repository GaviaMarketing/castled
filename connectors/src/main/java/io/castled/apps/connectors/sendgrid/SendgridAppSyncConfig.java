package io.castled.apps.connectors.sendgrid;

import io.castled.OptionsReferences;
import io.castled.apps.models.GenericSyncObject;
import io.castled.apps.syncconfigs.BaseAppSyncConfig;
import io.castled.commons.models.AppSyncMode;
import io.castled.forms.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@GroupActivator(dependencies = {"object"}, group = MappingFormGroups.SYNC_MODE)
public class SendgridAppSyncConfig extends BaseAppSyncConfig {

    @FormField(title = "select sendgrid object to sync", schema = FormFieldSchema.OBJECT, type = FormFieldType.DROP_DOWN, group = MappingFormGroups.OBJECT,
            optionsRef = @OptionsRef(value = OptionsReferences.OBJECT, type = OptionsRefType.DYNAMIC))
    private GenericSyncObject object;

    @FormField(title = "Lists", type = FormFieldType.DROP_DOWN, description = "An array of Lists that this contact will be added to", group = "listId",
            optionsRef = @OptionsRef(value = OptionsReferences.SENDGRID_LISTS, type = OptionsRefType.DYNAMIC), required = false)
    private String listIds;

    @FormField(type = FormFieldType.RADIO_GROUP, schema = FormFieldSchema.ENUM, title = "Sync Mode", description = "Sync mode which controls whether records will be appended, updated or upserted", group = MappingFormGroups.SYNC_MODE,
            optionsRef = @OptionsRef(value = OptionsReferences.SYNC_MODE, type = OptionsRefType.DYNAMIC))
    private AppSyncMode mode;
}
