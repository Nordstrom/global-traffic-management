# Copyright (C) 2018 Nordstrom, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys
import requests
from .constants import MESSAGE

AWS_METADATA_URI = 'http://169.254.169.254/latest/dynamic/instance-identity/document'
ACCOUNT_ID_KEY = 'accountId'

class Introspect_Service:
    """
    This class is used to fetch the account_id that the this instance is running in
    """

    def __init__(self, requests_service, logger, call_timeout):
        self.requests_service = requests_service
        self.logger = logger
        self.call_timeout = call_timeout

    def get_account_id(self):
        result = None
        try:
            req = self.requests_service.get(AWS_METADATA_URI, timeout=self.call_timeout)
            if req.status_code == 200:
               json_payload = req.json()
               if ACCOUNT_ID_KEY in json_payload:
                   result = json_payload[ACCOUNT_ID_KEY]
            else:
                if self.logger is not None:
                    log_val = 'there was a Request Non-200 issue with the introspecting accountId'
                    self.logger.info({MESSAGE:log_val})
        except self.requests_service.exceptions.RequestException as e:
            if self.logger is not None:
                log_val = 'there was a RequestException issue with the introspecting accountId'
                self.logger.info({MESSAGE:log_val})
        except ValueError:
            if self.logger is not None:
                log_val = 'there was a ValueError issue with the introspecting accountId'
                self.logger.info({MESSAGE:log_val})
        except:
            if self.logger is not None:
                e = sys.exc_info()[0]
                log_val = 'there was a Generic Excedption %s issue with the introspecting accountId' % (e)
                self.logger.info({MESSAGE:log_val})
        finally:
            return result

#------------------------------
# Built in helpful test drive
#------------------------------
if __name__ == "__main__":
    subject = Introspect_Service(requests, None, 5)
    print(subject.get_account_id())
