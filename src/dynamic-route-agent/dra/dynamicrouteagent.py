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
import os.path
import argparse
import logging
import base64
import requests
import pymysql.cursors

from .asg_manager import Asg_Manager
from .ec2_tag_query_manager import EC2_Tag_Query_Manager
from .ip_address_service import Ip_Address_Service
from .route_diff_manager import Route_Diff_Manager
from .route_enrichment_manager import Route_Enrichment_Manager
from .introspect_service import Introspect_Service
from .rds_service import Rds_Service
from .json_formatter import JSON_Formatter
from .custom_logger import Custom_Logger
from .rds_config_loader import Rds_Config_Loader
from .argument_validator import Argument_Validator
from .constants import MESSAGE
from .constants import JSON_STRING
from .constants import MODE_KEY
from .constants import MODE_TAGGED_EC2
from .constants import MODE_TAGGED_ASG
from .constants import MODE_NAMED_EC2
from .constants import MODE_IP_ADDRESS_LIST

class Dynamic_Route_Agent:
    """
    This class is the top level class used to fetch/write the route.conf file
    """

    def __init__(self, route_enrichment_manager, route_diff_manager, introspect_service, rds_service, rds_config_loader):
        self.route_enrichment_manager = route_enrichment_manager
        self.route_diff_manager = route_diff_manager
        self.introspect_service = introspect_service
        self.rds_service = rds_service
        self.rds_config_loader = rds_config_loader

    # This method is used when we want to let the dynamicrouteagent figure out what account it is in and use that information to fetch the ipaddresses
    def run_with_introspect(self, logger, args):
        # find out what cloud account id we should use
        self.account_id = self.introspect_service.get_account_id()
        # extract the rds_parameters out of rds_config_file
        rds_parameters = self.rds_config_loader.unpack_rds_config_file(logger, args.rds_config_file)
        # only continue if we actually got an account_id
        if self.account_id is not None and rds_parameters is not None:
            route_parameter_array = self.rds_service.fetch_route_information(rds_parameters, self.account_id)
            self.__run(logger, route_parameter_array, args)
        else:
            error_message = ''
            if self.account_id is None:
                error_message += 'Unable to introspect account_id. '
            if rds_parameters is None:
                error_message += 'Unable to parse all of the required rds_config_file parameters. on cloud_account_id: %s' % (self.account_id)
            # if we are unable to get an account_id or rds_parameters we will not update the routes
            logger.info({MESSAGE:error_message})

    # This method is used when we want to explicitly specify an input json instead of using introspection
    def run_with_config(self, logger, args):
        route_parameter_array = []
        # try to open the route_parameter_file submitted through args
        try:
            with open(args.route_parameter_file) as json_data:
                route_parameter_array = json.load(json_data)
        except:
            # if the route parameter file is invalid bail out and don't update anything
            logger.info({MESSAGE:"invalid route_parameter_file provided"})
            sys.exit(0)

        self.__run(logger, route_parameter_array, args)

    # __filter is used when we are separating the routes based on mode type
    def __filter(self, route_parameter, mode_type):
        mode = route_parameter.get(MODE_KEY)
        return mode is not None and mode == mode_type

    # __extract filters the input array for the mode_type
    def __extract(self, route_parameter_array, mode_type):
        # filter out routes that are missing parameters, then use the tag_key/tag_value to populate the ip_addresses for each route
        return [route_parameter for route_parameter in route_parameter_array if self.__filter(route_parameter, mode_type)]

    # __enrich_routes separates the various route_parameter entries by mode and enrich them with ip addresses
    def __enrich_routes(self, logger, route_parameter_array, args):
        enriched_route_array = []
        try:
            ec2_tagged_route_parameters = self.__extract(route_parameter_array, MODE_TAGGED_EC2)
            enriched_route_array.extend(self.route_enrichment_manager.populate_tagged_ec2_routes_with_ip_addresses(ec2_tagged_route_parameters))

            asg_tagged_route_parameters = self.__extract(route_parameter_array, MODE_TAGGED_ASG)
            enriched_route_array.extend(self.route_enrichment_manager.populate_tagged_asg_routes_with_ip_addresses(asg_tagged_route_parameters))

            asg_named_route_parameters = self.__extract(route_parameter_array, MODE_NAMED_EC2)
            enriched_route_array.extend(self.route_enrichment_manager.populate_named_asg_routes_with_ip_addresses(asg_named_route_parameters))

            # MODE_IP_ADDRESS_LIST is already enriched with sweet ip addresses so we just use that
            ip_address_list_route_parameters = self.__extract(route_parameter_array, MODE_IP_ADDRESS_LIST)
            enriched_route_array.extend(ip_address_list_route_parameters)

        except:
            account_id_string = "There was an issue contacting the cloud services when enriching routes on cloud_account_id: %s" % (self.account_id)
            logger.info({MESSAGE:account_id_string})
            raise

        return enriched_route_array

    # __run actually does all the work of building the routes that we output
    def __run(self, logger, route_parameter_array, args):
        enriched_route_array = self.__enrich_routes(logger, route_parameter_array, args)
        new_route_string = json.dumps(enriched_route_array, indent=2, separators=(',', ': '))
        if self.route_diff_manager.are_routes_different(logger, new_route_string, args.route_conf_path):
            self.route_diff_manager.update_route_conf(new_route_string, args.route_conf_path) # this saves the route.json file to the host
            update_message = "new routes updated for cloud_account_id: %s" % (self.account_id)
            logger.info({MESSAGE:update_message, JSON_STRING:new_route_string})
        else:
            update_message = "routes are the same for cloud_account_id: %s" % (self.account_id)
            logger.info({MESSAGE:update_message, JSON_STRING:new_route_string})

def __build_agent(logger, args):
    session = boto3.Session()
    asg_client = session.client('autoscaling', region_name='us-west-2')
    ec2_client = session.client('ec2', region_name='us-west-2')

    asg_manager = Asg_Manager(asg_client, ec2_client)
    ec2_tag_manager = EC2_Tag_Query_Manager(ec2_client)
    ip_address_service = Ip_Address_Service(asg_manager, ec2_tag_manager)
    route_enrichment_manager = Route_Enrichment_Manager(ip_address_service, logger)
    route_diff_manager = Route_Diff_Manager()
    # 5 second timeout on the introspect service
    introspect_service = Introspect_Service(requests, logger, 5)
    rds_service = Rds_Service(pymysql, logger)
    rds_config_loader = Rds_Config_Loader()

    return Dynamic_Route_Agent(route_enrichment_manager, route_diff_manager, introspect_service, rds_service, rds_config_loader)

def main():
    # build custom logger with the correct formatting
    custom_logger = Custom_Logger()
    logger = custom_logger.get_custom_logger()

    # build the command line argument parser and validate the parameters
    parser = argparse.ArgumentParser()
    argument_validator = Argument_Validator(parser, logger)
    args = argument_validator.validate_params()

    # build the all the dependencies for the agent and the agent itself
    agent = __build_agent(logger, args)

    # invoke the main operation of the agent
    if args.introspect == 'True':
        agent.run_with_introspect(logger, args)
    else:
        agent.run_with_config(logger, args)

if __name__ == "__main__":
    main()
