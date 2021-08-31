import { Component, OnInit, Input } from '@angular/core';
import SQS from 'aws-sdk/clients/sqs';
import { ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-result',
  templateUrl: './result.component.html',
  styleUrls: ['./result.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResultComponent implements OnInit {
  @Input() imageDataSet; // decorate the property with @Input()
  timeInterval = 10000;
  timeOut;

  accessKeyId = '';
  secretAccessKey = '';
  region = 'us-east-1';

  sqs = new SQS({
    accessKeyId: this.accessKeyId,
    secretAccessKey: this.secretAccessKey,
    region: this.region,
  });

  params = {
    QueueUrl: '',
    VisibilityTimeout: 30, // 10 min wait time for anyone else to process.
    MessageAttributeNames: ['All'],
  };

  constructor(private cd: ChangeDetectorRef, private _snackBar: MatSnackBar) {}

  ngOnInit(): void {
    this.cd.detectChanges();
    this.fetchSQSQueueMessages();
  }

  fetchSQSQueueMessages() {
    const ref = this;
    clearTimeout(ref.timeOut);
    this.sqs.receiveMessage(this.params, (error, data) => {
      if (data && data.Messages[0] && data.Messages[0].MessageAttributes) {
        console.log(data.Messages[0].MessageAttributes);
        const outputMessage =
          data.Messages[0].MessageAttributes.Output.StringValue;
        const inputString =
          data.Messages[0].MessageAttributes.Input.StringValue;
        const receiptHandle = data.Messages[0].ReceiptHandle;
        this.imageDataSet.forEach((image) => {
          if (image.inputString === inputString && image.output === null) {
            image.output = outputMessage;
            ref.deleteSQSMessage(receiptHandle);
            ref.cd.detectChanges();
          }
          clearTimeout(ref.timeOut);
          ref.timeOut = setTimeout(() => ref.fetchSQSQueueMessages(), 10);
        });
      } else {
        if (ref.checkInputAndOutputMessages()) {
          clearTimeout(ref.timeOut);
          console.log('Terminate the Fetch Queue');
          ref.openSnackBar('Successfully processed images');
          return true;
        }
        console.log('Wait for 1 min');
        clearTimeout(ref.timeOut);
        ref.timeOut = setTimeout(() => ref.fetchSQSQueueMessages(), 20 * 1000);
      }
    });
    
  }

  checkInputAndOutputMessages() {
    let val = true;
    this.imageDataSet.forEach((image) => {
      if (image.output === null) {
        val = false;
        return false;
      }
    });
    return val;
  }

  deleteSQSMessage(receiptHandle) {
    const params = {
      QueueUrl: '',
      ReceiptHandle: receiptHandle,
    };
    this.sqs.deleteMessage(params, (error, data) => {
      if (error) {
        console.log('Error while deleting message', error);
      } else {
        console.log('Message Deleted for: ', data);
      }
    });
  }

  openSnackBar(message) {
    this._snackBar.open(message, 'Close', {
      duration: 3000,
      horizontalPosition: 'start',
      verticalPosition: 'bottom',
    });
  }
}
