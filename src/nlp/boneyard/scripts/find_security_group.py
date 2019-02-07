import boto3

def find_security_group(ec2_client):
  filters = [{'Name':'tag:Name', 'Values':['int-test-backend-server']}]
  results = ec2_client.describe_security_groups(Filters=filters)
  if len(results) == 0:
      print("none")
      return None
  else:
      print(results['SecurityGroups'][0]['GroupId'])
      return results['SecurityGroups'][0]['GroupId']
