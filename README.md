# content-authorisation-proxy

Find out if a download of the digital edition is allowed. 

1) When does this user's trial expire?
Check with the Dynamo table that keeps track by calling `/auth`

Look in `DEV.public.conf` to see which table we are referring to when running locally.

```
POST /auth HTTP/1.1
Host: localhost:9300
Content-Type: application/json
Cache-Control: no-cache


{
    "appId": "<appId>",
    "deviceId": "<deviceId>"
}
```
Sample response:
```
{
  "expiry": {
    "expiryType": "free",
    "provider": "default",
    "expiryDate": "2017-05-04"
  }
}
```

2) Can the user download the digital edition because they are subscribed?
`/subs` will check with Zuora to see when the subscription expires. 
```
POST /subs HTTP/1.1
Host: localhost:9300
Content-Type: application/json
Cache-Control: no-cache

{
    "appId": <appId>,
    "deviceId": <deviceId>,
    "subscriberId": <Id from Zuora. From Zuora dev if running locally>,
    "password": <password>
}

```
Sample response:
```
{
  "expiry": {
    "expiryType": "sub",
    "expiryDate": "2018-04-19",
    "content": "SevenDay"
  }
}
```

## Running the app locally

1. Setup AWS credentials (we use the gu-membership account)

   You need membership AWS credentials from Janus. 
   
2. Download our private keys from the `subscriptions-private` S3 bucket. 

    If you have the AWS CLI set up you can run
    ```
    aws s3 cp s3://subscriptions-private/DEV/cas-proxy.private.conf /etc/gu --profile membership
    ```
3. Run `sbt` then `devrun`.  
    
## Links

* **Automated tests on Runscope:** https://www.runscope.com/radar/f862w29p8z5f/7ebb04c6-4b2d-4c25-a4d8-11d641faf2d2/overview
* **TeamCity:** https://teamcity-aws.gutools.co.uk/viewType.html?buildTypeId=memsub_subscriptions_ContentAuthorisationProxy
* **RiffRaff:** https://riffraff.gutools.co.uk/deployment/history?projectName=content-authorisation-proxy
