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
from dra.route_enrichment_manager import Route_Enrichment_Manager
from dra.constants import IP_ADDRESSES_KEY

class fake_ip_address_service:

    def __init__(self, ec2_ip_addresses_by_tag, asg_ip_addresses_by_tag, asg_ip_addresses_by_name):
        self.ec2_ip_addresses_by_tag = ec2_ip_addresses_by_tag
        self.asg_ip_addresses_by_tag = asg_ip_addresses_by_tag
        self.asg_ip_addresses_by_name = asg_ip_addresses_by_name

    def get_running_ip_addresses_by_tag(self, tag_key, tag_value):
        return self.ec2_ip_addresses_by_tag

    def get_asg_ips_for_asg_tag(self, tag_key, tag_value):
        return self.asg_ip_addresses_by_tag

    def get_asg_ips_for_asg_names(self, asg_names):
        return self.asg_ip_addresses_by_name

class Test_route_enrichment_manager:

    def test_populate_tagged_ec2_routes_with_ip_addresses(self):
        tag_parameter_array = [
            {
                'tag_key': 'Name',
                'tag_value': 'tag_value_1',
                'path': '/path1/',
                'port_number': 'port1',
                'tls_enabled': False
            },
            {
                'tag_key': 'Name',
                'tag_value': 'tag_value_2',
                'path': '/path2/',
                'port_number': 'port2',
                'tls_enabled': True
            },
            {
                'tag_key': 'Name',
                'tag_value': 'tag_value_3',
                'path': '/path3/',
                'port_number': 'port3',
                'tls_enabled': False
            }
        ]

        EC2_IP_ADDRESS_BY_TAG = ['1.2.3.4', '1.2.3.5']

        expected_results = [
            {
                'tag_key': 'Name',
                'tag_value': 'tag_value_1',
                'path': '/path1/',
                'port_number': 'port1',
                'tls_enabled': False,
                IP_ADDRESSES_KEY: EC2_IP_ADDRESS_BY_TAG
            },
            {
                'tag_key': 'Name',
                'tag_value': 'tag_value_2',
                'path': '/path2/',
                'port_number': 'port2',
                'tls_enabled': True,
                IP_ADDRESSES_KEY: EC2_IP_ADDRESS_BY_TAG
            },
            {
                'tag_key': 'Name',
                'tag_value': 'tag_value_3',
                'path': '/path3/',
                'port_number': 'port3',
                'tls_enabled': False,
                IP_ADDRESSES_KEY: EC2_IP_ADDRESS_BY_TAG
            }
        ]

        ip_address_service = fake_ip_address_service(EC2_IP_ADDRESS_BY_TAG, None, None)
        subject = Route_Enrichment_Manager(ip_address_service, None)
        results = subject.populate_tagged_ec2_routes_with_ip_addresses(tag_parameter_array)

        assert results == expected_results

    def test_populate_tagged_asg_routes_with_ip_addresses(self):
        tag_parameter_array = [
            {
                'tag_key': 'Name',
                'tag_value': 'tag_value-1',
                'path': '/path1/',
                'port_number': 'port1',
                'tls_enabled': False
            },
            {
                'tag_key': 'Name',
                'tag_value': 'tag_value-2',
                'path': '/path2/',
                'port_number': 'port2',
                'tls_enabled': True
            },
            {
                'tag_key': 'Name',
                'tag_value': 'tag_value-3',
                'path': '/path3/',
                'port_number': 'port3',
                'tls_enabled': False
            }
        ]

        ASG_IP_ADDRESS_BY_TAG = ['1.2.3.4', '1.2.3.5']

        expected_results = [
            {
                'tag_key': 'Name',
                'tag_value': 'tag_value-1',
                'path': '/path1/',
                'port_number': 'port1',
                'tls_enabled': False,
                IP_ADDRESSES_KEY: ASG_IP_ADDRESS_BY_TAG
            },
            {
                'tag_key': 'Name',
                'tag_value': 'tag_value-2',
                'path': '/path2/',
                'port_number': 'port2',
                'tls_enabled': True,
                IP_ADDRESSES_KEY: ASG_IP_ADDRESS_BY_TAG
            },
            {
                'tag_key': 'Name',
                'tag_value': 'tag_value-3',
                'path': '/path3/',
                'port_number': 'port3',
                'tls_enabled': False,
                IP_ADDRESSES_KEY: ASG_IP_ADDRESS_BY_TAG
            }
        ]

        ip_address_service = fake_ip_address_service(None, ASG_IP_ADDRESS_BY_TAG, None)
        subject = Route_Enrichment_Manager(ip_address_service, None)
        results = subject.populate_tagged_asg_routes_with_ip_addresses(tag_parameter_array)

        assert results == expected_results

    def test_populate_named_asg_routes_with_ip_addresses(self):

        name_parameter_array = [
            {
                'asg_name_value': 'name_value_1',
                'path': '/path1/',
                'port_number': 'port1',
                'tls_enabled': False
            },
            {
                'asg_name_value': 'name_value_2',
                'path': '/path2/',
                'port_number': 'port2',
                'tls_enabled': True
            },
            {
                'asg_name_value': 'name_value_3',
                'path': '/path3/',
                'port_number': 'port3',
                'tls_enabled': False
            }
        ]

        ASG_IP_ADDRESS_BY_NAME = ['1.2.3.4', '1.2.3.5']

        expected_results = [
            {
                'asg_name_value': 'name_value_1',
                'path': '/path1/',
                'port_number': 'port1',
                'tls_enabled': False,
                IP_ADDRESSES_KEY: ASG_IP_ADDRESS_BY_NAME
            },
            {
                'asg_name_value': 'name_value_2',
                'path': '/path2/',
                'port_number': 'port2',
                'tls_enabled': True,
                IP_ADDRESSES_KEY: ASG_IP_ADDRESS_BY_NAME
            },
            {
                'asg_name_value': 'name_value_3',
                'path': '/path3/',
                'port_number': 'port3',
                'tls_enabled': False,
                IP_ADDRESSES_KEY: ASG_IP_ADDRESS_BY_NAME
            }
        ]

        ip_address_service = fake_ip_address_service(None, None, ASG_IP_ADDRESS_BY_NAME)
        subject = Route_Enrichment_Manager(ip_address_service, None)
        results = subject.populate_named_asg_routes_with_ip_addresses(name_parameter_array)

        assert results == expected_results
