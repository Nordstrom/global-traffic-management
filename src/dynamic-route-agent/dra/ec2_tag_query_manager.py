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
import pprint
import jmespath

RUNNING_CODE = 16

class EC2_Tag_Query_Manager:
    """
    This class is responsible for fetching the ec2 ip addresses associated
    with a specific tag key/value pair
    """

    def __init__(self, ec2_client):
        self.ec2_client = ec2_client

    def get_running_ip_addresses_by_tag(self, tag_key, tag_value):
        ec2_response = self._get_ec2_response_by_tag(tag_key, tag_value)
        ip_addresses = self._get_ip_addresses_from_ec2_response(ec2_response)
        return ip_addresses

    def _get_ec2_response_by_tag(self, tag_key, tag_value):
        tag_name = "tag:%s" % (tag_key)
        results = self.ec2_client.describe_instances(Filters=[
            {
                'Name': tag_name,
                'Values': [
                        tag_value,
                ]
            }])
        return results

    def _get_ip_addresses_from_ec2_response(self, ec2_response):
        ip_addresses = []
        reservations = ec2_response['Reservations']
        for res in reservations:
           instances = res['Instances']
           for instance in instances:
               ip_address = self._has_private_ip_and_is_running(instance)
               if ip_address != None:
                   ip_addresses.append(ip_address)
        return ip_addresses

    def _has_private_ip_and_is_running(self, instance):
        private_ip = instance['PrivateIpAddress']
        state = instance['State']
        if private_ip != None and state != None and state['Code'] == RUNNING_CODE:
            return private_ip
        else:
            return None



#------------------------------
# Built in helpful test drive
#------------------------------
if __name__ == "__main__":
    session = boto3.Session()
    ec2_client = session.client('ec2', region_name='us-west-2')

    subject = EC2_Tag_Query_Manager(ec2_client)
    ec2_response = subject._get_ec2_response_by_tag("Name", "MyTagName")
    private_ips = subject._get_ip_addresses_from_ec2_response(ec2_response)
    pp = pprint.PrettyPrinter(indent=4)
    pp.pprint(private_ips)

