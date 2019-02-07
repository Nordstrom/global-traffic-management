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

import os.path
import pytest
import sys


PROJECT_ROOT = os.path.abspath(os.path.join(
    os.path.dirname(__file__),
    '..',
))


# allow testing from a source checkout as well as an installation (e.g. tox)
try:
    import dra
    del dra
except ImportError:
    # assume that the test targets are in the parent directory
    sys.path.insert(0, PROJECT_ROOT)
    import dra

@pytest.fixture
def testdata_dir():
    return os.path.abspath(
            os.path.join(
                PROJECT_ROOT,
                'test',
                'testdata',
                )
            )
