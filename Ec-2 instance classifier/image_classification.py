import torch
import torchvision.transforms as transforms
import torchvision.models as models
from PIL import Image
import json
import sys
import numpy as np
import boto3
import os
from botocore.exceptions import ClientError
import time
import urllib.request
from urllib.request import urlopen

input_string = ''
aws_access_key_id ='AKIATSRIJ7WFBC7EBCPG'
aws_secret_access_key = '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'
region_name = 'us-east-1'

endpoint_url='https://sqs.us-east-1.amazonaws.com'

monitor_queue_url = 'https://sqs.us-east-1.amazonaws.com/245971418506/MonitorQueue'
output_que_url = 'https://sqs.us-east-1.amazonaws.com/245971418506/OutputQueue'
launch_que_url = 'https://sqs.us-east-1.amazonaws.com/245971418506/Launch-Queue'

ec2 = boto3.client('ec2', aws_access_key_id= aws_access_key_id, aws_secret_access_key=aws_secret_access_key, region_name=region_name)
sqs = boto3.client('sqs', aws_access_key_id= aws_access_key_id, aws_secret_access_key=aws_secret_access_key, endpoint_url=endpoint_url, region_name=region_name)
s3  = boto3.resource('s3',aws_access_key_id= aws_access_key_id, aws_secret_access_key=aws_secret_access_key, region_name=region_name)
s3_image = boto3.client('s3',aws_access_key_id= aws_access_key_id, aws_secret_access_key=aws_secret_access_key, region_name=region_name)

instance_id = urllib.request.urlopen('http://169.254.169.254/latest/meta-data/instance-id').read().decode()

while(1):
# Fetch images from Launch Queue for processing
    response = sqs.receive_message(
    QueueUrl=launch_que_url,
    AttributeNames=[
        'SentTimestamp'
    ],
    MaxNumberOfMessages=1,
    MessageAttributeNames=[
        'All'
    ],
    VisibilityTimeout=30,
    WaitTimeSeconds=0
    )
    if (response.get('Messages')):
        message = response['Messages'][0]
        receipt_handle = message['ReceiptHandle']
        input_url = message['MessageAttributes']['InputUrl']['StringValue']
        input_string = message['MessageAttributes']['Input']['StringValue']
        retry_count = message['MessageAttributes']['RetryCount']['StringValue']
# To count the number of times same image has been classified
# Used for error check of images
        retryCount = int(retry_count) + 1

        sqs.delete_message(
            QueueUrl=launch_que_url,
            ReceiptHandle=receipt_handle
        )

        print('Received and deleted message input_url :',input_url)
        print('Received and deleted message input_string:',input_string)

# Send message to MonitorQueue with Status for Ec2 instance as Running
        resp = sqs.send_message(
            QueueUrl=monitor_queue_url,
            MessageBody=(input_url),
                MessageAttributes={
                    'InstanceId': {
                        'StringValue': instance_id,
                        'DataType': 'String'
                    },  
                    'Status':{
                        'StringValue':'Running',
                        'DataType':'String'
                    },
                    'Input':{
                        'StringValue':input_string,
                        'DataType':'String'
                    },
                    'InputUrl':{
                        'StringValue': input_url,
                        'DataType':'String'
                    },
                    'RetryCount':{
                        'StringValue': str(retryCount),
                        'DataType':'String'
                    }
                    
                },
        )

        print('Message delivered to monitor queue with messageId:',resp['MessageId'])
        print('Instance ID:', instance_id)

# Fetch Image from S3 bucket to process it

        image = input_string
        bucket = 'input-image'
        with open(image, 'wb') as data:
            s3_image.download_fileobj(bucket,image, data)

# Image Classifier logic

        print('Classifier started runnning') 
        path= "/home/ubuntu/classifier/" + image
        img = Image.open(path)
        model = models.resnet18(pretrained=True)
        model.eval()
        img_tensor = transforms.ToTensor()(img).unsqueeze_(0)
        outputs = model(img_tensor)
        _, predicted = torch.max(outputs.data, 1)

        with open('./imagenet-labels.json') as f:
            labels = json.load(f)
        output_result = labels[np.array(predicted)[0]]
        print(f"{output_result}")
        os.remove(path)

# Send output to OutputQueue
        resp = sqs.send_message(
            QueueUrl=output_que_url,
            MessageBody=(output_result),
                MessageAttributes={
                    'InstanceId': {
                        'StringValue': instance_id,
                        'DataType': 'String'
                    },
                    'Status':{
                        'StringValue':'Completed',
                        'DataType':'String'
                    },
                    'Input':{
                        'StringValue':input_string,
                        'DataType':'String'
                    },
                    'InputUrl':{
                        'StringValue': input_url,
                        'DataType':'String'
                    },
                    'Output':{
                        'StringValue': output_result,
                        'DataType':'String'
                    },
                },

        )

        input_string = os.path.splitext(input_string)[0]
        # Output result to S3 object
        s3.Object('output-classifier-result2', input_string).put(Body=output_result)
        print('Message delivered to output queue with messageId:',resp['MessageId'])
    else:
        print("Empty Launch Que Terminating")
        break

# Call terminate_instances after launch queue is empty
try:
    response = ec2.terminate_instances(InstanceIds=[instance_id], DryRun=False)
    print(response)
except ClientError as e:
    print(e)

