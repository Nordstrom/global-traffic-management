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
from dra.dynamicrouteagent import Dynamic_Route_Agent
from dra.constants import RDS_USER
from dra.constants import RDS_PASSWORD
from dra.constants import RDS_HOST
from dra.constants import RDS_DATABASE
from dra.constants import IP_ADDRESSES_KEY

class fake_logger:
    def info(self, *args):
        return None

class fake_args:
    def __init__(self, route_conf_path, route_parameter_file=None, rds_config_file=None):
        self.route_conf_path = route_conf_path
        self.route_parameter_file = route_parameter_file
        self.rds_config_file = rds_config_file

class fake_route_enrichment_manager:

    def __init__(self, tagged_ec2_ip_address_array, tagged_asg_ip_address_array, named_asg_ip_address_array):
        self.tagged_ec2_ip_address_array = tagged_ec2_ip_address_array
        self.tagged_asg_ip_address_array = tagged_asg_ip_address_array
        self.named_asg_ip_address_array = named_asg_ip_address_array

    def __add_ip(self, route_parameter, mode):
        if mode == 'tagged_ec2':
            route_parameter[IP_ADDRESSES_KEY] = self.tagged_ec2_ip_address_array
        elif mode == 'tagged_asg':
            route_parameter[IP_ADDRESSES_KEY] = self.tagged_asg_ip_address_array
        elif mode == 'named_asg':
            route_parameter[IP_ADDRESSES_KEY] = self.named_asg_ip_address_array

        return route_parameter

    def populate_tagged_ec2_routes_with_ip_addresses(self, route_parameters):
        self.tagged_ec2_route_parameters = route_parameters
        return [self.__add_ip(route_parameter, 'tagged_ec2') for route_parameter in route_parameters]

    def populate_tagged_asg_routes_with_ip_addresses(self, route_parameters):
        self.tagged_asg_route_parameters = route_parameters
        return [self.__add_ip(route_parameter, 'tagged_asg') for route_parameter in route_parameters]

    def populate_named_asg_routes_with_ip_addresses(self, route_parameters):
        self.named_asg_route_parameters = route_parameters
        return [self.__add_ip(route_parameter, 'named_asg') for route_parameter in route_parameters]

class fake_introspect_service:
    def __init__(self, cloud_account_id):
        self.cloud_account_id = cloud_account_id

    def get_account_id(self):
        return self.cloud_account_id

class fake_rds_config_loader:
    def unpack_rds_config_file(self, logger, rds_config_file):
        return {
                RDS_USER: "",
                RDS_PASSWORD: "",
                RDS_HOST: "",
                RDS_DATABASE: ""
                }

class fake_rds_service:
    def __init__(self, route_parameter_file):
        self.route_parameter_file = route_parameter_file

    def fetch_route_information(self, *arg):
        f = open (self.route_parameter_file)
        response = json.load(f)
        f.close()
        return response

class fake_route_diff_manager:
    def __init__(self, are_routes_different):
        self._are_routes_different = are_routes_different
        self.did_call_update_route_conf = False

    def are_routes_different(self, logger, new_route_string, path):
        self._recorded_route_string = new_route_string
        return self._are_routes_different

    def update_route_conf(self, new_route_string, path):
        self.did_call_update_route_conf = True
        return None

class Test_Dynamic_Route_Agent:

    def test_introspect_building_routes_from_inputs_when_they_are_different_from_current_route_conf(self, testdata_dir):
        args = fake_args(testdata_dir + '/nonexistantfile.json')
        logger = fake_logger()
        route_enrichment_manager = fake_route_enrichment_manager( \
                tagged_ec2_ip_address_array=["1.2.3.4"], \
                tagged_asg_ip_address_array=["2.3.4.5"], \
                named_asg_ip_address_array=["3.4.5.6"]) #give every route this ip address

        route_diff_manager = fake_route_diff_manager(True) # return routes are DIFFERENT
        introspect_service = fake_introspect_service('fake_cloud_account_id')
        rds_service = fake_rds_service(testdata_dir + '/dynamicrouteagent_test_route_parameter_file.json')
        rds_config_loader = fake_rds_config_loader()

        # create the subject
        subject = Dynamic_Route_Agent(route_enrichment_manager, route_diff_manager, introspect_service, rds_service, rds_config_loader)
        # execute the behavior
        subject.run_with_introspect(logger, args)

        # load results from test to json object
        results = json.loads(route_diff_manager._recorded_route_string)

        # load results from expected results file
        f = open (testdata_dir + '/expected_dynamicrouteagent_test_results.json')
        expected_results = json.load(f)
        f.close()

        # make sure that the update method on the route_diff_manager was invoked
        assert route_diff_manager.did_call_update_route_conf == True
        # make sure the results match the expected results in the testdata folder
        assert expected_results == results

    def test_introspect_building_routes_from_inputs_when_they_are_the_same_as_the_current_route_conf(self, testdata_dir):
        args = fake_args(testdata_dir + '/nonexistantfile.json')
        logger = fake_logger()
        route_enrichment_manager = fake_route_enrichment_manager( \
                tagged_ec2_ip_address_array=["1.2.3.4"], \
                tagged_asg_ip_address_array=["2.3.4.5"], \
                named_asg_ip_address_array=["3.4.5.6"]) #give every route this ip address

        route_diff_manager = fake_route_diff_manager(False) # return routes are the SAME
        introspect_service = fake_introspect_service('fake_cloud_account_id')
        rds_service = fake_rds_service(testdata_dir + '/dynamicrouteagent_test_route_parameter_file.json')
        rds_config_loader = fake_rds_config_loader()

        # create the subject
        subject = Dynamic_Route_Agent(route_enrichment_manager, route_diff_manager, introspect_service, rds_service, rds_config_loader)
        # execute the behavior
        subject.run_with_introspect(logger, args)

        # make sure that we did not call update to routes
        assert route_diff_manager.did_call_update_route_conf == False

