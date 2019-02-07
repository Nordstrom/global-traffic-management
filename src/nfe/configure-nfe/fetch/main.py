#!/usr/bin/env python

import argparse
import boto3
import collections
import json
import os
import subprocess
from pprint import pprint
from StringIO import StringIO

def fetch_manifest(s3, bucket, key):
  data = StringIO()
  s3.download_fileobj(bucket, key, data)
  return json.loads(data.getvalue())

def fetch_file(s3, bucket, key):
  data = StringIO()
  s3.download_fileobj(bucket, key, data)
  return data.getvalue()

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
  return parser.parse_args()

def main():
  args = parse_args()
  s3 = boto3.client('s3')
  kms = boto3.client('kms')

  s3_manifest_key = "nfe/{version}/manifest.json".format(version=args.deploy_version)
  manifest = fetch_manifest(s3, args.configuration_bucket, s3_manifest_key)
  print "manifest", manifest
  if not verify_manifest(manifest):
    return

  for entry in manifest.get("files"):
    entry_s3_key = "nfe/{version}/{name}".format(version=args.deploy_version, name=entry.get("name"))
    entry_file = entry.get("dest")
    print "file", entry_file
    entry_encryption_key = entry.get("encryption_key")
    print entry_file, entry_s3_key, entry_encryption_key
    entry_data = fetch_file(s3, args.configuration_bucket, entry_s3_key)
    if entry_encryption_key:
      print "decrypting", entry_file
      response = kms.decrypt(CiphertextBlob=entry_data)
      entry_data = response['Plaintext']
    dirname = os.path.dirname(entry_file)
    if not os.path.exists(dirname):
      os.makedirs(dirname)
    with file(entry_file, 'w') as data:
      data.write(entry_data)

if __name__ == '__main__':
  main()
