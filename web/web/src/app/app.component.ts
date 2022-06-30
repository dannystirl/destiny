import { Observable, Subject } from '@angular/common/src/facade/async';
import { Component, Inject, Injectable } from '@angular/core';
import { DOCUMENT } from '@angular/platform-browser';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})


export class AppComponent {
  public projectTitle = 'Destiny Applications';
  title = 'Destiny Applications';

  constructor(@Inject(DOCUMENT) private document: Document, @Inject(DataSender) private myService: DataSender) {
    console.log(this.document.location.href);
    this.myService.myMethod(this.projectTitle);
  }
}

@Injectable()
export class DataSender {
  myMethod$: Observable<any>;
  private myMethodSubject = new Subject<any>();

  constructor() {
    this.myMethod$ = this.myMethodSubject.asObservable();
  }

  myMethod(data) {
    console.log(data);
    this.myMethodSubject.next(data);
  }
}
