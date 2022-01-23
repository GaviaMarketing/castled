package io.castled.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.castled.events.appevents.ExternalAppCreatedEvent;
import io.castled.events.warehousevents.WarehouseCreatedEvent;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        visible = true,
        property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NewInstallationEvent.class, name = "NEW_INSTALLATION"),
        @JsonSubTypes.Type(value = ExternalAppCreatedEvent.class, name = "APP_CREATED"),
        @JsonSubTypes.Type(value = WarehouseCreatedEvent.class, name = "WAREHOUSE_CREATED")})
@NoArgsConstructor
public abstract class CastledEvent {

    public CastledEvent(CastledEventType eventType) {
        this.eventType = eventType;
    }

    private CastledEventType eventType;

    public abstract String entityId();

}
