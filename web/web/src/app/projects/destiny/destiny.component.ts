import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-destiny',
  templateUrl: './destiny.component.html',
  styleUrls: ['./destiny.component.css']
})
export class DestinyComponent implements OnInit {

  constructor() { }

  ngOnInit() {
    console.log('destiny');
  }

}
