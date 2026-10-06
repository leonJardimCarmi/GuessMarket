// The web client's entry point (like ClientLauncher + ClientApp in the JavaFX client):
// creates the shared context, which decides the first screen.
import { ClientContext } from './context.js';

document.getElementById('startup-message').hidden = true; // the code runs, so the page was opened the right way
new ClientContext().start();
