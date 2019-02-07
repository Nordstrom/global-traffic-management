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

import sys
from .constants import MESSAGE

class Argument_Validator:
    """
    This class is used to validate the input parameters into the main application
    """

    def __init__(self, parser, logger):
        self.parser = parser
        self.logger = logger

    def __help_print(self):
        print("There are two ways run the agent")
        print("1. Using a local route_parameter.json file")
        print("dynamicrouteagent.pex --introspect False --route_conf_path routes.json --mode tagged_asg --route_parameter_file route_parameters.json")
        print("2. Using the introspection to and database look up")
        print("dynamicrouteagent.pex --introspect True --route_conf_path routes.json  --rds_config_file file_that_has_rds_infomation")

    def validate_params(self):
        # these parameters are always required
        self.parser.add_argument('--introspect', '--intro', help='example: --introspect True/False', type=str, default='')
        self.parser.add_argument('--route_conf_path', '--rcp', help='example: --rcp path/to/route.json  (This is the json config file that nlp ingests up)', type=str, default='')

        # these parameters are only relevent in NON introspect mode
        self.parser.add_argument('--route_parameter_file', '--rpf', help='example: --rpf path/to/route_parameter_file )', type=str, default='')

        # these parameters are only relevant if you are using introspect mode
        self.parser.add_argument('--rds_config_file', help='example: --rds_config_file my_config_file_that_has_username_password_host_database', type=str, default='')

        args = self.parser.parse_args()

        if args.route_conf_path == '' or args.introspect == '' or (args.introspect != 'True' and args.introspect != 'False'):
            self.logger.info({MESSAGE:"invalid arguments provided, you need to provide route_config_path and specify introspect True/False"})
            print(self.parser.format_help())
            self.__help_print()
            sys.exit(0)

        if args.introspect == 'True' and args.rds_config_file == '':
            self.logger.info({MESSAGE:"invalid arguments provided, when using introspect mode you need to provide user/password/host/database. This mode defaults to TAGGED_ASG"})
            print(self.parser.format_help())
            self.__help_print()
            sys.exit(0)

        if args.introspect == 'False' and args.route_parameter_file == '':
            self.logger.info({MESSAGE:"invalid arguments provided, when NOT using introspect mode you must provide a route_parameter_file AND mode"})
            print(self.parser.format_help())
            self.__help_print()
            sys.exit(0)

        return args

