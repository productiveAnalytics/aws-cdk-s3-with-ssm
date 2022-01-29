package com.myorg.buildingblocks;

import static com.myorg.buildingblocks.MyCdkAppContext.extractModuleName;
import static com.myorg.buildingblocks.MyCdkAppContext.extractS3Bucket;
import static com.myorg.buildingblocks.MyCdkAppContext.extractS3BaseFolder;
import static com.myorg.buildingblocks.MyCdkAppContext.extractSSMParameter;
import static com.myorg.buildingblocks.MyCdkAppContext.EXPORTED_PREXIX;
import static com.myorg.buildingblocks.MyCdkAppContext.DEFAULT_S3_BUCKET;
import static com.myorg.buildingblocks.MyCdkAppContext.DEFAULT_S3_BASE_FOLDER;

import java.util.List;
import java.util.Set;
import java.time.Instant;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;

import org.jetbrains.annotations.NotNull;

import software.constructs.Construct;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

// CDK
import software.amazon.awscdk.services.ssm.IStringParameter;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.amazon.awscdk.services.s3.*;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;

// SDK V2
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3ReaderCdkStack extends Stack {
    private String effectiveS3Bucket;
    private String effectiveS3Folder;

    public S3ReaderCdkStack(final Construct scope, final String id) throws IOException {
        this(scope, id, null);
    }

    public S3ReaderCdkStack(final Construct scope, final String id, final StackProps props) throws IOException {
        super(scope, id, props);
        
        final String MODULE_NAME = extractModuleName(this);
        String SCHEMAS_FOLDER_S3_PATH = getSchemaFolderS3Path();

        String MY_REGION = this.getRegion();
        String CDK_REGION = props.getEnv().getRegion();
        System.err.println(String.format("MY REGION=***%s*** | CDK REGION=***%s***", MY_REGION, CDK_REGION));
        System.err.println("SCHEMAS_FOLDER_S3_PATH===*"+ SCHEMAS_FOLDER_S3_PATH +"*");

        final Region myRegion = Region.of(MY_REGION);
        S3Client s3_cli = S3Client.builder().region(myRegion).build();
        ListObjectsV2Response listObjectsV2Res = null;

        try {
            final String S3_BUCKET_NAME_FROM_CTX = extractS3Bucket(this);
            final String MODULE_FOLDER_FROM_CTX = extractS3BaseFolder(this) + "/" + MODULE_NAME;

            listObjectsV2Res = s3_cli.listObjectsV2(
                ListObjectsV2Request.builder()
                    .bucket(S3_BUCKET_NAME_FROM_CTX)
                    .prefix(MODULE_FOLDER_FROM_CTX)
                    .build()
            );

            // Save effective bucket and module-specific folder
            this.effectiveS3Bucket = S3_BUCKET_NAME_FROM_CTX;
            this.effectiveS3Folder = MODULE_FOLDER_FROM_CTX;
        }catch (Exception e) {
            System.err.println("=========");
            e.printStackTrace(System.err);
            System.err.println("=========");

            final String MODULE_FOLDER_DEFAULT = DEFAULT_S3_BASE_FOLDER + "/" + MODULE_NAME;

            System.err.println(String.format("WARN: Fallback to S3 bucket: %s and base folder: %s", DEFAULT_S3_BUCKET, DEFAULT_S3_BASE_FOLDER));
            listObjectsV2Res = s3_cli.listObjectsV2(
                ListObjectsV2Request.builder()
                    .bucket(DEFAULT_S3_BUCKET)
                    .prefix(MODULE_FOLDER_DEFAULT)
                    .build()
            );

            // Save effective bucket (as default bucket) and module-specific folder
            this.effectiveS3Bucket = DEFAULT_S3_BUCKET;
            this.effectiveS3Folder = MODULE_FOLDER_DEFAULT;
        }

        final List<S3Object> s3Objects = listObjectsV2Res.contents();

        if (s3Objects.isEmpty()) {
            throw new RuntimeException("Found empty s3 folder: "+ this.effectiveS3Folder);
        }
        
        // 777
        final Set<PosixFilePermission> fp = PosixFilePermissions.fromString("rwxrwxrwx");
        final FileAttribute<Set<PosixFilePermission>> FILE_PERM_777 = PosixFilePermissions.asFileAttribute(fp);

        Instant now = Instant.now();
        Path TEMP_FOLDER = Files.createTempDirectory("local_"+ now.toEpochMilli());
        String schemaFileName = null;
        Path tempFilePath = null;
        for (S3Object s3obj : s3Objects) {
            String[] parts = s3obj.key().split("/");
            schemaFileName = parts[parts.length - 1];

            tempFilePath = Paths.get(TEMP_FOLDER.toString(), schemaFileName);
            Files.deleteIfExists(tempFilePath); // Ensure the file does NOT exist!

            // Files.createTempFile(TEMP_FOLDER, schemaFileName, null, FILE_PERM_777);

            System.err.println("Retrieved schema file: "+ s3obj.key() +" size: "+ s3obj.size());
            download(s3_cli, s3obj.key(), tempFilePath);
        }
    }

    private String getSchemaFolderS3Path() {
        final String IMPORTED_SSM_PARAM_CDK_ID = "imported-mycdk-ssm-param";

        final String MODULE_NAME = extractModuleName(this);
        final String SSM_PARAM_NAME = extractSSMParameter(this);

        IStringParameter importedSsmParam = StringParameter.fromStringParameterName(this, IMPORTED_SSM_PARAM_CDK_ID, SSM_PARAM_NAME);
        @NotNull String s3Path = importedSsmParam.getStringValue();
     
        System.err.println("S3 Path==="+ s3Path);

        String moduleSpecificS3Path = String.format("%s/%s/", s3Path, MODULE_NAME);
        System.err.println(String.format("%s-Module-specific path (original): %s", MODULE_NAME, moduleSpecificS3Path));
        System.err.println(String.format("%s-Module-specific path (resolved): %s", MODULE_NAME, this.resolve(moduleSpecificS3Path)));

        final String EXPORTED_MODULE_SCHEMA_FOLDER_CDK_ID = String.format(EXPORTED_PREXIX+"%s-schema-folder-on-s3-arn", MODULE_NAME);
        CfnOutput exportedModuleSpecificFolder = CfnOutput.Builder.create(this, EXPORTED_MODULE_SCHEMA_FOLDER_CDK_ID)
        	.exportName(EXPORTED_MODULE_SCHEMA_FOLDER_CDK_ID)
        	.description("ARN of S3 schema folder for module: "+ MODULE_NAME)
        	.value(moduleSpecificS3Path)
        	.build();
        System.err.println("Exported folder for module (Logial ID):" + exportedModuleSpecificFolder.getLogicalId());

        IBucket bucketByArn = Bucket.fromBucketArn(this, "BucketForSchemaByArn", "arn:aws:s3:::"+ this.effectiveS3Bucket);
        final String MODULE_SPECIFIC_FOLDER = String.format("%s/%s", this.effectiveS3Folder, MODULE_NAME);
        String s3FolderPath = bucketByArn.urlForObject(MODULE_SPECIFIC_FOLDER);
        System.err.println("S3 Folder Path (original) ==="+ s3FolderPath);
        System.err.println("S3 Folder Path (resolved) ==="+ this.resolve(s3FolderPath));

        return s3FolderPath;
    }

    private void download(final S3Client s3Client, final String keyName, final Path destination) {
        GetObjectRequest getRequest = GetObjectRequest.builder().bucket(this.effectiveS3Bucket).key(keyName).build();
        s3Client.getObject(getRequest, ResponseTransformer.toFile(destination));

        GetObjectTaggingResponse s3FileTagRes = s3Client.getObjectTagging(
            GetObjectTaggingRequest.builder()
                .bucket(this.effectiveS3Bucket)
                .key(keyName)
                .build())
        ;

        System.err.println("Tags for file: "+ keyName);
        s3FileTagRes.tagSet().forEach(System.err::println);

        int fileHash = keyName.hashCode();
        final String EXPORTED_SUCCESSFUL_SCHEMA_FILE_CDK_ID = String.format(EXPORTED_PREXIX+"s3-file-%s-success", fileHash);
        CfnOutput exportedSchmeFileS3Url = CfnOutput.Builder.create(this, EXPORTED_SUCCESSFUL_SCHEMA_FILE_CDK_ID)
        	.exportName(EXPORTED_SUCCESSFUL_SCHEMA_FILE_CDK_ID)
        	.description("ARN of S3 schema file: "+ keyName)
        	.value(String.format("s3://%s/%s", this.effectiveS3Bucket, keyName))
        	.build();
        System.err.println("Exported schema file S3 URL (Logial ID):" + exportedSchmeFileS3Url.getLogicalId());
        System.err.println("---");
    }
}
