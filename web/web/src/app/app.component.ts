import { Component, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})

export class AppComponent {
  public projectTitle: string;
  public title: string;

  constructor(@Inject(DOCUMENT) private document: Document, private http: HttpClient, private router: Router, private route: ActivatedRoute) {
    this.title = 'Welcome';
    this.projectTitle = 'Daniel Stirling';
    console.log(this.document.location.href);
  }

  public getProjectTitle() {
    return this.projectTitle;
  }
}
