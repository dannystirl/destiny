import { Component, OnInit } from '@angular/core';
import { AppComponent } from 'app/app.component';

@Component({
  selector: 'app-projects',
  templateUrl: 'projects.component.html',
  styleUrls: ['projects.component.css']
})
export class ProjectPageComponent implements OnInit {
  file: File;
  projectTitle: string;
  title: string;

  constructor() {
    this.projectTitle = AppComponent.call("getProjectTitle");
    this.title = 'Projects';
    this.file = new File([], '../../../../../output/WishListScripted.txt');
  }

  ngOnInit() {
    console.log(this.file);
  }
}
