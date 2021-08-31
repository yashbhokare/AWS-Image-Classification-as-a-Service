package com.amazonaws.samples;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

public class ClientService {
	static private AmazonEC2 ec2Client = null;
	static private AmazonSQS sqsClient = null;
	static private AmazonS3 s3Client = null;
    // Fetch Authentication Credentials
    private  ProfileCredentialsProvider getAuthCredentials() {    	
        ProfileCredentialsProvider profileCredentials = new ProfileCredentialsProvider();        
        try {
        	profileCredentials.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (C:\\Users\\yash4\\.aws\\credentials), and is in valid format.",
                    e);
        }
        return profileCredentials;
    }
    
    protected void initialize() {
    	ec2Client = null;
    	sqsClient = null;
    	s3Client = null;
    }
    
    // Creates an EC2 Client 
    protected  AmazonEC2 CreateEC2Client() {
    	ec2Client = AmazonEC2ClientBuilder
    			.standard()
        		.withCredentials(getAuthCredentials())
        		.withRegion(Regions.US_EAST_1)
        		.build();
    	return ec2Client;
    }
    
    // Creates a SQS Client 
    protected  AmazonSQS CreateSQSClient() {
    	sqsClient = AmazonSQSClientBuilder
    			.standard()
                .withCredentials(getAuthCredentials())
                .withRegion(Regions.US_EAST_1)
                .build();
    	return sqsClient;
    }
    
    // Creates a S3 Client 
    protected AmazonS3 CreateS3Client() {
    	s3Client = AmazonS3ClientBuilder
			  .standard()
			  .withCredentials(getAuthCredentials())
			  .withRegion(Regions.US_EAST_1)
			  .build();
    	return s3Client;
    }
    
    protected AmazonEC2 GetEC2Client() {
    	if(ec2Client != null) {
    		return ec2Client;
    	} else {
    		return CreateEC2Client();
    	}
    }
    
    protected AmazonSQS GetSQSClient() {
    	if(sqsClient != null) {
    		return sqsClient;
    	} else {
    		return CreateSQSClient();
    	}
    }
    
    protected AmazonS3 GetS3Client() {
    	if(s3Client != null) {
    		return s3Client;
    	} else {
    		return CreateS3Client();
    	}
    }
    
}
