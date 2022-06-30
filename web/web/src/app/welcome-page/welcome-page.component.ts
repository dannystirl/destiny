import { Component, Inject, OnInit } from '@angular/core';
import { DataSender } from 'app/app.component';

@Component({
  selector: 'app-welcome-page',
  templateUrl: './welcome-page.component.html',
  styleUrls: ['./welcome-page.component.css']
})

@Inject
export class WelcomePageComponent implements OnInit {
  projectTitle: string;
  file: File;

  constructor(@Inject(DataSender) private myService: DataSender) {
    this.file = new File([], '../../../../../output/WishListScripted.txt');
    this.myService.myMethod$.subscribe((data) => {
      this.projectTitle = data; // And he have data here too!
    }
    );
  }

  ngOnInit() {
    console.log(this.file.name);
  }

}
