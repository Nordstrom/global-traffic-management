import unittest
import time
import requests
import subprocess

from create_asg import create_asgard
from create_asg import helper_get_asg_ips

max_failure_count = 20

class Test_integration_test(unittest.TestCase):
    
    @classmethod
    def setUpClass(cls):
        print("setup")
        subprocess.check_call(['./resize_asg.sh', '1', '1', 'int-test-backend-server'])
        subprocess.check_call(['./build_and_upload.sh'])

    @classmethod
    def tearDownClass(cls):
        print("teardown")
        subprocess.check_call(['./resize_asg.sh', '0', '0', 'int-test-backend-server'])
        subprocess.check_call(['./destroyNLP.sh'])

    def make_call_expecting_success(self, req):
        failure_count = 0
        result = None
        req_result = None
        while result is None: 
            try:
                print(".")
                req_result = requests.get(req, verify=False)
                if req_result.status_code == 200:
                    result = True
                    print("Received Expected 200 Response")
                else:
                    print(req_result)
                    failure_count += 1
                    if failure_count > max_failure_count:
                        print("Exausted tries %s" % (failure_count))
                        result = False
            except requests.exceptions.RequestException as e:  
                print(e)
                failure_count += 1
                if failure_count > max_failure_count:
                    print("Exhausted tries %s" % (failure_count))
                    result = False
            time.sleep(5)
        return (result, req_result)

    def make_call_expecting_not_found(self, req):
        failure_count = 0
        result = None
        while result is None: 
            try:
                print(".")
                req_result = requests.get(req, verify=False)
                if req_result.status_code != 404:
                    failure_count += 1
                    if failure_count > max_failure_count:
                        print("Exhausted tries %s" % (failure_count))
                        result = False
                else:
                    print("Received expected 404 Response")
                    result = True
            except requests.exceptions.RequestException as e:
                print(e)
                failure_count += 1
                if failure_count > max_failure_count:
                    print("Exhausted tries %s" % (failure_count))
                    result = False
            time.sleep(5)
        
        return result

    def test_creation(self):
        nlp_ips = create_asgard('kratosmac', 'int-test-nlp', './userData.sh')
        origin_server_ips = helper_get_asg_ips('int-test-backend-server')

        # this returns true once we successfully get a 200 from the nlp 
        print("Testing for new route to origin server")
        req = 'https://%s:8080/banner/whatever' % (nlp_ips[0])
        results_first = self.make_call_expecting_success(req)

        result = results_first[0]
        req_result = results_first[1]
        self.assertTrue(result)
        self.assertTrue(req_result.ok)
        headers = req_result.headers
        header_tag = headers['header-tag']
        self.assertTrue(header_tag in origin_server_ips)

        # spin down the origin servers so that the NLP will end up with empty routes
        print("Testing for lack of routes to origin server")
        subprocess.check_call(['./resize_asg.sh', '0', '0', 'int-test-backend-server'])
        # this returns true once we sucessfully get a 404 from the nlp 
        result = self.make_call_expecting_not_found(req)
        # assert that we did indeed get a 404 since the route table is empty now
        self.assertTrue(result)
        
        # spin the origin servers back up so that the NLP can see the new routes
        print("Testing for new route to origin server")
        subprocess.check_call(['./resize_asg.sh', '1', '1', 'int-test-backend-server'])
        origin_server_ips = helper_get_asg_ips('int-test-backend-server')
        results_first = self.make_call_expecting_success(req)

        result = results_first[0]
        req_result = results_first[1]
        self.assertTrue(result)
        self.assertEquals(req_result.status_code, 200)
        headers = req_result.headers
        header_tag = headers['header-tag']
        print("header_tag = %s" % (header_tag))
        print("origin ips = %s" % (origin_server_ips)) 
        self.assertTrue(header_tag in origin_server_ips)



if __name__ == '__main__':
    unittest.main()
