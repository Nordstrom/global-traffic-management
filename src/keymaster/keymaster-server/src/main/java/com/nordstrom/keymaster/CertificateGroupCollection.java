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

import com.nordstrom.keymaster.CertificateRequest.DataClassification;
import java.io.IOException;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.RequiredArgsConstructor;

@ParametersAreNonnullByDefault
@RequiredArgsConstructor
public class CertificateGroupCollection {
  private final KeymasterDao dao;
  private CertificateGroup gtmCertificateGroup = null;
  private CertificateGroup pciCertificateGroup = null;
  private CertificateGroup piiCertificateGroup = null;
  private CertificateGroup trivialCertificateGroup = null;

  @Nullable
  public CertificateGroup getCertificateGroup(DataClassification dataClassification)
      throws IOException {
    switch (dataClassification) {
      case GTM:
        return gtmCertificateGroup(dataClassification);
      case PCI:
        return pciCertificateGroup(dataClassification);
      case PII:
        return piiCertificateGroup(dataClassification);
      case TRIVIAL:
        return trivialCertificateGroup(dataClassification);
      default:
        return null;
    }
  }

  private CertificateGroup gtmCertificateGroup(DataClassification dataClassification)
      throws IOException {
    if (gtmCertificateGroup == null) {
      gtmCertificateGroup =
          new CertificateGroup(
              dataClassification, this.dao, "snakeoil.cert.pem", "snakeoil.key.pem");
    }
    return gtmCertificateGroup;
  }

  private CertificateGroup pciCertificateGroup(DataClassification dataClassification)
      throws IOException {
    if (pciCertificateGroup == null) {
      pciCertificateGroup =
          new CertificateGroup(
              dataClassification, this.dao, "snakeoil.cert.pem", "snakeoil.key.pem");
    }
    return pciCertificateGroup;
  }

  private CertificateGroup piiCertificateGroup(DataClassification dataClassification)
      throws IOException {
    if (piiCertificateGroup == null) {
      piiCertificateGroup =
          new CertificateGroup(
              dataClassification, this.dao, "snakeoil.cert.pem", "snakeoil.key.pem");
    }
    return piiCertificateGroup;
  }

  private CertificateGroup trivialCertificateGroup(DataClassification dataClassification)
      throws IOException {
    if (trivialCertificateGroup == null) {
      trivialCertificateGroup =
          new CertificateGroup(
              dataClassification, this.dao, "snakeoil.cert.pem", "snakeoil.key.pem");
    }
    return trivialCertificateGroup;
  }
}
