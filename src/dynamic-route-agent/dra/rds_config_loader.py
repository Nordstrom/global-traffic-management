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

import os
import json
from .constants import MESSAGE
from .constants import RDS_USER
from .constants import RDS_PASSWORD
from .constants import RDS_HOST
from .constants import RDS_DATABASE

class Rds_Config_Loader:
    """
    This class is used to load the RDS Config paramaters from file
    """

    def unpack_rds_config_file(self, logger, rds_config_file):
       result = None
       try:
           with open(rds_config_file) as f:
               result = json.load(f)
       except:
           logger.info({MESSAGE:"Unable to load rds_config_file."})
           raise

       if result is not None and \
          result[RDS_USER] != '' and \
          result[RDS_PASSWORD] != '' and \
          result[RDS_HOST] != '' and \
          result[RDS_DATABASE] != '':
           return result
       else:
           return None

