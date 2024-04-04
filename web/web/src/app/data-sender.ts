import { Observable, Subject } from '@angular/common/src/facade/async';
import { Injectable } from '@angular/core';

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
