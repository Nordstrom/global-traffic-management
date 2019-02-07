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

from .asg_manager import Asg_Manager
from .ec2_tag_query_manager import EC2_Tag_Query_Manager

class Ip_Address_Service:
    """
    This class is a facade over the two dependency classes that implement ip address
    fetching based on tags and names
    """

    def __init__(self, asg_manager, ec2_tag_query_manager):
        self.asg_manager = asg_manager
        self.ec2_tag_query_manager = ec2_tag_query_manager

    def get_asg_ips_for_asg_names(self, asg_names):
        return self.asg_manager.get_asg_ips_for_asg_names(asg_names)

    def get_asg_ips_for_asg_tag(self, asg_tag_key, asg_tag_name):
        return self.asg_manager.get_asg_ips_for_asg_tag(asg_tag_key, asg_tag_name)

    def get_running_ip_addresses_by_tag(self, tag_key, tag_value):
        return self.ec2_tag_query_manager.get_running_ip_addresses_by_tag(tag_key, tag_value)
