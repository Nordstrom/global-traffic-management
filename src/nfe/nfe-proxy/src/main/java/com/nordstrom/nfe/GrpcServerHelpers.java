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
package com.nordstrom.nfe;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.ExecutionException;

public class GrpcServerHelpers {
  public static StatusException statusExceptionFrom(Exception e) {
    // Check if it is already a `StatusException`.
    if (e instanceof StatusException) {
      return (StatusException) e;
    }

    // Check if it is an `ExecutionException` that contains a `StatusException`.
    if ((e instanceof ExecutionException)) {
      if ((e.getCause() instanceof StatusRuntimeException)) {
        StatusRuntimeException cause = (StatusRuntimeException) e.getCause();
        return new StatusException(cause.getStatus());
      }
    }

    // Else return an `.INTERNAL` error.
    return new StatusException(Status.INTERNAL);
  }
}
