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
import pprint

temp_file_name = './temp_route.dat'

class Route_String_Factory:
    """
    This class is responsible for creating the templatlated route configuration file based on computed data
    """

    def create_new_conf_string(self, route_array):
        config_string = self._create_conf(route_array)
        config_string.encode('utf-8')
        return config_string

    def _create_conf(self, route_array):
        conf  = '{\n'
        conf += '  routes = [\n'
        conf +=      self._create_routes(route_array)      
        conf += '  ]\n'
        conf += '}\n'
        return conf

    def _create_routes(self, route_array):
        route_conf = ''
        for route in route_array:
            route_name = route['name']
            route_path = route['path']
            ip_addresses = route['ip_addresses']
            client_name = route['client_name']
            port_number = route['port_number']
            tls_enabled = route['tls_enabled']

            route_conf += '    ${nlp.proxyRouteTemplate} {\n'
            route_conf += '      name = %s\n' % (route_name)
            route_conf += '      path = "%s"\n' % (route_path)
            route_conf += '      clients = [\n'
            route_conf +=          self._create_client_config(ip_addresses, client_name, port_number, tls_enabled)
            route_conf += '      ]\n'
            route_conf += '    }\n'
        return route_conf

    def _create_client_config(self, ip_addresses, client_name, port_number, tls_enabled):
        combined_string = '' 
        for ip in ip_addresses:
            combined_string += '       ${xio.clientTemplate} {\n'
            if tls_enabled == False:
                combined_string += '         settings {\n'
                combined_string += '           tls {\n'
                combined_string += '             useSsl = false\n'
                combined_string += '           }\n'
                combined_string += '         }\n'
            combined_string += '         name = "%s"\n' % (client_name)
            combined_string += '         remoteIp = "%s"\n' % (ip)
            combined_string += '         remotePort = %s\n' % (port_number)
            combined_string += '       }\n'
        return combined_string


#------------------------------
# Built in helpful test drive
#------------------------------
if __name__ == "__main__":
    route_array = [
        {
            'name': 'route1',
            'path': '/path1/',
            'ip_addresses': [
                '123.123.123.123',
                '123.123.123.124',
                '123.123.123.125'
            ],
            'client_name': 'client1',
            'port_number': 'port1',
            'tls_enabled': False
        },
        {
            'name': 'route2',
            'path': '/path2/',
            'ip_addresses': [
                '223.123.123.123',
                '223.123.123.124',
                '223.123.123.125'
            ],
            'client_name': 'client2',
            'port_number': 'port2',
            'tls_enabled': True
        },
        {
            'name': 'route3',
            'path': '/path3/',
            'ip_addresses': [
                '323.123.123.123',
                '323.123.123.124',
                '323.123.123.125'
            ],
            'client_name': 'client3',
            'port_number': 'port3',
            'tls_enabled': False
        }
    ]

    subject = Route_String_Factory()
    result = subject.create_new_conf_string(route_array)
    print(result)
