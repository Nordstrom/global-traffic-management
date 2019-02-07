import os.path
import sys

from setuptools import setup

PROJECT_ROOT = os.path.abspath(os.path.dirname(__file__))

with open(PROJECT_ROOT + '/VERSION') as f:
    VERSION = f.read()

setup(
    name='dynamicrouteagent',
    version=VERSION,
    packages=['dra'],
    install_requires=[
        'pex==1.4.1',
        'requests==2.18.4',
        'jmespath==0.9.3',
        'boto3==1.7.9',
        'botocore==1.10.9',
        'python-dateutil==2.7.2',
        'PyMySQL>=0.8.1,<0.9'
    ],
    include_package_data=True,
    zip_safe=False
)
