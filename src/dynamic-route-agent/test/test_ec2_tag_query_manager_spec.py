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

import json
import sys
from dra.ec2_tag_query_manager import EC2_Tag_Query_Manager

class fake_ec2_client:
    def __init__(self, filename):
        self.filename = filename

    def describe_instances(self, **kwargs):
        f = open(self.filename)
        response = json.load(f)
        f.close()
        return response

class TestEC2TagQueryManager:

    def test_getting_instance_ids_for_asgs_by_tags(self, testdata_dir):
        ec2_client = fake_ec2_client(testdata_dir + '/ec2.json')
        subject = EC2_Tag_Query_Manager(ec2_client)
        ip_addresses = subject.get_running_ip_addresses_by_tag("tag_key", "tag_value")
        expected_ip_addresses = ["1.1.1.1", "1.1.1.2", "1.1.1.4", "1.1.1.5"]

        assert ip_addresses == expected_ip_addresses


if __name__ == '__main__':
    unittest.main()
