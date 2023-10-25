package com.myorg;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.IResolvable;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.Asset;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.BundlingOutput;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.DockerImage;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.ILocalBundling;
import software.amazon.awscdk.IResolvable;
import software.amazon.awscdk.PhysicalName;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.lambda.AssetCode;
import software.amazon.awscdk.services.lambda.CfnFunction;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Permission;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.assets.Asset;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.CacheControl;
import software.amazon.awscdk.services.s3.deployment.ISource;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.amazon.awscdk.services.apigateway.SpecRestApi;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.cloudfront.Behavior;
import software.amazon.awscdk.services.cloudfront.CloudFrontWebDistribution;
import software.amazon.awscdk.services.cloudfront.OriginAccessIdentity;
import software.amazon.awscdk.services.cloudfront.S3OriginConfig;
import software.amazon.awscdk.services.cloudfront.SourceConfiguration;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.apigateway.AssetApiDefinition;
import software.amazon.awscdk.services.apigateway.InlineApiDefinition;

public class InfraStack extends Stack {

    private CfnOutput restIdOutput;

    private CfnOutput bucketNameOutput;

    private CfnOutput cloudFrontURLOutput;

    private CfnOutput cloudFrontDistributionIdOutput;

    public CfnOutput getRestIdOutput() {
        return restIdOutput;
    }

    public CfnOutput getBucketNameOutput() {
        return bucketNameOutput;
    }

    public CfnOutput getCloudFrontURLOutput() {
        return cloudFrontURLOutput;
    }

    public CfnOutput getCloudFrontDistributionIdOutput() {
        return cloudFrontDistributionIdOutput;
    }

    public InfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // The code that defines your stack goes here

        Asset openAPIAsset = Asset.Builder.create(this, "OpenAPIDishAsset")
                .path("../api/swagger-apigateway.json").build();

        Map<String, String> transformMap = new HashMap<String, String>();
        transformMap.put("Location", openAPIAsset.getS3ObjectUrl());
        // Include the OpenAPI template as part of the API Definition supplied to API
        // Gateway
        IResolvable data = Fn.transform("AWS::Include", transformMap);

        InlineApiDefinition apiDefinition = AssetApiDefinition.fromInline(data);

        SpecRestApi restAPI = SpecRestApi.Builder.create(this, "LunchMenuRestAPI")
                .apiDefinition(apiDefinition)
                .restApiName("LunchMenuWidgetAPI")
                .endpointExportName("LunchMenuWidgetRestApiEndpoint")
                .deployOptions(StageOptions.builder().stageName("test").build())
                .deploy(true)
                .build();

        restIdOutput = CfnOutput.Builder.create(this, "LunchMenuAPIRestIdOutput")
                .value(restAPI.getRestApiId())
                .build();


        List<String> functionOnePackagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "cd FunctionOne " +
                        "&& mvn clean install " +
                        "&& cp /asset-input/FunctionOne/target/functionone.jar /asset-output/"
        );

        List<String> functionTwoPackagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                "cd FunctionTwo " +
                        "&& mvn clean install " +
                        "&& cp /asset-input/FunctionTwo/target/functiontwo.jar /asset-output/"
        );

        BundlingOptions.Builder builderOptions = BundlingOptions.builder()
                .command(functionOnePackagingInstructions)
                .image(Runtime.JAVA_11.getBundlingImage())
                .volumes(singletonList(
                        // Mount local .m2 repo to avoid download all the dependencies again inside the container
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()
                ))
                .user("root")
                .outputType(ARCHIVED);

        Function functionOne = new Function(this, "FunctionOne", FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../software/", AssetOptions.builder()
                        .bundling(builderOptions
                                .command(functionOnePackagingInstructions)
                                .build())
                        .build()))
                .handler("helloworld.App")
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .logRetention(RetentionDays.ONE_WEEK)
                .build());

        Function functionTwo = new Function(this, "FunctionTwo", FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../software/", AssetOptions.builder()
                        .bundling(builderOptions
                                .command(functionTwoPackagingInstructions)
                                .build())
                        .build()))
                .handler("helloworld.App")
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .logRetention(RetentionDays.ONE_WEEK)
                .build());



    }
}
