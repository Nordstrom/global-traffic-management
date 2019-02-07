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

import sys
import json
import pprint
import pymysql.cursors
from .constants import MESSAGE
from .constants import MODE_KEY
from .constants import MODE_IP_ADDRESS_LIST
from .constants import RDS_USER
from .constants import RDS_PASSWORD
from .constants import RDS_HOST
from .constants import RDS_DATABASE
from .constants import ROUTE_PARAMETER_ID
from .constants import IP_ADDRESSES_KEY
from .constants import IP_ADDRESS_VALUE_KEY

class Rds_Service:
    """
    This class is used to fetch the route_parameters for the nlp in the current aws account_id
    """

    def __init__(self, sqlconnector, logger):
        self.sqlconnector = sqlconnector
        self.logger = logger

    def __fetch_ip_addresses(self, result):
        try:
            with self.cnx.cursor() as cursor:
                query = "SELECT * FROM ip_address WHERE ip_address.route_parameter_id=%s"
                cursor.execute(query, (result.get(ROUTE_PARAMETER_ID),))
                ip_results = cursor.fetchall()
                if ip_results is not None:
                    # grab all the ip values by key
                    ip_list = [ip_result.get(IP_ADDRESS_VALUE_KEY) for ip_result in ip_results]
                    # filter out any None values due to missing key in the ip_results
                    result[IP_ADDRESSES_KEY] = [ip for ip in ip_list if ip is not None]
        except:
            if self.logger is not None:
                log_val = 'there was an issue with the rds_query (fetching ip addresses) on account_id: %s' % (account_id)
                self.logger.info({MESSAGE:log_val})
            # dont raise here since we don't want to propagate a single ip_address query error up

        return result

    def __apply_mode_transform(self, result):
        if result.get(MODE_KEY) == MODE_IP_ADDRESS_LIST and result.get(ROUTE_PARAMETER_ID) is not None:
            return self.__fetch_ip_addresses(result)
        else:
            return result

    def fetch_route_information(self, rds_parameters, account_id):
        host = rds_parameters[RDS_HOST]
        user = rds_parameters[RDS_USER]
        password = rds_parameters[RDS_PASSWORD]
        database = rds_parameters[RDS_DATABASE]
        cursor = None
        self.cnx = self.sqlconnector.connect(host=host, user=user, password=password, db=database, charset='utf8mb4', cursorclass=self.sqlconnector.cursors.DictCursor)
        try:
            with self.cnx.cursor() as cursor:
                query = "SELECT * FROM route_parameter WHERE cloud_account_id=%s"
                cursor.execute(query, (account_id,))
                results = cursor.fetchall()
                results = [self.__apply_mode_transform(result) for result in results]
            return results
        except:
            if self.logger is not None:
                log_val = 'there was an issue with the rds_query on account_id: %s' % (account_id)
                self.logger.info({MESSAGE:log_val})
            raise
        finally:
            if self.cnx is not None:
                self.cnx.close()

#------------------------------
# Built in helpful test drive
#------------------------------
if __name__ == "__main__":
    subject = Rds_Service(pymysql, None)
    rds_parameters = { RDS_USER:"my-user", RDS_PASSWORD:sys.argv[1], RDS_HOST:"myrdshost.com", RDS_DATABASE:"RouteInformation" }
    results = subject.fetch_route_information(rds_parameters, sys.argv[2])
    print(results)
