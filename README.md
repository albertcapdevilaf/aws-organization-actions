# aws-organization-actions

This solution deploys a lambda function that deletes specific services in all the accounts of an Organization that follow a specific prefix pattern.
Currently it disables Security Hub and Guard Duty, but could be extended to also disable or delete other resources in the lambda function.
The naming also permits to be extended to apply additional actions affecting multiple accounts, by creating other lambda functions.

## Solution architecture

The solution consists in a singe lambda function (organizationActionsNukeAccounts), without any public interface to call it for security reasons.

## Deploying the solution

This project is built using the AWS Serverless Application Model (SAM), a framework extending AWS CloudFormation syntax to easily define serverless components such as AWS Lambda functions or Amazon DynamoDB tables. It leverages [AWS toolkit for Eclipse] (https://aws.amazon.com/es/blogs/developer/aws-toolkit-for-eclipse-serverless-application/) to build serverless applications based on SAM and Maven.

The following resources act as a pre-requisite:
* Eclipse with AWS toolkit for Eclipse and Maven.
* AWs Organizations activated in the account where the solution will be deployed.
* User in the AWS master account and Eclipse configured to use it in the AWS toolkit.
* Role created in the master account with 'lambda.amazonaws.com' as a trusted entity with a policy granting the permissions present on lambdaPolicyExample.json file.
* An Amazon S3 bucket to store the artifact that will be deployed.

The project includes a single J-Unit test that needs to be modified so the assert is successful, permitting the artifact to be compiled:
* Open NukeAccountsTest.java
* Change the `prefix` property in the `CreateInput` method so it matches a single account in your Organization (right now it has the value `Workshop40`)

To build the solution, run the following in your maven environment `mvn clean package`
A new jar file will be created in the target folder with a name similar to 'organizationActions-1.0.0.jar'

* Right click in the Eclipse project an run 'Amazon Web Services' --> 'Deploy serverless Project'
* Select the desired AWS region, a name to identify the Cloudformation stack that will be created, and check the button "CAPABILITY_IAM"
* Specify the ARN of the role specified as a pre-requisite.

### Template parameters
* `RoleARN`: ARN of the IAM role the lambda function will use.

### Template output
* None.

## How to use

You can launch the lambda function from the AWS console or from your Eclipse environment.
In `exampleRequest.json` you have an example of the invocation parameters:
* `region`: AWS region where the nuke action will be executed.
* `prefix`: Account name prefix to filter the accounts where the nuke will be executed
* `skipGuardDuty`: `true` if you don't want GuardDuty to be disabled in the accounts.
* `skipSecHub`: `true` if you don't want Security Hub to be disabled in the accounts.

The lambda funcion will list all the accounts in AWS Organizations matching the prefix and will disable GuardDuty and Security Hub in the specified region for those accounts. Logs are generated indicating the actions done by the lambda function.
