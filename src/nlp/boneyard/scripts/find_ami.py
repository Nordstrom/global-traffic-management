import boto3

def findTheNewest(x, y):
    if x['CreationDate'] > y['CreationDate']:
        return x
    else:
        return y

def find_ami(ec2_client, ami_name):
  # $AMI example is ami-123ab232
  if ami_name == '':
      print(parser.format_help())
      sys.exit(0)

  filters = [{'Name':'tag:Name', 'Values':[ami_name]}]
  results = ec2_client.describe_images(Filters=filters)
  if len(results) == 0:
      print("none")
      return None
  else:
      newest = {'CreationDate': '0'}
      for val in results['Images']:
          if val['CreationDate'] > newest['CreationDate']:
              newest = val
      print(newest['ImageId'])
      return newest['ImageId']
