# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).


## [0.5] - 2018-08-07
### Added
- Added new output type of route.json. The consumer of this sidecar will intake a json output now

## [0.6] - 2018-08-13
### Added
- Added capability to introspect based on cloud account id

## [0.7] - 2018-08-20
### Added
- Updated the rds db query that was added in 0.0.6

## [0.8] - 2018-08-21
### Added
- Changed the parameters for introspect mode, now we just point to a file that has the rds_configuration data as json

## [0.8.1] - 2018-08-23
### Added
- Updated the requirements.txt and the setup.py to include the dependencies we actually need

## [0.8.2] - 2018-08-23
### Added
- Updated the requirements.txt and the setup.py to use mysql-connector instead of mysql-connector-python-rf

## [0.8.3] - 2018-08-27
### Added
- Updated the requirements.txt and the setup.py to use PyMySql because mysql.connector has issues when pexing
- Updated code to use PyMySql

## [0.8.4] - 2018-08-27
### Added
- Updated the etc/cron.d/dra to point to route.json

## [0.8.5] - 2018-08-27
### Added
- Updated the logging to use JSON Formatter and also added a constants.py file

## [0.8.6] - 2018-08-28
### Added
- Fixed bug in logging (using json.loads(string) instead of json.load(string))

## [0.8.7] - 2018-08-29
### Added
- YOLO OPS

## [0.8.8] - 2018-08-29
### Added
- YOLO OPS

## [0.9.0] - 2018-08-30
### Added
- Updated the datamodel to expect the mode parameter, this will allow a heterogenous set of route_parameters (tagged_asg, tagged_ec2, named_asg)
- Added additional integration tests for the top level dynamicrouteagent object
- Refactored out some behaviors into separate classes

## [0.9.1] - 2018-09-04
### Added
- Added logging in the route_enrichment_manager to report out the cloud_account_id of any errors

## [0.9.2] - 2018-09-04
### Added
- Readded code to create the /var/log/dynamicrouteconfig/ folder if it does not exist

## [0.9.3] - 2018-09-13
### Added
- Added code to handle the mode type ip_address list
