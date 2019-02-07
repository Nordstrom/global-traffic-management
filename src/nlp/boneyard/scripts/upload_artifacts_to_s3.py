import boto3
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--local_name', help="example: --local_name nlp-0.1.1-SNAPSHOT-all.jar")
parser.add_argument('--s3_name', help="example: --s3_name nlp.jar")
parser.add_argument('--application_conf', help="example --application_conf ./int-test-deploy/application.conf")
parser.add_argument('--route_conf', help="example --route_conf ./int-test-deploy/route.conf")

args = parser.parse_args()

if args.local_name == '' or args.s3_name == '':
    print(parser.format_help())
    sys.exit(0)

session = boto3.Session()
s3_client = session.client('s3', region_name='us-west-2')

s3_client.upload_file(args.local_name, 'nonprod-taco-integration-testing-helpers', args.s3_name)
s3_client.upload_file(args.application_conf, 'nonprod-taco-integration-testing-helpers', 'application.conf')
s3_client.upload_file(args.route_conf, 'nonprod-taco-integration-testing-helpers', 'route.conf')
