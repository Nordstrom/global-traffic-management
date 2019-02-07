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
package com.nordstrom.apikey;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import javax.annotation.Nullable;

public class Result<T> {
  private T value;
  private Throwable error;

  @Nullable
  public T getValue() {
    return value;
  }

  @Nullable
  public Throwable getError() {
    return error;
  }

  public static <T> Result<T> resultFromFuture(Supplier<ListenableFuture<T>> supplier)
      throws Exception {
    final Result<T> result = new Result<>();
    final CountDownLatch latch = new CountDownLatch(1);
    Futures.addCallback(
        supplier.get(),
        new FutureCallback<T>() {
          @Override
          public void onSuccess(@Nullable T success) {
            result.value = success;
            latch.countDown();
          }

          @Override
          public void onFailure(Throwable t) {
            result.error = t;
            latch.countDown();
          }
        },
        MoreExecutors.directExecutor());
    latch.await();
    return result;
  }
}
