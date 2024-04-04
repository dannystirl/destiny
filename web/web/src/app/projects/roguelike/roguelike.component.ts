import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-roguelike',
  templateUrl: './roguelike.component.html',
  styleUrls: ['./roguelike.component.css']
})
export class RoguelikeComponent implements OnInit {

  constructor() { }

  ngOnInit() {
    console.log('roguelike');
  }

}
