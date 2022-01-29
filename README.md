# CDK with SSM Parameter and reading files from S3

## One-time setup for AWS CDK project
1. Login to aws:
```
npm install -g aws-saml-auth
aws-saml-auth (follow interactive widget to login thru SSO)
aws sts get-caller-identity
```
2. Install aws CDK:
```
npm install -g aws-cdk
cdk --version
```
3. Initialize starter CDK project, in the language of your choice (Default: TypeScript):
```
cdk init --language java
```

The `cdk.json` file has information like CDK's qualifier. It also instructs the CDK Toolkit how to execute your app.

## Useful commands

 * `mvn package`     compile and run tests
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk diff`        compare deployed stack with current state
 * `cdk deploy`      deploy this stack to your default AWS account/region

## [DEBUG] Download Glue Schema (CSV) files from S3
```
aws s3 cp --recursive <s3-path-for-folder-conataining-output-of-AVRO-2-Glue-lambda> ./src/main/resources/cdk_test/
```

## Build project (skipping tests for speedy build)
```
mvn clean package -DskipTests
(or for local): mvn --offline clean install -DskipTests
```

## Ensure that environment variables are set for CDK e.g. CDK_MODULE_NAME that refers to Linear module.
Note: The qualifier MUST match with that specified in `cdk.json`
```
export CDK_NEW_BOOTSTRAP=1
export CDK_DEBUG=true
export CDK_DEFAULT_ACCOUNT="$(aws sts get-caller-identity --query Account --output text)"
export CDK_DEFAULT_REGION=<aws_region_id-e.g.us-east-1>
export CDK_QUALIFIER="datasync03"
export CDK_MODULE_NAME="con-common"
```

Run following command to ensure environment variables are set for CDK
```
cdk doctor
```

## Bootstrap CDK for AWS account and Region (Note: qualifier should match with qualifier specified in cdk.json)
```
cdk bootstrap aws://${CDK_DEFAULT_ACCOUNT}/${CDK_DEFAULT_REGION} \
--profile default \
--trust 887847050650 --trust-for-lookup 887847050650 \
--cloudformation-execution-policies arn:aws:iam::887847050650:policy/dtci-admin \
--qualifier datasync03 \
--toolkit-stack-name cdk-toolkit-s3-with-ssm-datasync03-stack \
--tags author=LDC --tags usage=cdk --tags module=con-cust --tags qualifier=datasync03 \
--force
```

Check the CDK bootstrapping template:
```
cdk bootstrap \
--toolkit-stack-name cdk-toolkit-s3-with-ssm-datasync03-stack  \
--show-template  > ./bootstrap_template_datasync03.yaml
```

## Debug CDK stack
Check stack(s) in this CDK application:
```
cdk \
--toolkit-stack-name cdk-toolkit-s3-with-ssm-datasync03-stack \
ls
```
Ouput would be, like:
* MyCdkSSMParamBuilderStack
* MyCdkS3ReaderStack

Check the change-set:
```
cdk \
--toolkit-stack-name cdk-toolkit-s3-with-ssm-datasync03-stack \
--staging \
diff
```

## Deploy CDK stack along with new Glue Database
```
cdk \
--toolkit-stack-name cdk-toolkit-s3-with-ssm-datasync03-stack \
--staging \
--require-approval never \
--progress events \
deploy --all
```


----

## To manually delete stacks:
1. Disable termination protection for stack
```
aws cloudformation update-termination-protection \
--no-enable-termination-protection \
--stack-name datasync-con-cust-base-stack-sbx
```
2. Destroy full-stack:
```
cdk \
--toolkit-stack-name cdk-toolkit-s3-with-ssm-datasync03-stack \
destroy --all
```