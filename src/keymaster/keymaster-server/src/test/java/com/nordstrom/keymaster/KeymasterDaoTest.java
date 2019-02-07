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
package com.nordstrom.keymaster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nordstrom.keymaster.util.KeymasterTestDao;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class KeymasterDaoTest {
  @RegisterExtension static KeymasterTestDao testDb = new KeymasterTestDao();

  @Test
  void testHandleNewKeypair() throws InterruptedException {
    KeymasterDao subject = testDb.getDao();

    BigInteger firstSerialFreddyForGtm =
        subject.handleNewKeypair("freddy krueger", CertificateRequest.DataClassification.GTM);
    assertEquals(BigInteger.valueOf(1), firstSerialFreddyForGtm);

    BigInteger secondSerialFreddyForGtm =
        subject.handleNewKeypair("freddy krueger", CertificateRequest.DataClassification.GTM);
    assertEquals(BigInteger.valueOf(2), secondSerialFreddyForGtm);

    BigInteger thirdSerialFreddyForGtm =
        subject.handleNewKeypair("freddy krueger", CertificateRequest.DataClassification.GTM);
    assertEquals(BigInteger.valueOf(3), thirdSerialFreddyForGtm);

    BigInteger firstSerialFreddyForTrivial =
        subject.handleNewKeypair("freddy krueger", CertificateRequest.DataClassification.TRIVIAL);
    assertEquals(BigInteger.valueOf(1), firstSerialFreddyForTrivial);
  }
}
