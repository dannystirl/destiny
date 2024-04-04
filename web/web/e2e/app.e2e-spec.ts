import { NpAppPage } from './app.po';

describe('np-app App', function() {
  let page: NpAppPage;

  beforeEach(() => {
    page = new NpAppPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
