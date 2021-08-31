import { Component, OnInit } from '@angular/core';
import S3 from 'aws-sdk/clients/s3';
import SQS from 'aws-sdk/clients/sqs';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-file-upload',
  templateUrl: './file-upload.component.html',
  styleUrls: ['./file-upload.component.css'],
})
export class FileUploadComponent implements OnInit {
  imageUrls = [];
  imageFiles: any[] = [];
  BucketName = '';
  accessKeyId = '';
  secretAccessKey = '';
  region = 'us-east-1';
  launchQueueUrl =
    '';
  uploadSuccess = false;
  imagesDataSet = [];
  constructor(private _snackBar: MatSnackBar) {}

  ngOnInit(): void {}

  onSelectFile(event) {
    if (event.target.files && event.target.files[0]) {
      var filesAmount = event.target.files.length;
      for (let i = 0; i < filesAmount; i++) {
        this.imageFiles.push(event.target.files[i]);
        var reader = new FileReader();
        reader.onload = (event: any) => {
          this.imageUrls.push(event.target.result);
        };
        reader.readAsDataURL(event.target.files[i]);
      }
    }
  }

  uploadFiles() {
    for (var i = 0; i < this.imageUrls.length; i++) {
      this.uploadImageToS3(this.imageFiles[i]);
      const imageData = {
        inputString: this.imageFiles[i].name,
        value: this.imageUrls[i],
        output: null,
      };
      this.imagesDataSet.push(imageData);
    }
    console.log('Files uploaded');
    this.openSnackBar();
    this.uploadSuccess = true;
  }

  uploadImageToS3(file) {
    const s3Bucket = new S3({
      accessKeyId: this.accessKeyId,
      secretAccessKey: this.secretAccessKey,
      region: this.region,
    });

    const params = {
      Bucket: this.BucketName,
      Key: file.name,
      Body: file,
      ACL: 'private',
    };

    let ref = this;
    s3Bucket.upload(params, function (err, data) {
      if (err) {
        console.log('There was an error uploading your file: ', err);
        return false;
      }
      console.log('Successfully uploaded file.', data);
      ref.uploadToLaunchQueue(data.Key, data.Location);
    });
  }

  uploadToLaunchQueue(imageName, imageUrl) {
    const sqs = new SQS({
      accessKeyId: this.accessKeyId,
      secretAccessKey: this.secretAccessKey,
      region: this.region,
    });

    const params = {
      MessageBody: imageUrl,
      QueueUrl: this.launchQueueUrl,
      MessageAttributes: {
        Input: {
          DataType: 'String',
          StringValue: imageName,
        },
        InputUrl: {
          DataType: 'String',
          StringValue: imageUrl,
        },
        RetryCount: {
          DataType: 'String',
          StringValue: '0',
        },
      },
    };

    sqs.sendMessage(params, (error, data) => {
      if (error) {
        console.log('Launch Queue Error', error);
      } else {
        console.log('Launch Queue Success', data.MessageId);
      }
    });
  }

  openSnackBar() {
    this._snackBar.open('Uploaded Successfully!!', 'Close', {
      duration: 3000,
      horizontalPosition: 'start',
      verticalPosition: 'bottom',
    });
  }
}
