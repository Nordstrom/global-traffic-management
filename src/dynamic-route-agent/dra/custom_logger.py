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
import os
import os.path
from .json_formatter import JSON_Formatter
from datetime import datetime

class Custom_Logger(logging.Formatter):
    def get_custom_logger(self):
        formatter = JSON_Formatter()
        # turn off logging for boto3 and botocore, unless critical
        logging.getLogger('boto3').setLevel(logging.CRITICAL)
        logging.getLogger('botocore').setLevel(logging.CRITICAL)
        # set up logging to file - see previous section for more details
        logging.basicConfig(level=logging.INFO)

        # filehandler for writing to log directory
        LOG_DIR = '/var/log/dynamicrouteagent'
        if not os.path.exists(LOG_DIR):
            os.makedirs(LOG_DIR)
        fh = logging.FileHandler(filename='{}/dynamicrouteagent.log'.format(LOG_DIR), mode='a')
        fh.setLevel(logging.INFO)
        fh.setFormatter(formatter)

        # add the handlers to the root logger
        logging.getLogger('dynamicrouteagent').addHandler(fh)

        return logging.getLogger('dynamicrouteagent')

