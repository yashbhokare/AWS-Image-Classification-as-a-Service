# Image-Classification-as-a-Service


Problem statement
The aim of this project is to build an elastic application that can automatically scale out and scale in on demand. The application provides an image Classification service to users. Images uploaded by the users are classified and the corresponding results along with the input image are stored in Amazon S3 buckets.


Design and implementation
1. Web-Tier: On web-tier start, the user selects multiple images on the web-tier and uploads them. The web-tier processes and sends the request to the S3 'input' bucket, where the images are stored, and a URL is generated for the location of the stored image. The URL is returned to the web-tier. The web-tier then passes the input-app to the SQS Launch Queue.
2. Launch Phase: The Launch Queue is used to maintain all the input image URLs uploaded by the user. The Launch controller is used to scale up the instances based on the load. The launch controller checks the launch queue and creates EC2 instances of the image-classifier. Here the launch controller also keeps a check on the status of EC2 instances and maintains a count of ‘running’ and ‘free’ instances of image-classifier. It creates a maximum of 20 instances when the load is too high.
3. EC2- Instance of image-classifier: The EC2 instance for image-classier when started fetches input-URL from Launch Queue and deletes the message from the queue. As it has fetched the input it passes the message to Monitor Queue stating that it started processing the image. It then fetches the image from S3 ‘input’ bucket based on the given input-URL. The image is classified, and the output result is stored on the S3 bucket and then sent to the Output Queue. The instance then again checks for any new messages inside the Launch Queue and then repeats the process. If there are no more input-URLs to process it terminates itself which helps in scaling down the application as there’s not much load.
Figure 2
4. Monitor Phase: The monitor controller is used for handling multiple error scenarios. It monitors the health of instances for a given input and terminates them if it can’t handle the load. It monitors the Monitor Queue and fetches the image and instance id from it. It then checks for output on the S3 bucket for the given input after some duration. If there’s no Output for the given Input it terminates the instance and sends the message back to the Launch Queue. For a given image this would happen for a maximum of 3 tries after which it sends a message to Output Queue which states an error for giving an image.
5. Output Phase: The web-tier fetches the output result data from the Output Queue and displays it to the user.
