# Image-Classification-as-a-Service


Problem statement
The aim of this project is to build an elastic application that can automatically scale out and scale in on demand. The application provides an image Classification service to users. Images uploaded by the users are classified and the corresponding results along with the input image are stored in Amazon S3 buckets.


Design and implementation

1. Web-Tier: On web-tier start, the user selects multiple images on the web-tier and uploads them. The web-tier processes and sends the request to the S3 'input' bucket, where the images are stored, and a URL is generated for the location of the stored image. The URL is returned to the web-tier. The web-tier then passes the input-app to the SQS Launch Queue.
2. Launch Phase in in App-Controller: The Launch Queue is used to maintain all the input image URLs uploaded by the user. The Launch controller is used to scale up the instances based on the load. The launch controller checks the launch queue and creates EC2 instances of the image-classifier. Here the launch controller also keeps a check on the status of EC2 instances and maintains a count of ‘running’ and ‘free’ instances of image-classifier. It creates a maximum of 20 instances when the load is too high.
3. EC2- Instance of image-classifier: The EC2 instance for image-classier when started fetches input-URL from Launch Queue and deletes the message from the queue. As it has fetched the input it passes the message to Monitor Queue stating that it started processing the image. It then fetches the image from S3 ‘input’ bucket based on the given input-URL. The image is classified, and the output result is stored on the S3 bucket and then sent to the Output Queue. The instance then again checks for any new messages inside the Launch Queue and then repeats the process. If there are no more input-URLs to process it terminates itself which helps in scaling down the application as there’s not much load.
4. Monitor Phase in App-Controller: The monitor controller is used for handling multiple error scenarios. It monitors the health of instances for a given input and terminates them if it can’t handle the load. It monitors the Monitor Queue and fetches the image and instance id from it. It then checks for output on the S3 bucket for the given input after some duration. If there’s no Output for the given Input it terminates the instance and sends the message back to the Launch Queue. For a given image this would happen for a maximum of 3 tries after which it sends a message to Output Queue which states an error for giving an image.
5. Output Phase in App-Controller: The web-tier fetches the output result data from the Output Queue and displays it to the user.





Autoscaling:
Scaling in and scaling out is done at the application tier. Depending on the current load we perform the autoscaling. We ensure autoscaling with following components:


SQS: SQS decouples the components of our application, and queues the requests received by the web tier. It keeps the request in the queue till it is processed by EC2 instances. We have an upper limit of 20 on the number of instances in running state at any point of time, thus SQS queues all the pending requests when it reaches this threshold. We are using the length of the queue as the metric to determine the current load and perform scaling in and scaling out of compute resources. We check the length of the queue, the current number of running instances which are processing images, and then decide how many new instances are to be spawned. In order to avoid making continuous requests to the SQS queue we are using long polling, which reduces the cost of using SQS by eliminating the number of empty responses.

Launch Controller: Scaling out of EC2 instances is done at application controller level. The launch module of the controller uses the length of the launch queue as a metric to scale up the instances, making sure that at any point in time the instances count does not go beyond 20. The logic we have implemented is to process the images as soon as they are received to provide fast service to end users. To achieve this, we try to match the number of messages visible in the launch queue and the number of running instances. If the number of SQS messages is higher than 20, the launch controller will spin up EC2 instances making sure that their count does not go beyond 20. This ensures the scaling up of the app tier depending on load. The deployed app instances will process one message each from the queue parallelly, while the remaining messages will be in the queue and will be processed once the app instance is ready to process the next request. With an implementing queue, we make sure that all the requests of the user are processed, and no request is lost.

App Instance: Scaling in is done at application instance level. It processes the current image, sends its output to S3 and output queue. After completing the current request, it checks for the next message in the queue and performs the automatic scaling down operation if there is no message to be processed in the launch queue, by terminating itself.
