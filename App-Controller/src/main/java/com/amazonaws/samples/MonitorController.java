package com.amazonaws.samples;

import java.util.List;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

public class MonitorController {
	String monitorQueueURL = "https://sqs.us-east-1.amazonaws.com/386130216436/MonitorQueue";
	static private AmazonSQS sqs = new ClientService().GetSQSClient(); 
	private int maxCount = 100;
	
	protected void start(){
	    while(true) {
	    	ReceiveMessageRequest receiveMonitorMessageRequest = new ReceiveMessageRequest(monitorQueueURL);
	        List<Message> monitorMessage = sqs.receiveMessage(receiveMonitorMessageRequest.withMessageAttributeNames("All")).getMessages();
	        System.out.println();
	        if(maxCount ==0) {
	        	System.out.println("Stopping the MonitorController");
	        	break;
	        }
	        if(!monitorMessage.isEmpty()) {
		        for (Message message : monitorMessage) {
		            String instanceId = message.getMessageAttributes().get("InstanceId").getStringValue();
		            String inputString = message.getMessageAttributes().get("Input").getStringValue();
		            String inputURL = message.getMessageAttributes().get("InputUrl").getStringValue();
		            System.out.println("MonitorController InstaceId:"+inputString);
		            new InstaceTimer(instanceId,inputString,inputURL);
		        }
	            System.out.println("MonitorController Deleting a message");
	            String messageReceiptHandle = monitorMessage.get(0).getReceiptHandle();
	            sqs.deleteMessage(new DeleteMessageRequest(monitorQueueURL, messageReceiptHandle));
	        } else {
	        	System.out.println("MonitorQueue is Empty. TimeOut for 10 secs");
	        	wait(10*1000);
	        	maxCount--;
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
}
