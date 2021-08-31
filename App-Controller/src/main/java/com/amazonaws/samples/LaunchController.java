package com.amazonaws.samples;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.QueueAttributeName;

public class LaunchController {
	String launchQueueUrl = "https://sqs.us-east-1.amazonaws.com/386130216436/LaunchQueue";
    String monitorQueueUrl = "https://sqs.us-east-1.amazonaws.com/386130216436/MonitorQueue"; //sqs.getQueueUrl("MonitorQueue").getQueueUrl();
	static private AmazonSQS sqs = new ClientService().GetSQSClient();
	static private AmazonEC2 ec2 = new ClientService().GetEC2Client();

	
   	private QueueAttributeName[] messageCountAttrs = {
            QueueAttributeName.ApproximateNumberOfMessages,
            QueueAttributeName.ApproximateNumberOfMessagesDelayed,
            QueueAttributeName.ApproximateNumberOfMessagesNotVisible
    };
   	
   	GetQueueAttributesRequest launchQueueRequest = null;
   	GetQueueAttributesRequest monitorQueueRequest = null;
   	
	private int maxCount = 20;
	
	public LaunchController(){
        launchQueueRequest = 
        		new GetQueueAttributesRequest()
                .withQueueUrl(launchQueueUrl)
                .withAttributeNames(messageCountAttrs);
        
        monitorQueueRequest = 
        		new GetQueueAttributesRequest()
                .withQueueUrl(monitorQueueUrl)
                .withAttributeNames(messageCountAttrs);
    	
	}

	public void start(){
		int countOfInstancesRunandPend = 0;
		int maxNumberOfInstances = 18;
        while(true) {
            countOfInstancesRunandPend = countOfRunningAndPendingInstances() - 1; // -2 for webTier and controller instance state
	        GetQueueAttributesResult launchQueueResult = sqs.getQueueAttributes(launchQueueRequest);
	        Map<String, String> launchQueueAttr = launchQueueResult.getAttributes();
	        System.out.println("Launch Queue Attributes"+ launchQueueAttr);
	        
	        GetQueueAttributesResult monitorQueueResult = sqs.getQueueAttributes(monitorQueueRequest);  
	        Map<String, String> monitorQueueAttr = monitorQueueResult.getAttributes();
	        System.out.println("Monitor Queue Attributes"+ monitorQueueAttr);		        
	        System.out.println("Number of Pending and Runnnig instances is "+ countOfInstancesRunandPend);	
	        int que_diff = Integer.parseInt(launchQueueAttr.get("ApproximateNumberOfMessages")) - Integer.parseInt(monitorQueueAttr.get("ApproximateNumberOfMessages"));
	        // int que_diff = Integer.parseInt(launchQueueAttr.get("ApproximateNumberOfMessages")) - countOfInstancesRunandPend;
	        System.out.println("Queue Difference is "+ que_diff);
	        if(que_diff > 0 && countOfInstancesRunandPend < maxNumberOfInstances) {

	        	// Create multiple EC2 instances
	        	int freeInstances = maxNumberOfInstances - countOfInstancesRunandPend;
	        	que_diff = que_diff > freeInstances ? freeInstances : que_diff;
	        	System.out.println("Create Ec2 instances:"+que_diff);
	        	System.out.println("Timeout of 1 min for LaunchController");
	        	new InstanceService().createMultipleInstances(ec2, que_diff);
	        	wait(1*60*1000);
	        	maxCount--;
	        	
	        } else {
	        	System.out.println("Timeout of 20 secs for LaunchController "+que_diff);
	        	wait(20*1000);
	        	maxCount--;
	        }
	        System.out.println("------------------------------------------------------------------------------------------");
	        if(maxCount == 0) {
	        	System.out.println("Stopping the LaunchController");
	        	break;
	        }
        }
	}
	
	public static void wait(int ms)
	{
	    try
	    {
	        Thread.sleep(ms);
	    }
	    catch(InterruptedException ex)
	    {
	        Thread.currentThread().interrupt();
	    }
	}

	// Status Code and it's Name
	//	0 : pending
	//	16 : running
	//	32 : shutting-down
	//	48 : terminated
	//	64 : stopping
	//	80 : stopped
	
	private int countOfRunningAndPendingInstances() {
		DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
		DescribeInstancesResult response = ec2.describeInstances(describeInstancesRequest);
    	List<Reservation> reservations = response.getReservations();
    	int  totalCount = 0;
    	for(Reservation reservation: reservations ) {
    		int state_code = reservation.getInstances().get(0).getState().getCode();
    		if(state_code == 0 || state_code == 16  ) {
    			totalCount ++; 
    		}
    	}
    	return totalCount;
	}
}
