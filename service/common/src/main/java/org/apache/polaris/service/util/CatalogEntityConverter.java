/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.polaris.service.util;

import static org.apache.polaris.core.admin.model.StorageConfigInfo.StorageTypeEnum.AZURE;
import static org.apache.polaris.core.entity.CatalogEntity.CATALOG_TYPE_PROPERTY;
import static org.apache.polaris.core.entity.CatalogEntity.DEFAULT_BASE_LOCATION_KEY;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.polaris.core.admin.model.AwsStorageConfigInfo;
import org.apache.polaris.core.admin.model.AzureStorageConfigInfo;
import org.apache.polaris.core.admin.model.Catalog;
import org.apache.polaris.core.admin.model.CatalogProperties;
import org.apache.polaris.core.admin.model.ConnectionConfigInfo;
import org.apache.polaris.core.admin.model.ExternalCatalog;
import org.apache.polaris.core.admin.model.FileStorageConfigInfo;
import org.apache.polaris.core.admin.model.GcpStorageConfigInfo;
import org.apache.polaris.core.admin.model.PolarisCatalog;
import org.apache.polaris.core.admin.model.StorageConfigInfo;
import org.apache.polaris.core.connection.ConnectionConfigInfoDpo;
import org.apache.polaris.core.context.CallContext;
import org.apache.polaris.core.entity.CatalogEntity;
import org.apache.polaris.core.entity.PolarisEntityConstants;
import org.apache.polaris.core.storage.FileStorageConfigurationInfo;
import org.apache.polaris.core.storage.PolarisStorageConfigurationInfo;
import org.apache.polaris.core.storage.aws.AwsStorageConfigurationInfo;
import org.apache.polaris.core.storage.azure.AzureStorageConfigurationInfo;
import org.apache.polaris.core.storage.gcp.GcpStorageConfigurationInfo;

public final class CatalogEntityConverter {

  private CatalogEntityConverter() {}

  public static Catalog toApiPayloadSchema(CatalogEntity entity) {
    Map<String, String> internalProperties = entity.getInternalPropertiesAsMap();
    Catalog.TypeEnum catalogType =
        Optional.ofNullable(internalProperties.get(CATALOG_TYPE_PROPERTY))
            .map(Catalog.TypeEnum::valueOf)
            .orElseGet(
                () -> entity.getName().equalsIgnoreCase("ROOT") ? Catalog.TypeEnum.INTERNAL : null);
    Map<String, String> propertiesMap = entity.getPropertiesAsMap();
    CatalogProperties catalogProps =
        CatalogProperties.builder(propertiesMap.get(DEFAULT_BASE_LOCATION_KEY))
            .putAll(propertiesMap)
            .build();
    return catalogType == Catalog.TypeEnum.EXTERNAL
        ? ExternalCatalog.builder()
            .setType(Catalog.TypeEnum.EXTERNAL)
            .setName(entity.getName())
            .setProperties(catalogProps)
            .setCreateTimestamp(entity.getCreateTimestamp())
            .setLastUpdateTimestamp(entity.getLastUpdateTimestamp())
            .setEntityVersion(entity.getEntityVersion())
            .setStorageConfigInfo(getEntityStorageInfo(internalProperties, entity))
            .setConnectionConfigInfo(getEntityConnectionInfo(internalProperties, entity))
            .build()
        : PolarisCatalog.builder()
            .setType(Catalog.TypeEnum.INTERNAL)
            .setName(entity.getName())
            .setProperties(catalogProps)
            .setCreateTimestamp(entity.getCreateTimestamp())
            .setLastUpdateTimestamp(entity.getLastUpdateTimestamp())
            .setEntityVersion(entity.getEntityVersion())
            .setStorageConfigInfo(getEntityStorageInfo(internalProperties, entity))
            .build();
  }

  public static CatalogEntity fromApiPayloadSchema(CallContext callContext, Catalog catalog) {
    CatalogEntity.Builder builder =
        new CatalogEntity.Builder()
            .setName(catalog.getName())
            .setProperties(catalog.getProperties().toMap())
            .setCatalogType(catalog.getType().name());
    Map<String, String> internalProperties = new HashMap<>();
    internalProperties.put(CATALOG_TYPE_PROPERTY, catalog.getType().name());
    builder.setInternalProperties(internalProperties);
    builder.setStorageConfigurationInfo(
        callContext,
        catalog.getStorageConfigInfo(),
        catalog.getProperties().getDefaultBaseLocation());
    return builder.build();
  }

  private static StorageConfigInfo getEntityStorageInfo(
      Map<String, String> internalProperties, CatalogEntity entity) {
    if (internalProperties.containsKey(PolarisEntityConstants.getStorageConfigInfoPropertyName())) {
      PolarisStorageConfigurationInfo configInfo = entity.getStorageConfigurationInfo();
      if (configInfo instanceof AwsStorageConfigurationInfo) {
        AwsStorageConfigurationInfo awsConfig = (AwsStorageConfigurationInfo) configInfo;
        return AwsStorageConfigInfo.builder()
            .setRoleArn(awsConfig.getRoleARN())
            .setExternalId(awsConfig.getExternalId())
            .setUserArn(awsConfig.getUserARN())
            .setStorageType(StorageConfigInfo.StorageTypeEnum.S3)
            .setAllowedLocations(awsConfig.getAllowedLocations())
            .setRegion(awsConfig.getRegion())
            .build();
      }
      if (configInfo instanceof AzureStorageConfigurationInfo) {
        AzureStorageConfigurationInfo azureConfig = (AzureStorageConfigurationInfo) configInfo;
        return AzureStorageConfigInfo.builder()
            .setTenantId(azureConfig.getTenantId())
            .setMultiTenantAppName(azureConfig.getMultiTenantAppName())
            .setConsentUrl(azureConfig.getConsentUrl())
            .setStorageType(AZURE)
            .setAllowedLocations(azureConfig.getAllowedLocations())
            .build();
      }
      if (configInfo instanceof GcpStorageConfigurationInfo) {
        GcpStorageConfigurationInfo gcpConfigModel = (GcpStorageConfigurationInfo) configInfo;
        return GcpStorageConfigInfo.builder()
            .setGcsServiceAccount(gcpConfigModel.getGcpServiceAccount())
            .setStorageType(StorageConfigInfo.StorageTypeEnum.GCS)
            .setAllowedLocations(gcpConfigModel.getAllowedLocations())
            .build();
      }
      if (configInfo instanceof FileStorageConfigurationInfo) {
        FileStorageConfigurationInfo fileConfigModel = (FileStorageConfigurationInfo) configInfo;
        return new FileStorageConfigInfo(
            StorageConfigInfo.StorageTypeEnum.FILE, fileConfigModel.getAllowedLocations());
      }
      return null;
    }
    return null;
  }

  public static Catalog.TypeEnum convertCatalogTypeToEnum(String type) {
    return Optional.ofNullable(type).map(Catalog.TypeEnum::valueOf).orElse(null);
  }

  private static ConnectionConfigInfo getEntityConnectionInfo(
      Map<String, String> internalProperties, CatalogEntity entity) {
    if (internalProperties.containsKey(
        PolarisEntityConstants.getConnectionConfigInfoPropertyName())) {
      ConnectionConfigInfoDpo configInfo = entity.getConnectionConfigInfoDpo();
      return configInfo.asConnectionConfigInfoModel();
    }
    return null;
  }
}
