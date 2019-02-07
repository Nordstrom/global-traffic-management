import boto3
import argparse
import time
from find_ami import find_ami
from find_security_group import find_security_group

def is_auto_scaling_group_alive(asg_client, asg_name):
    response = asg_client.describe_auto_scaling_groups(
        AutoScalingGroupNames=[asg_name]
    )
    return len(response['AutoScalingGroups']) > 0

def is_launch_configuration_alive(asg_client, lc_name):
    response = asg_client.describe_launch_configurations(
        LaunchConfigurationNames=[lc_name]
    )
    return len(response['LaunchConfigurations']) > 0

def get_instance_ids(asg_client, asg_name):
    response = asg_client.describe_auto_scaling_groups(
        AutoScalingGroupNames=[asg_name]
    )

    instanceIds = []
    asgs = response['AutoScalingGroups']
    for asg in asgs:
        instances = asg['Instances']
        for instance in instances:
            if instance['HealthStatus'] == 'Healthy':
                instanceIds.append(instance['InstanceId'])
    return instanceIds

def get_ips_from_instance_ids(ec2_client, instance_ids):
    if len(instance_ids) == 0:
        return []
    
    response = ec2_client.describe_instances(
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

def get_asg_ips(asg_client, ec2_client, asg_name):
    instance_ids = get_instance_ids(asg_client, asg_name)
    ips = get_ips_from_instance_ids(ec2_client, instance_ids)
    return ips

def helper_get_asg_ips(asg_name):
    session = boto3.Session()
    asg_client = session.client('autoscaling', region_name='us-west-2')
    ec2_client = session.client('ec2', region_name='us-west-2')
    ips = get_asg_ips(asg_client, ec2_client, asg_name)
    while len(ips) == 0:
        print("Waiting for origin servers to come online")
        ips = get_asg_ips(asg_client, ec2_client, asg_name)
        time.sleep(5)
    return ips

def tear_down_the_world(asg_client, name):
    if is_auto_scaling_group_alive(asg_client, name):
        print("Found an existing ASG %s, we will now kill it" % (name))
        asg_client.delete_auto_scaling_group(
            AutoScalingGroupName=name,
            ForceDelete=True
        )

    while is_auto_scaling_group_alive(asg_client, name):
        print("[AutoScalingGroup] Waiting to terminate %s . . . " % (name))
        time.sleep(5)

    if is_launch_configuration_alive(asg_client, name):
        print("Found an existing LaunchConfiguration %s, we will now kill it" % (name))
        asg_client.delete_launch_configuration(
            LaunchConfigurationName=name
        )

    while is_launch_configuration_alive(asg_client, name):
        print("[LaunchConfigurations] Waiting to terminate %s . . . " % (name))
        time.sleep(5)

def create_asgard(key_name, name, user_data):
    session = boto3.Session()
    asg_client = session.client('autoscaling', region_name='us-west-2')
    ec2_client = session.client('ec2', region_name='us-west-2')

    ami = find_ami(ec2_client, name)
    security_group = find_security_group(ec2_client)
    subnet = "subnet-3bac8a60"

    tear_down_the_world(asg_client, name)

    if user_data == '':
        asg_client.create_launch_configuration(
            LaunchConfigurationName=name,
            ImageId=ami,
            KeyName=key_name,
            SecurityGroups=[
                security_group,
            ],
            InstanceType='t2.micro',
            IamInstanceProfile='arn:aws:iam::234460336057:instance-profile/int-test-nlp'
        )
    else:
        with open(user_data) as file:
            data = file.read()
            asg_client.create_launch_configuration(
                LaunchConfigurationName=name,
                ImageId=ami,
                KeyName=key_name,
                SecurityGroups=[
                    security_group,
                ],
                InstanceType='t2.micro',
                IamInstanceProfile='arn:aws:iam::234460336057:instance-profile/int-test-nlp',
                UserData=data
            )

    asg_client.create_auto_scaling_group(
        AutoScalingGroupName=name,
        LaunchConfigurationName=name,
        MinSize=1,
        MaxSize=1,
        VPCZoneIdentifier=subnet,
        Tags=[ 
            {
                'Key': 'Name',
                'Value': name
            }
        ]
    )

    while len(get_asg_ips(asg_client, ec2_client, name)) == 0:
        print("[AutoScalingGroup] Waiting for instances to spawn on %s . . . " % (name))
        time.sleep(5)

    ips = get_asg_ips(asg_client, ec2_client, name)
    return ips

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--key_name', help="example: --key_name kratos")
    parser.add_argument('--name', help="example: --name int-test-backend-server")
    parser.add_argument('--user_data', help="example --user-data ./userData.sh")

    args = parser.parse_args()

    if args.key_name == '' or args.name == '':
        print(parser.format_help())
        sys.exit(0)

    create_asgard(args.key_name, args.name, args.user_data)
