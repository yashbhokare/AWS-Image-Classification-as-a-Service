package com.amazonaws.samples;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class InstaceTimer {
	private Timer timer;
	static private AmazonEC2 ec2= new ClientService().GetEC2Client();
	static private AmazonS3 s3 = new ClientService().GetS3Client(); 
	static private AmazonSQS sqs = new ClientService().GetSQSClient(); 
	static private InstanceService instanceService = new InstanceService();
	static private String bucketName = "output-classifier-result";
	private String launchQueURL = "https://sqs.us-east-1.amazonaws.com/386130216436/LaunchQueue";
	
	public InstaceTimer(String instanceId,String inputValue,String inputURL) {
		timer = new Timer();
		timer.schedule(new RunTask(instanceId, inputValue,inputURL),60*1000); // Timer of 60 seconds
	}
	
	public class RunTask extends TimerTask {
		String instaceId,inputValue,inputURL = null;
		 
		public RunTask(String id, String ipValue,String ipURL) {
			 instaceId = id;
			 inputValue = ipValue;
			 inputURL = ipURL;
		 }
		 
		 public void run() {
			System.out.println("\nTime's up for "+inputValue);
			timer.cancel(); //Terminate the timer thread
			String filePath = inputValue+".txt";
			boolean isValidfile = isValidFile(bucketName,filePath);
			if(isValidfile) {
				System.out.println("File exists in Output Bucket:  "+filePath);
			} else {
				System.out.println("File does not exist in Output Bucket: "+filePath);
				instanceService.TerminateInstance(instaceId, ec2);
				sendMessageToLaunchQueue(inputValue,inputURL);
			}
		}
	}
	
	private boolean isValidFile(
	        String bucketName,
	        String path) throws AmazonClientException, AmazonServiceException {
	    boolean isValidFile = true;
	    try {
	        s3.getObjectMetadata(bucketName, path);
	    } catch (AmazonS3Exception s3e) {
	        if (s3e.getStatusCode() == 404) {
	        // i.e. 404: NoSuchKey - The specified key does not exist
	            isValidFile = false;
	        }
	        else {
	            throw s3e;    // re-throw all S3 exceptions other than 404   
	        }
	    }
	    return isValidFile;
	}
	
	private void sendMessageToLaunchQueue(String inputValue, String inputURL) {
		final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		messageAttributes.put("Input", new MessageAttributeValue()
			.withDataType("String")
			.withStringValue(inputValue));
		messageAttributes.put("InputUrl", new MessageAttributeValue()
				.withDataType("String")
				.withStringValue(inputURL));
		final SendMessageRequest sendMessageRequest = new SendMessageRequest();
		sendMessageRequest.withMessageBody(inputValue);
		sendMessageRequest.withQueueUrl(launchQueURL);
		sendMessageRequest.withMessageAttributes(messageAttributes);
		sqs.sendMessage(sendMessageRequest);	
	}
	
}

