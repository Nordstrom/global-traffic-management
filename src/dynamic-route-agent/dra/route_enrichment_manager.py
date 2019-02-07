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
import pprint
from .asg_manager import Asg_Manager
from .ec2_tag_query_manager import EC2_Tag_Query_Manager
from .ip_address_service import Ip_Address_Service
from .constants import IP_ADDRESSES_KEY
from .constants import MESSAGE

TAG_KEY = 'tag_key'
TAG_VALUE = 'tag_value'
ASG_NAME = 'asg_name_value'

class Route_Enrichment_Manager:
    """
    This class is used to fetch and append ip addresses to each route diciontary passed in
    """

    def __init__(self, ip_address_service, logger):
        self.ip_address_service = ip_address_service
        self.logger = logger

    def populate_tagged_ec2_routes_with_ip_addresses(self, route_parameter_array):
        return [self._enrich_tagged_ec2_route(route) for route in route_parameter_array if self._tagged_parameters_valid(route)]

    def populate_tagged_asg_routes_with_ip_addresses(self, route_parameter_array):
        return [self._enrich_tagged_asg_route(route) for route in route_parameter_array if self._tagged_parameters_valid(route)]

    def populate_named_asg_routes_with_ip_addresses(self, route_parameter_array):
        return [self._enrich_named_asg_route(route) for route in route_parameter_array if self._named_asg_parameters_valid(route)]

    def _tagged_parameters_valid(self, route):
        result = route.get(TAG_KEY) != None and \
                    route.get(TAG_VALUE) != None and \
                    route.get('path') != None and \
                    route.get('port_number') != None and \
                    route.get('tls_enabled') != None
        if not result:
            if self.logger is not None:
                self.logger.info({MESSAGE:'tagged_parameters_not_valid'})
        return result

    def _named_asg_parameters_valid(self, route):
        result = route[ASG_NAME] != None and \
                    route.get('path') != None and \
                    route.get('port_number') != None and \
                    route.get('tls_enabled') != None
        if not result:
            if self.logger is not None:
                self.logger.info({MESSAGE:'named_parameters_not_valid'})
        return result

    def _enrich_tagged_ec2_route(self, route):
        route[IP_ADDRESSES_KEY] = self.ip_address_service.get_running_ip_addresses_by_tag(route[TAG_KEY], route[TAG_VALUE])
        return route

    def _enrich_tagged_asg_route(self, route):
        route[IP_ADDRESSES_KEY] = self.ip_address_service.get_asg_ips_for_asg_tag(route[TAG_KEY], route[TAG_VALUE])
        return route

    def _enrich_named_asg_route(self, route):
        asgnames = [
            route[ASG_NAME] 
        ]
        route[IP_ADDRESSES_KEY] =  self.ip_address_service.get_asg_ips_for_asg_names(asgnames)
        return route


#------------------------------
# Built in helpful test drive
#------------------------------
if __name__ == "__main__":
    tag_parameter_array = [
        {
            'tag_key': 'Name',
            'tag_value': 'myTag1',
            'name': 'route1',
            'path': '/path1/',
            'client_name': 'client1',
            'port_number': 'port1',
            'tls_enabled': False
        },
        {
            'tag_key': 'Name',
            'tag_value': 'myTag2',
            'name': 'route2',
            'path': '/path2/',
            'client_name': 'client2',
            'port_number': 'port2',
            'tls_enabled': True
        },
        { 
            'tag_key': 'Name',
            'tag_value': 'myTag3',
            'name': 'route3',
            'path': '/path3/',
            'client_name': 'client3',
            'port_number': 'port3',
            'tls_enabled': False
        }
    ]

    name_parameter_array = [
        {
            'asg_name_value': 'myTag4',
            'name': 'route1',
            'path': '/path1/',
            'client_name': 'client1',
            'port_number': 'port1',
            'tls_enabled': False
        },
        {
            'asg_name_value': 'myTag5',
            'name': 'route2',
            'path': '/path2/',
            'client_name': 'client2',
            'port_number': 'port2',
            'tls_enabled': True
        },
        {
            'asg_name_value': 'myTag6',
            'name': 'route3',
            'path': '/path3/',
            'client_name': 'client3',
            'port_number': 'port3',
            'tls_enabled': False
        }
    ]


    pp = pprint.PrettyPrinter(indent=4)
    session = boto3.Session()
    ec2_client = session.client('ec2', region_name='us-west-2')
    asg_client = session.client('autoscaling', region_name='us-west-2')
    asg_manager = Asg_Manager(asg_client, ec2_client)
    ec2_tag_query_manager = EC2_Tag_Query_Manager(ec2_client)
    ip_address_service = Ip_Address_Service(asg_manager, ec2_tag_query_manager)

    subject = Route_Enrichment_Manager(ip_address_service)

    results1 = subject.populate_tagged_ec2_routes_with_ip_addresses(tag_parameter_array)
    pp.pprint(results1)
    results2 = subject.populate_tagged_asg_routes_with_ip_addresses(tag_parameter_array)
    pp.pprint(results2)
    results3 = subject.populate_named_asg_routes_with_ip_addresses(name_parameter_array)
    pp.pprint(results3)

