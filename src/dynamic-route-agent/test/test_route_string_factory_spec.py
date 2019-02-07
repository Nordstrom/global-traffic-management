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
from dra.route_string_factory import Route_String_Factory

class Test_route_string_factory:

    def test_creating_route_string(self, testdata_dir):
        enriched_route_array = [
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
        result = subject.create_new_conf_string(enriched_route_array)
        expected_route_conf = testdata_dir + "/route_string_factory_expected_results.dat"
        f = open(expected_route_conf)
        expected_results_string = f.read()
        f.close()
        assert result == expected_results_string
