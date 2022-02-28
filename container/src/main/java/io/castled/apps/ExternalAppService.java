package io.castled.apps;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.castled.ObjectRegistry;
import io.castled.apps.daos.ExternalAppDAO;
import io.castled.apps.dtos.AppSyncConfigDTO;
import io.castled.apps.models.ExternalAppSchema;
import io.castled.apps.optionfetchers.AppOptionsFetcher;
import io.castled.apps.syncconfigs.AppSyncConfig;
import io.castled.caches.ExternalAppCache;
import io.castled.caches.UsersCache;
import io.castled.daos.PipelineDAO;
import io.castled.dtos.ExternalAppOauthState;
import io.castled.dtos.ExternalAppTypeDTO;
import io.castled.dtos.OAuthAppAttributes;
import io.castled.encryption.EncryptionManager;
import io.castled.events.CastledEventsClient;
import io.castled.events.appevents.ExternalAppCreatedEvent;
import io.castled.exceptions.CastledRuntimeException;
import io.castled.exceptions.connect.ConnectException;
import io.castled.forms.dtos.FieldOptionsDTO;
import io.castled.forms.dtos.FormFieldsDTO;
import io.castled.models.users.User;
import io.castled.oauth.OAuthAccessProvider;
import io.castled.oauth.OAuthAccessProviderFactory;
import io.castled.oauth.OAuthClientConfig;
import io.castled.oauth.OAuthServiceType;
import io.castled.optionsfetchers.appsync.AppSyncOptionsFetcher;
import io.castled.pubsub.MessagePublisher;
import io.castled.pubsub.registry.ExternalAppUpdatedMessage;
import io.castled.resources.validators.ResourceAccessController;
import io.castled.schema.mapping.MappingGroup;
import io.castled.utils.DocUtils;
import io.castled.utils.JsonUtils;
import io.castled.utils.OAuthStateStore;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@SuppressWarnings({"rawtypes", "unchecked"})
public class ExternalAppService {

    private final ExternalAppDAO externalAppDAO;
    private final PipelineDAO pipelineDAO;
    private final EncryptionManager encryptionManager;
    private final ResourceAccessController accessController;
    private final Map<ExternalAppType, ExternalAppConnector> appConnectors;
    private final ExternalAppCache externalAppCache;
    private final MessagePublisher messagePublisher;
    private final CastledEventsClient castledEventsClient;
    private final Map<String, AppSyncOptionsFetcher> appSyncOptionsFetchers;
    private final OAuthAccessProviderFactory oAuthAccessProviderFactory;
    private final Map<String, AppOptionsFetcher> appOptionsFetchers;


    @Inject
    public ExternalAppService(Jdbi jdbi, Map<ExternalAppType, ExternalAppConnector> appConnectors,
                              EncryptionManager encryptionManager, ExternalAppCache externalAppCache,
                              ResourceAccessController accessController, MessagePublisher messagePublisher,
                              Map<String, AppSyncOptionsFetcher> appSyncOptionsFetchers, CastledEventsClient castledEventsClient,
                              OAuthAccessProviderFactory oAuthAccessProviderFactory, Map<String, AppOptionsFetcher> appOptionsFetchers) {
        this.externalAppDAO = jdbi.onDemand(ExternalAppDAO.class);
        this.pipelineDAO = jdbi.onDemand(PipelineDAO.class);
        this.appConnectors = appConnectors;
        this.encryptionManager = encryptionManager;
        this.externalAppCache = externalAppCache;
        this.accessController = accessController;
        this.messagePublisher = messagePublisher;
        this.appSyncOptionsFetchers = appSyncOptionsFetchers;
        this.oAuthAccessProviderFactory = oAuthAccessProviderFactory;
        this.appOptionsFetchers = appOptionsFetchers;
        this.castledEventsClient = castledEventsClient;

    }

    public Long createExternalApp(String name, AppConfig appConfig, User user) {
        try {
            this.appConnectors.get(appConfig.getType()).validateAppConfig(appConfig);
            AppConfig enrichedAppConfig = this.appConnectors.get(appConfig.getType()).enrichAppConfig(appConfig);
            String configText = this.encryptionManager.encryptText(JsonUtils.objectToString(enrichedAppConfig), user.getTeamId());
            Long appId = this.externalAppDAO.createExternalApp(name, appConfig.getType(), configText, user.getTeamId());
            this.castledEventsClient.publishCastledEvent(new ExternalAppCreatedEvent(appId));
            return appId;
        } catch (ConnectException e) {
            throw new BadRequestException(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create external app of type {} and name {}", appConfig.getType(), name);
            throw new CastledRuntimeException(e);
        }
    }

    public URI createOAuthExternalApp(User user, OAuthAppAttributes externalAppAttributes) {

        OAuthAppConfig oAuthAppConfig = (OAuthAppConfig) externalAppAttributes.getConfig();
        try {
            //Optional.ofNullable(StringUtils.nullIfEmpty(externalAppAttributes.getName())).orElseThrow(()->new InvalidConfigException("Name is mandatory"));
            OAuthServiceType oAuthServiceType = externalAppAttributes.getConfig().getType().oauthServiceType();
            if (oAuthServiceType == null) {
                throw new BadRequestException(String.format("Oauth not supported for app type %s", externalAppAttributes.getConfig().getType()));
            }
            externalAppAttributes.setConfig(oAuthAppConfig);
            return getAuthorizationUrl(user, oAuthServiceType, externalAppAttributes, null);
        } catch (Exception e) {
            log.error("Failed to create external oauth app of type {} and name {}", oAuthAppConfig.getType(), externalAppAttributes.getName());
            throw new CastledRuntimeException(e);
        }
    }


    public URI updateOAuthExternalApp(User user, Long appId, OAuthAppAttributes externalAppAttributes) {

        OAuthAppConfig oAuthAppConfig = (OAuthAppConfig) externalAppAttributes.getConfig();
        try {
            OAuthServiceType oAuthServiceType = externalAppAttributes.getConfig().getType().oauthServiceType();
            if (oAuthServiceType == null) {
                throw new BadRequestException(String.format("Oauth not supported for app type %s", externalAppAttributes.getConfig().getType()));
            }
            externalAppAttributes.setConfig(oAuthAppConfig);
            return getAuthorizationUrl(user, oAuthServiceType, externalAppAttributes, appId);
        } catch (Exception e) {
            log.error("Failed to create external oauth app of type {} and name {}", oAuthAppConfig.getType(), externalAppAttributes.getName());
            throw new CastledRuntimeException(e);
        }
    }

    public void deleteExternalApp(Long appId, Long teamId) {
        ExternalApp externalApp = getExternalApp(appId, false);
        accessController.validAppAccess(externalApp, teamId);
        if (pipelineDAO.getPipelinesByAppId(appId).size() > 0) {
            throw new BadRequestException("Please delete all pipelines corresponding to this app before deleting it");

        }
        this.externalAppDAO.deleteApp(appId);
    }

    public void updateExternalApp(Long appId, Long teamId, String name, AppConfig appConfig) {
        try {
            ExternalApp externalApp = getExternalApp(appId, false);
            accessController.validAppAccess(externalApp, teamId);
            Map<String, Object> mergedConfig = mergeConfigs(JsonUtils.objectToMap(externalApp.getConfig()), JsonUtils.objectToMap(appConfig));
            String config = this.encryptionManager.encryptText(JsonUtils.objectToString(mergedConfig), externalApp.getTeamId());
            this.externalAppDAO.updateExternalApp(appId, name, config);
            this.messagePublisher.publishMessage(new ExternalAppUpdatedMessage(appId));
        } catch (Exception e) {
            log.error("Failed to update config for app {}", appId);
            throw new CastledRuntimeException(e);
        }
    }

    private Map<String, Object> mergeConfigs(Map<String, Object> existingConfig, Map<String, Object> newConfig) {
        Map<String, Object> mergedConfig = Maps.newHashMap(existingConfig);
        mergedConfig.putAll(newConfig);
        return mergedConfig;
    }

    public ExternalApp getExternalApp(Long appId, boolean cached) {
        if (cached) {
            return externalAppCache.getValue(appId);
        }
        return this.externalAppDAO.getExternalApp(appId);
    }

    public ExternalApp getExternalApp(Long appId) {
        return getExternalApp(appId, false);
    }

    public FormFieldsDTO getMappingFormFields(ExternalAppType appType) {
        return this.appConnectors.get(appType).getMappingFields();
    }

    public FormFieldsDTO getFormFields(ExternalAppType appType) {
        return this.appConnectors.get(appType).getFormFields();
    }

    public ExternalAppSchema getObjectSchema(Long appId, AppSyncConfig appSyncConfig) {
        ExternalApp externalApp = getExternalApp(appId, true);
        return this.appConnectors.get(externalApp.getType()).getSchema(externalApp.getConfig(), appSyncConfig);
    }

    public List<MappingGroup> getMappingGroup(Long appId, AppSyncConfig appSyncConfig) {
        ExternalApp externalApp = getExternalApp(appId, true);
        return this.appConnectors.get(externalApp.getType()).getMappingGroups(externalApp.getConfig(), appSyncConfig);
    }

    public List<ExternalApp> listExternalApps(Long teamId, ExternalAppType externalAppType) {
        return this.externalAppDAO.listExternalApps(teamId).stream()
                .filter(externalApp -> (externalAppType == null || externalApp.getType() == externalAppType)).collect(Collectors.toList());

    }

    public URI getAuthorizationUrl(User user, OAuthServiceType serviceType, OAuthAppAttributes oAuthAppAttributes,
                                   Long appId) throws Exception {

        OAuthAppConfig oAuthAppConfig = (OAuthAppConfig)oAuthAppAttributes.getConfig();
        ExternalAppOauthState externalAppOauthState = new ExternalAppOauthState(user.getId(), appId, oAuthAppAttributes);
        String stateId = ObjectRegistry.getInstance(OAuthStateStore.class).persistOAuthState(JsonUtils.objectToString(externalAppOauthState));
        OAuthClientConfig oAuthClientConfig = new OAuthClientConfig(oAuthAppConfig.getClientId(),
                oAuthAppConfig.getClientSecret());
        OAuthAccessProvider oAuthAccessProvider = oAuthAccessProviderFactory.getAccessProvider(serviceType, oAuthClientConfig);
        return UriBuilder.fromUri(oAuthAccessProvider.getAuthorizationUrl(stateId, getRedirectUri(serviceType,
                oAuthAppAttributes.getServerUrl()), oAuthClientConfig.getClientId())).build();
    }

    public URI handleAuthorizationCallback(OAuthServiceType serviceType, String stateId, String authorizationCode) {

        String state = ObjectRegistry.getInstance(OAuthStateStore.class).getOAuthState(stateId);
        ExternalAppOauthState externalAppOauthState = JsonUtils.jsonStringToObject(state, ExternalAppOauthState.class);
        if (externalAppOauthState == null) {
            throw new BadRequestException(String.format("Invalid state id %s", stateId));
        }
        OAuthAppAttributes oAuthAppAttributes = externalAppOauthState.getOAuthAppAttributes();
        OAuthAppConfig oAuthAppConfig = (OAuthAppConfig)oAuthAppAttributes.getConfig();
        OAuthClientConfig oAuthClientConfig = new OAuthClientConfig(oAuthAppConfig.getClientId(),
                oAuthAppConfig.getClientSecret());
        try {
            OAuthAccessProvider oAuthAccessProvider = this.oAuthAccessProviderFactory.getAccessProvider(serviceType, oAuthClientConfig);
            Long oauthToken = oAuthAccessProvider.persistAccessConfig(authorizationCode,
                    getRedirectUri(serviceType, oAuthAppAttributes.getServerUrl()));
            oAuthAppConfig.setOAuthToken(oauthToken);
            User user = ObjectRegistry.getInstance(UsersCache.class).getValue(externalAppOauthState.getUserId());
            if (externalAppOauthState.getAppId() == null) {
                Long appId = createExternalApp(oAuthAppAttributes.getName(), oAuthAppConfig, user);
                return UriBuilder.fromUri(oAuthAppAttributes.getSuccessUrl()).queryParam("id", appId).build();
            }
            updateExternalApp(externalAppOauthState.getAppId(), user.getTeamId(), oAuthAppAttributes.getName(),
                    oAuthAppConfig);
            return UriBuilder.fromUri(oAuthAppAttributes.getSuccessUrl()).queryParam("id", externalAppOauthState.getAppId()).build();

        } catch (Exception e) {
            log.error("Authorization callback failed for {}", serviceType, e);
            return UriBuilder.fromUri(oAuthAppAttributes.getFailureUrl()).build();
        }
    }

    private String getRedirectUri(OAuthServiceType oAuthProviderType, String serverUrl) {
        return String.format("%s/v1/oauth/apps/%s/callback", serverUrl, oAuthProviderType);
    }


    public List<ExternalAppTypeDTO> listExternalAppTypes(User user) {
        List<ExternalApp> externalApps = this.externalAppDAO.listExternalApps(user.getTeamId());
        return Arrays.stream(ExternalAppType.values()).map(appType -> new ExternalAppTypeDTO(appType,
                appType.title(), appType.getAccessType(), appType.logoUrl(), DocUtils.constructDocUrl(appType.docUrl()),
                externalApps.stream().filter(externalApp -> externalApp.getType().equals(appType)).count(),
                appType.mappingType())).collect(Collectors.toList());
    }


    public FieldOptionsDTO getAppSyncOptions(AppSyncConfigDTO appSyncConfig, String optionsReference) {
        ExternalApp externalApp = getExternalApp(appSyncConfig.getAppId(), true);
        return new FieldOptionsDTO(this.appSyncOptionsFetchers.get(optionsReference).getOptions(appSyncConfig, externalApp));
    }

    public FieldOptionsDTO getConfigOptions(AppConfig appConfig,
                                            String optionsReference) {
        return Optional.ofNullable(this.appOptionsFetchers.get(optionsReference))
                .map(optionsFetcher -> new FieldOptionsDTO(optionsFetcher.getFieldOptions(appConfig)))
                .orElse(null);
    }
}
