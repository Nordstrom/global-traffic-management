if you are using a virtual env you can just use the word pip and python  
step 1) install python 3 (sudo apt-get install python3)  
step 2) install pip3 (sudo apt-get install python3-pip)  
step 3) pip3 install -r requirements.txt (you made need to sudo this)  
step 4) ./build_distribution_package.sh (this will output a dynamicrouteagent.pex executable)  
step 5) ./run.sh  
step 6) profit  
step 7) optionally, you can upload the pex to s3 using ./upload_pex_to_s3.sh  

--------------------------------------------------------
How to run the main of the sources files individually  
step 1) python -m dra.<file_that_has_a_main_you_want_to_run>  

### What is the dynamic route agent
The dynamic route agent is a sidecar python application that can create/update the config file
that the NLP uses to update it's routing configuration. The NLP will periodically check the config file outputted
by the agent. If there is a change in the content of the config file, the NLP will update it's routing configuration
accordingly.

### How does it work
The dynamic route agent has two modes of operation.  
1. Introspect mode  
2. Non-Introspect mode  

In Introspect mode the dynamic route agent will interrogate the Cloud Account it is running in. It will then will then use that information to do a database lookup. This database lookup will return the relevant application deployment information associated with the Cloud Account The information returned by the database will include information such as tag_key, tag_value, route name, relative path, port number, and whether or not TLS is enabled for that deployed application.  This information is then used to fetch the ip addresses associated with that tagged/named route. Once the ip addresses are fetched, the agent will build up a route json that merges the route information with the ip addresses associated with the route. This is the raw information that the NLP uses to construct it's routing table.  

In Non-Introspect mode the dynamic route agent will intake a route_parameter_file that has the same information that would have been provided by the database call described previously.  Non-Introspect mode allows the user to use the tag key/value to search for tagged_autoscalinggroups, tagged_ec2_instances, or simply the name of the autoscaling groups.  
* When using tagged_asg or tagged_ec2 the tag_key/tag_value dictionary key/value pairs are the values required  
* When using named_asg the asg_named_value dictionary key/value pair is used.  This mode is depricated.  

### There are two ways run the agent  
1. Using a local route_parameter.json file  
   dynamicrouteagent.pex --introspect False --route_conf_path routes.json --route_parameter_file route_parameters.json  
   * routes.json is the output configuration file that we feed into the NLP  
   * route_parameters.json lets us know know aht autoscaling group tags to use and what configurations the application endpoints are  

2. Using the introspection to and database look up  
   dynamicrouteagent.pex --introspect True --route_conf_path routes.json  --rds_user <rds user> --rds_password <rds password> --rds_host <host of rds> --rds_database <rds database>  

## Valid Modes in route_parameter entry  
* "mode": "tagged_asg"
* "mode": "tagged_ec2"
* "mode": "named_asg"
* "mode": "ip_address_list"


### The contents of the rds_config_file looks like this
```
{  
  "rds_user": "my_rds_user",  
  "rds_password": "my_rds_password",  
  "rds_host": "my_rds_host",  
  "rds_database": "my_rds_database"  
}  
```

### The contents of the route_conf_path file looks like this
```
[  
  {  
    "mode": "tagged_asg",  
    "route_parameter_id": 3,  
    "organizational_unit_id": 2,  
    "tag_key": "not-gtm-tag-key",  
    "tag_value": "not-gtm-tag-value",  
    "name": "not-gtm-name",  
    "path": "/not-gtm-path/",  
    "client_name": "not-gtm-client-name",  
    "port_number": 1234,  
    "tls_enabled": 1,  
    "ip_addresses": [],  
    "account_id": "1234",  
    "ou": "not-gtm"  
  }  
]  
```

### The contents of the route_parameter_file looks like this
```
[  
  {  
      "mode": "tagged_asg",  
      "tag_key": "Name",  
      "tag_value": "GTM-NLP-234460336057-nonprod",  
      "name": "route1",  
      "path": "/path1/",  
      "client_name": "client1",  
      "port_number": 1234,  
      "tls_enabled": false  
  }  
]  
```
