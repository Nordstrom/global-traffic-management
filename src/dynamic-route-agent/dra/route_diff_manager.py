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

import boto3
import sys
import json
import os
import argparse
import hashlib
from .constants import MESSAGE

temp_file_name = './temp_route.dat'

class Route_Diff_Manager:
    """
    This class is responsible for detecting if a route configuration string is different
    than the one that is loaded off of disk (through the --rcp parameter)
    """

    def update_route_conf(self, config_string, route_config_path):
        f = open(temp_file_name, 'w+')
        f.write(config_string)
        f.close()
        os.rename(temp_file_name, route_config_path)

    def are_routes_different(self, logger, route_config_string, route_config_path):
        existing_file = None
        try:
            existing_file = open(route_config_path)
            existing_contents = existing_file.read()
            existing_file.close()

            existing_hash = hashlib.md5()
            new_hash = hashlib.md5()
            existing_hash.update(existing_contents.encode('utf-8'))
            new_hash.update(route_config_string.encode('utf-8'))
            existing_digest = existing_hash.digest()
            new_digest = new_hash.digest()
            return new_digest != existing_digest
        except:
            if logger != None:
                logger.info({MESSAGE:"failed to read the reference typesafeconfig file, perhaps the original file %s did not exist" % (route_config_path)})
            return True
        finally:
            if existing_file is not None:
                existing_file.close()
