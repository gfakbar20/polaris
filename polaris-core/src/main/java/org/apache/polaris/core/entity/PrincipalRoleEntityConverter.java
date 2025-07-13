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

package org.apache.polaris.core.entity;

import org.apache.polaris.core.admin.model.PrincipalRole;
import org.apache.polaris.core.entity.table.federated.FederatedEntities;

public final class PrincipalRoleEntityConverter {
    public static PrincipalRoleEntity fromPrincipalRole(PrincipalRole principalRole) {
      return new PrincipalRoleEntity.Builder()
          .setName(principalRole.getName())
          .setFederated(principalRole.getFederated())
          .setProperties(principalRole.getProperties())
          .build();
    }

    public static PrincipalRole asPrincipalRole(PrincipalRoleEntity entity) {
      return new PrincipalRole(
          entity.getName(),
          FederatedEntities.isFederated(entity),
          entity.getPropertiesAsMap(),
          entity.getCreateTimestamp(),
          entity.getLastUpdateTimestamp(),
          entity.getEntityVersion());
    }
}
