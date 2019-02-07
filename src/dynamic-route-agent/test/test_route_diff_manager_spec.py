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

import subprocess
import json
import sys
import filecmp
from dra.route_diff_manager import Route_Diff_Manager

class Test_route_diff_manager:

    def test_are_routes_different____when_existing_route_conf_is_empty(self, testdata_dir):
        reference_route_conf = testdata_dir + "/new_route.conf"
        f = open(reference_route_conf)
        new_route_string = f.read()
        f.close()
        empty_conf_file = testdata_dir + "/empty_route.conf"
        subject = Route_Diff_Manager()
        result = subject.are_routes_different(None, new_route_string, empty_conf_file)
        assert result == True

    def test_are_routes_different____when_existing_route_conf_is_different(self, testdata_dir):
        reference_route_conf = testdata_dir + "/new_route.conf"
        f = open(reference_route_conf)
        new_route_string = f.read()
        f.close()
        different_conf_file = testdata_dir + "/different_route.conf"
        subject = Route_Diff_Manager()
        result = subject.are_routes_different(None, new_route_string, different_conf_file)
        assert result == True

    def test_are_routes_different____when_existing_route_conf_is_the_same(self, testdata_dir):
        reference_route_conf = testdata_dir + "/new_route.conf"
        f = open(reference_route_conf)
        new_route_string = f.read()
        f.close()
        same_conf_file = testdata_dir + "/same_route.conf"
        subject = Route_Diff_Manager()
        result = subject.are_routes_different(None, new_route_string, same_conf_file)
        assert result == False
