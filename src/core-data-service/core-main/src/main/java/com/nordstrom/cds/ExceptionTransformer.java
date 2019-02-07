/**
 * Copyright (C) 2018 Nordstrom, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nordstrom.cds;

import io.grpc.Status;
import io.grpc.StatusException;

class ExceptionTransformer {
  StatusException convertToStatusException(Exception exception) {
    if (exception instanceof CoreDataServiceException) {
      return statusExceptionFrom((CoreDataServiceException) exception);
    }

    return new StatusException(Status.INTERNAL);
  }

  private StatusException statusExceptionFrom(CoreDataServiceException coreDataServiceException) {
    Status status;
    switch (coreDataServiceException.getType()) {
      case SERVICE_ALREADY_EXISTS:
        status = Status.ALREADY_EXISTS;
        break;
      case API_KEY_ALREADY_EXISTS:
        status = Status.ALREADY_EXISTS;
        break;
      case INVALID_ARGUMENT:
        status = Status.INVALID_ARGUMENT;
        break;
      default:
        status = Status.INTERNAL;
    }

    return new StatusException(status.withDescription(coreDataServiceException.getDescription()));
  }
}
