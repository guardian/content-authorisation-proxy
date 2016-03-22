# content-authorisation-proxy
This proxies requests to the content authorisation services (CAS) to allow a new content subscription provider to be added

* **TeamCity:** https://teamcity.gutools.co.uk/viewType.html?buildTypeId=Subscriptions_ContentAuthorisationProxy
* **RiffRaff:** https://riffraff.gutools.co.uk/deployment/history?projectName=content-authorisation-proxy

## General Setup

1. Setup AWS credentials (we use the gu-membership account)

   Ask your teammate to create an account for you and securely send you the access key. For security, you must enable [MFA](http://aws.amazon.com/iam/details/mfa/).

   In `~/.aws/credentials` add the following:

   ```
   [membership]
   aws_access_key_id=[YOUR_AWS_ACCESS_KEY]
   aws_secret_access_key=[YOUR_AWS_SECRET_ACCESS_KEY]
   ```

   In `~/.aws/config` add the following:

   ```
   [default]
   output = json
   region = eu-west-1
   ```

1. Download our private keys from the `subscriptions-private` S3 bucket. You will need an AWS account so ask another dev.

    If you have the AWS CLI set up you can run
    ```
    aws s3 cp s3://subscriptions-private/DEV/cas-proxy.conf /etc/gu --profile membership

    ```
    
## Automated tests

https://www.runscope.com/radar/f862w29p8z5f/7ebb04c6-4b2d-4c25-a4d8-11d641faf2d2/overview
