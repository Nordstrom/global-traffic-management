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
import jmespath
from dra.asg_manager import Asg_Manager

class fake_page_iterator:
    def __init__(self, filename):
        self.filename = filename

    def search(self, farg, **args):
        f = open (self.filename)
        response = json.load(f)
        f.close()
        return jmespath.search(farg, response)

class fake_asg_client:
    def __init__(self, filename):
        self.filename = filename

    def describe_auto_scaling_groups(self, **kwargs):
        f = open (self.filename)
        response = json.load(f)
        f.close()
        return response

class fake_ec2_client:
    def __init__(self, filename):
        self.filename = filename

    def describe_instances(self, **kwargs):
        f = open(self.filename)
        response = json.load(f)
        f.close()
        return response

class TestAsgManager:

    def test_getting_instance_ids_for_asgs_by_tags(self, testdata_dir):
        subject = Asg_Manager(None, None)
        fake_iterator = fake_page_iterator(testdata_dir + '/asg_by_tag.json')
        # page_iterator is a proxy for a boto3 autoscalingclient page_iterator
        ip_addresses = subject._get_instance_ids_from_asg_tag(fake_iterator, 'test_key', 'test_value')
        expected_instance_ids = ["Healthy4", "Healthy6", "Healthy7", "Healthy9"]
        assert ip_addresses == expected_instance_ids

    def test_getting_instance_ids_for_asgnames(self, testdata_dir):
        asg_client = fake_asg_client(testdata_dir + '/asg1.json')
        subject = Asg_Manager(asg_client, None)
        ip_addresses = subject._get_instance_ids_from_asg_names(["doesntreallymatter"])
        expected_instance_ids = ["Healthy1", "Healthy3", "Healthy4", "Healthy6"]
        assert ip_addresses == expected_instance_ids

    def test_getting_ip_addresses_for_instanceids(self, testdata_dir):
        ec2_client = fake_ec2_client(testdata_dir + '/ec1.json')
        subject = Asg_Manager(None, ec2_client)
        ip_addresses = subject._get_ips_from_instance_ids(["doesntreallymatter"])
        expected_ip_addresses = ["1.1.1.1", "1.1.1.2", "1.1.1.3", "1.1.1.4", "1.1.1.5", "1.1.1.6", "1.1.1.7", "1.1.1.8"]
        assert ip_addresses == expected_ip_addresses

