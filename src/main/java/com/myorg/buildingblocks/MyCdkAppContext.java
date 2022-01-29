package com.myorg.buildingblocks;

import software.constructs.Construct;

public final class MyCdkAppContext {
    public static final String EXPORTED_PREXIX = "exported-";   // To easily track exported Cfn Outputs

    public static final String DEFAULT_S3_BUCKET        = "laap-ue1-gen-repository-sbx"; 
    public static final String DEFAULT_S3_BASE_FOLDER   = "avro_2_glue/output";

    public static final String CONTEXT_KEY_S3_BUCKET        = "s3_bucket";
    public static final String CONTEXT_KEY_S3_BASE_FOLDER   = "s3_base_folder";
    public static final String CONTEXT_KEY_MODULE_NAME      = "module_name";
    public static final String CONTEXT_KEY_SSM_PARAM_NAME   = "ssm_param_name";

    private MyCdkAppContext() {}

    public static String extractS3Bucket(final Construct c) {
        return (String) c.getNode().tryGetContext(CONTEXT_KEY_S3_BUCKET);
    }

    public static String extractS3BaseFolder(final Construct c) {
        return (String) c.getNode().tryGetContext(CONTEXT_KEY_S3_BASE_FOLDER);
    }

    public static String extractModuleName(final Construct c) {
        return (String) c.getNode().tryGetContext(CONTEXT_KEY_MODULE_NAME);
    }

    public static String extractSSMParameter(final Construct c) {
        return (String) c.getNode().tryGetContext(CONTEXT_KEY_SSM_PARAM_NAME);
    }
}
