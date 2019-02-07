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

class Asg_Manager:
    """
    This class is responsible for fetching the ip addresses
    associated with a given asg_name or a tag key/value pair
    """

    def __init__(self, asg_client, ec2_client):
        self.asg_client = asg_client
        self.ec2_client = ec2_client

    def _get_healthy_instance_ids_for_asgs(self, asgs):
        instanceIds = []
        for asg in asgs:
            instances = asg['Instances']
            for instance in instances:
                if instance['HealthStatus'] == 'Healthy':
                    instanceIds.append(instance['InstanceId'])
        return instanceIds

    def _get_instance_ids_from_asg_tag(self, page_iterator, gtm_tag_key, asg_tag):
        custom_filter = 'AutoScalingGroups[]'
        # in case you were wondering the below filter is using http://jmespath.org json query language 
        custom_filter = ('{} | [?contains(Tags[?Key==`{}`].Value, `{}`)]'.format(custom_filter, gtm_tag_key, asg_tag))
        filtered_asgs = page_iterator.search(custom_filter)
        return self._get_healthy_instance_ids_for_asgs(filtered_asgs)

    def _get_instance_ids_from_asg_names(self, asg_names):
        response = self.asg_client.describe_auto_scaling_groups(
            AutoScalingGroupNames=asg_names
        )
        asgs = response['AutoScalingGroups']
        return self._get_healthy_instance_ids_for_asgs(asgs)

    def _get_ips_from_instance_ids(self, instance_ids):
        if len(instance_ids) == 0:
            return []
        response = self.ec2_client.describe_instances(
            InstanceIds=instance_ids
        )

        ipAddresses = []
        reservations = response['Reservations']
        for res in reservations:
           instances = res['Instances']
           for instance in instances:
               network_interfaces = instance['NetworkInterfaces']
               for nic in network_interfaces:
                  ipAddresses.append(nic['PrivateIpAddress']) 
        return ipAddresses

    #-------------------------------------------------
    def get_asg_ips_for_asg_names(self, asg_names):
        instance_ids = self._get_instance_ids_from_asg_names(asg_names)
        ips = self._get_ips_from_instance_ids(instance_ids)
        return ips

    def get_asg_ips_for_asg_tag(self, asg_tag_key, asg_tag_name):
        paginator = self.asg_client.get_paginator('describe_auto_scaling_groups')
        page_iterator = paginator.paginate(PaginationConfig={'PageSize': 100})
        instance_ids = self._get_instance_ids_from_asg_tag(page_iterator, asg_tag_key, asg_tag_name)
        ips = self._get_ips_from_instance_ids(instance_ids)
        return ips

#------------------------------
# Built in helpful test drive
#------------------------------
if __name__ == "__main__":
    pp = pprint.PrettyPrinter(indent=4)
    session = boto3.Session()
    asg_client = session.client('autoscaling', region_name='us-west-2')
    ec2_client = session.client('ec2', region_name='us-west-2')

    subject = Asg_Manager(asg_client, ec2_client)
    ips = subject.get_asg_ips_for_asg_names(["MyAutoscalingGroup-FOO"])
    pp.pprint(ips)

    ips = subject.get_asg_ips_for_asg_tag("Name", "myasgtag")
    pp.pprint(ips)


