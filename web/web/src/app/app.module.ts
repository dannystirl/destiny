import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { AppComponent } from './app.component';
import { ProjectPageComponent } from './projects/projects.component';
import { DestinyComponent } from './projects/destiny/destiny.component';
import { RoguelikeComponent } from './projects/roguelike/roguelike.component';

const ROUTES: Routes = [
  { path: '', component: AppComponent },
  { path: 'projects', component: ProjectPageComponent },
  { path: 'projects/destiny', component: DestinyComponent },
  { path: 'projects/roguelike', component: RoguelikeComponent },
]
@NgModule({
  declarations: [
    AppComponent,
    ProjectPageComponent,
    DestinyComponent,
    RoguelikeComponent
  ],
  imports: [
    // Angular Modules - Do not fix
    RouterModule.forRoot(ROUTES),
    BrowserModule,
    FormsModule,
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
