package com.myorg;

import static com.myorg.buildingblocks.MyCdkAppContext.*;

import software.amazon.awscdk.App;
import software.amazon.awscdk.AppProps;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.myorg.buildingblocks.S3ReaderCdkStack;
import com.myorg.buildingblocks.SsmParamBuilderCdkStack;

public class MyCdkApp {
    public static void main(final String[] args) throws IOException {

        // Context enabled AppProps
    	AppProps appProps = AppProps.builder()
    			.analyticsReporting(true)
    			.stackTraces(true)
    			.context(buildContext())
    			.build();
    	
        App app = new App(appProps);
        
        final StackProps stackProps = StackProps.builder()
                // If you don't specify 'env', this stack will be environment-agnostic.
                // Account/Region-dependent features and context lookups will not work,
                // but a single synthesized template can be deployed anywhere.
                .env(Environment.builder()
                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                        .region(System.getenv("CDK_DEFAULT_REGION"))
                        .build())
                .build();
        
        Stack ssmParamBuilder = new SsmParamBuilderCdkStack(app, "MyCdkSSMParamBuilderStack", stackProps);

        Stack s3Reader = new S3ReaderCdkStack(app, "MyCdkS3ReaderStack", stackProps);
        s3Reader.getNode().addDependency(ssmParamBuilder);

        app.synth();
    }
    
    private static Map<String,String> buildContext() {
        Map<String, String> contextMap = new HashMap<>();

        // TODO: extract actual value from env-specific property file
        
        contextMap.put(CONTEXT_KEY_S3_BUCKET,           "laap-ue1-gen-repository-sbx");
        contextMap.put(CONTEXT_KEY_S3_BASE_FOLDER,      "avro_2_glue/output");
        contextMap.put(CONTEXT_KEY_MODULE_NAME,         "con-cust");
        contextMap.put(CONTEXT_KEY_SSM_PARAM_NAME,      "laap-ssm-ue1-cdk-schemas-s3-path");

    	return contextMap;
    }
}

