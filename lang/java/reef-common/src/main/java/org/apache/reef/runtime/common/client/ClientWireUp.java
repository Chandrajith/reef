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
package org.apache.reef.runtime.common.client;


import org.apache.reef.annotations.audience.ClientSide;
import org.apache.reef.annotations.audience.Private;
import org.apache.reef.proto.ReefServiceProtos;
import org.apache.reef.runtime.common.client.parameters.ClientPresent;
import org.apache.reef.runtime.common.utils.RemoteManager;
import org.apache.reef.tang.annotations.Parameter;
import org.apache.reef.util.Optional;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used on the Client side to setup event handlers and such.
 */
@ClientSide
@Private
final class ClientWireUp {
  private static final Logger LOG = Logger.getLogger(ClientWireUp.class.getName());
  private final RuntimeErrorProtoHandler runtimeErrorProtoHandler;
  private final JobStatusMessageHandler jobStatusMessageHandler;
  private final Optional<RemoteManager> remoteManager;
  private final boolean isClientPresent;
  private boolean isWired = false;

  @Inject
  ClientWireUp(final RemoteManager remoteManager,
               @Parameter(ClientPresent.class) final String clientPresent,
               final RuntimeErrorProtoHandler runtimeErrorProtoHandler,
               final JobStatusMessageHandler jobStatusMessageHandler) {
    this.remoteManager = Optional.ofNullable(remoteManager);
    this.runtimeErrorProtoHandler = runtimeErrorProtoHandler;
    this.jobStatusMessageHandler = jobStatusMessageHandler;
    this.isClientPresent = clientPresent.equals(ClientPresent.YES);
    LOG.log(Level.FINE, "Instantiated 'ClientWireUp'. Client present: " + this.isClientPresent());
  }

  @Inject
  ClientWireUp(@Parameter(ClientPresent.class) final String clientPresent,
               final RuntimeErrorProtoHandler runtimeErrorProtoHandler,
               final JobStatusMessageHandler jobStatusMessageHandler) {
    this(null, clientPresent, runtimeErrorProtoHandler, jobStatusMessageHandler);
  }

  synchronized void performWireUp() {
    if (this.isWired) {
      throw new IllegalStateException("performWireUp is only to be called once.");
    }
    if (this.remoteManager.isPresent()) {
      LOG.log(Level.FINEST, "Wiring up communications channels to the Driver.");
      final RemoteManager rm = this.remoteManager.get();
      rm.registerHandler(ReefServiceProtos.RuntimeErrorProto.class, this.runtimeErrorProtoHandler);
      rm.registerHandler(ReefServiceProtos.JobStatusProto.class, this.jobStatusMessageHandler);
      LOG.log(Level.FINE, "Wired up communications channels to the Driver.");
    }
    this.isWired = true;
  }

  synchronized boolean isClientPresent() {
    return this.isClientPresent;
  }

  synchronized String getRemoteManagerIdentifier() {
    if (!this.isClientPresent() || !this.remoteManager.isPresent()) {
      throw new RuntimeException("No need to setup the remote manager.");
    } else {
      return this.remoteManager.get().getMyIdentifier();
    }
  }

  /**
   * Closes the remote manager, if there was one.
   */
  synchronized void close() {
    if (this.remoteManager.isPresent()) {
      try {
        this.remoteManager.get().close();
      } catch (final Exception e) {
        LOG.log(Level.WARNING, "Exception while shutting down the RemoteManager.", e);
      }
    }
  }
}
