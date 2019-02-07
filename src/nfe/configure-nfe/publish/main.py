#!/usr/bin/env python

import argparse
import boto3
import collections
import json
import os
import subprocess
from pprint import pprint
from StringIO import StringIO

def publish_manifest(s3, bucket, key, local_file):
  s3.upload_file(local_file, bucket, key)

def publish_file(s3, bucket, key, file_data):
  data = StringIO(file_data)
  s3.upload_fileobj(data, bucket, key)

def load_manifest(manifest_file):
  print "loading manifest file:", manifest_file
  with file(manifest_file, 'r') as data:
    return json.load(data)

def verify_manifest(manifest):
  files = manifest.get("files", [])
  if len(files) < 1:
    print "no files specified in manifest"
    return False
  for entry in manifest.get("files"):
    if entry.get("name", "") == "":
      print "files entry missing name"
      return False
    if entry.get("dest", "") == "":
      print "files entry missing dest"
      return False

  return True

def parse_args():
  parser = argparse.ArgumentParser()
  parser.add_argument('configuration_bucket')
  parser.add_argument('deploy_env', choices=['nonprod', 'prod'])
  parser.add_argument('deploy_version')
  parser.add_argument('deploy_dir')
  return parser.parse_args()

def main():
  args = parse_args()
  s3 = boto3.client('s3')
  kms = boto3.client('kms')

  manifest_file = os.path.join(args.deploy_dir, 'manifest.json')

  manifest = load_manifest(manifest_file)
  if not verify_manifest(manifest):
    return

  manifest_s3_key = "nfe/{version}/manifest.json".format(version=args.deploy_version)
  print manifest_s3_key
  publish_manifest(s3, args.configuration_bucket, manifest_s3_key, manifest_file)
  for entry in manifest.get("files"):
    entry_file = os.path.join(args.deploy_dir, entry.get("name"))
    entry_s3_key = "nfe/{version}/{name}".format(version=args.deploy_version, name=entry.get("name"))
    entry_encryption_key = entry.get("encryption_key")
    entry_data = None
    with file(entry_file, 'r') as data:
      entry_data = data.read()
    print entry_file, entry_s3_key, entry_encryption_key
    if entry_encryption_key:
      print "encrypting", entry_file
      response = kms.encrypt(KeyId=entry_encryption_key, Plaintext=entry_data)
      entry_data = response['CiphertextBlob']
    publish_file(s3, args.configuration_bucket, entry_s3_key, entry_data)

if __name__ == '__main__':
  main()
