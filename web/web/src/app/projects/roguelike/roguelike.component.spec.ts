/* tslint:disable:no-unused-variable */
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import { RoguelikeComponent } from './roguelike.component';

describe('RoguelikeComponent', () => {
  let component: RoguelikeComponent;
  let fixture: ComponentFixture<RoguelikeComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RoguelikeComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RoguelikeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
