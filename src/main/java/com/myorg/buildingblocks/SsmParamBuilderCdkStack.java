package com.myorg.buildingblocks;

import static com.myorg.buildingblocks.MyCdkAppContext.extractS3Bucket;
import static com.myorg.buildingblocks.MyCdkAppContext.extractS3BaseFolder;
import static com.myorg.buildingblocks.MyCdkAppContext.extractSSMParameter;
import static com.myorg.buildingblocks.MyCdkAppContext.EXPORTED_PREXIX;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

import software.amazon.awscdk.services.ssm.*;

public class SsmParamBuilderCdkStack extends Stack {
	public SsmParamBuilderCdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }
	
	public SsmParamBuilderCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        
        final String SSM_PARAM_CDK_ID = "mycdk-ssm-param";
		final String S3_BUCKET_NAME = extractS3Bucket(this);
		final String S3_BASE_FOLDER = extractS3BaseFolder(this);
        final String SSM_PARAM_NAME = extractSSMParameter(this);

		final String S3_SCHEMA_FOLDER_PATH_WITHOUT_SLASH = String.format("s3://%s/%s", S3_BUCKET_NAME, S3_BASE_FOLDER);
        IStringParameter ssmParam = StringParameter.Builder.create(this, SSM_PARAM_CDK_ID)
        	.parameterName(SSM_PARAM_NAME)
        	.simpleName(true)
        	.type(ParameterType.STRING)
        	.description("S3 folder for Glue schema files (without ending '/')")
        	.tier(ParameterTier.STANDARD)
        	.stringValue(S3_SCHEMA_FOLDER_PATH_WITHOUT_SLASH)
        	.build();
        
        final String EXPORTED_SSM_PARAM_CDK_ID = EXPORTED_PREXIX + SSM_PARAM_CDK_ID +"-arn";
        CfnOutput exportedSSNParam = CfnOutput.Builder.create(this, EXPORTED_SSM_PARAM_CDK_ID)
        	.exportName(EXPORTED_SSM_PARAM_CDK_ID)
        	.description("ARN of SSM Param: "+ SSM_PARAM_CDK_ID)
        	.value(ssmParam.getParameterArn())
        	.build();
		System.err.println("Exported SSM param (Logial ID):" + exportedSSNParam.getLogicalId());
	}
}
