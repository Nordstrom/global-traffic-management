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

import logging
import json
from .constants import MESSAGE
from .constants import JSON_STRING
from datetime import datetime

class JSON_Formatter(logging.Formatter):
    def __init__(self):
        logging.Formatter.__init__(self)

    def format(self, log_message):
        timestamp = datetime.fromtimestamp(log_message.created).isoformat()
        # grab raw message from log_message parameter msg
        record = log_message.msg
        json_string = record.get(JSON_STRING, '')
        json_pojo = None
        if json_string:
            try:
                json_pojo = json.loads(json_string)
            except:
                json_pojo = { "bad json" : "bad json" }

        out = {
                '@timestamp': timestamp,
                'message': record.get(MESSAGE, ''),
                'json_payload': json_pojo,
                }
        return '%s\n' % json.dumps(out)


