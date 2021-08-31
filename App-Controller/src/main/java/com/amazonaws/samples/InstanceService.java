package com.amazonaws.samples;

import java.util.Base64;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

public class InstanceService {
	static String user_data = "#cloud-boothook\n"
			+ "#!/bin/bash\n"
			+ "cd /home/ubuntu/classifier\n"
			+ "chmod +x image_classification.py\n"
			+ "python3 image_classification.py";
	static String user_data_encoded = Base64.getEncoder().encodeToString(user_data.getBytes());
	RunInstancesRequest runInstancesRequest =  new RunInstancesRequest().withImageId("ami-0752c1c2fc1e4e9b8")
            .withInstanceType("t2.micro")
            .withMinCount(1)
            .withMaxCount(1)
            .withKeyName("Project_1")
            .withSecurityGroupIds("sg-0b25e3b93c58f7bad")
            .withUserData(user_data_encoded);	
	static int count = 1 ;

 
	protected void TerminateInstance(String instanceID, AmazonEC2 ec2Client) {
        TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest()
                .withInstanceIds(instanceID);
        ec2Client.terminateInstances(terminateInstancesRequest)
                .getTerminatingInstances()
                .get(0)
                .getPreviousState()
                .getName();
        System.out.println("Instance is Terminated with id: "+instanceID);
	}
	
	protected void createInstance(AmazonEC2 ec2Client) {
		RunInstancesResult runInstancesResult = ec2Client.runInstances(runInstancesRequest);
		Instance instance = runInstancesResult.getReservation().getInstances().get(0);
		System.out.println(count + "Instance is Created with id: "+instance.getInstanceId());
		count++;
    }
	
	protected void createMultipleInstances(AmazonEC2 ec2Client, int numberOfInstances) {
		for(int instance= 0;instance<numberOfInstances;instance++) {
			createInstance(ec2Client);
		}
	}
}
