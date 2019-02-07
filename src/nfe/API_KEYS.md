# Api Keys

#### NFE includes the following gRPC functions:

* generate
* list
* revoke

Status: Actual api key persistence is *planned* to be performed by Gatekeeper via DynamoDb. Right now, api key persistence
only functions in-memory within NFE. 


#### NFE Api Key Validation:

When an incoming http request occurs, NFE checks the configured "permission" for the specified route.
If the "permission" is "apikey" then api key validation and authorization occurs. Eventually, api-key validation and 
authorization will be performed by Gatekeeper. In the interim, however, api key validation and authorization will be performed
by NFE and api keys will be stored in the NFE config.
